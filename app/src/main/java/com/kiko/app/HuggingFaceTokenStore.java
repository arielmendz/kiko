package com.kiko.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class HuggingFaceTokenStore {
    private static final String TAG = "KikoModelAuth";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "kiko_hugging_face_token";
    private static final String PREFS_NAME = "model_credentials";
    private static final String PREF_CIPHERTEXT = "hf_token_ciphertext";
    private static final String PREF_IV = "hf_token_iv";

    private final SharedPreferences preferences;

    public HuggingFaceTokenStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasToken() {
        return loadToken() != null;
    }

    public void saveToken(String token) throws Exception {
        String trimmed = token == null ? "" : token.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Token must not be empty");
        }

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
        byte[] ciphertext = cipher.doFinal(trimmed.getBytes(StandardCharsets.UTF_8));

        preferences.edit()
                .putString(PREF_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .putString(PREF_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .apply();
    }

    public String loadToken() {
        String encodedCiphertext = preferences.getString(PREF_CIPHERTEXT, null);
        String encodedIv = preferences.getString(PREF_IV, null);
        if (encodedCiphertext == null || encodedIv == null) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateSecretKey(),
                    new GCMParameterSpec(128, Base64.decode(encodedIv, Base64.NO_WRAP))
            );
            byte[] plaintext = cipher.doFinal(
                    Base64.decode(encodedCiphertext, Base64.NO_WRAP)
            );
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception error) {
            Log.w(TAG, "Stored Hugging Face token could not be decrypted");
            clearToken();
            return null;
        }
    }

    public void clearToken() {
        preferences.edit()
                .remove(PREF_CIPHERTEXT)
                .remove(PREF_IV)
                .apply();
    }

    private SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null))
                    .getSecretKey();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
        );
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return keyGenerator.generateKey();
    }
}

