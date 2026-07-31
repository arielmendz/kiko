package com.kiko.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class FaceIdentityStore {
    private static final String TAG = "KikoFaces";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "kiko_face_identities";
    private static final String PREFS_NAME = "face_identities";
    private static final String PREF_CIPHERTEXT = "registry_ciphertext";
    private static final String PREF_IV = "registry_iv";

    private final SharedPreferences preferences;

    public FaceIdentityStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    public synchronized List<FaceIdentityRecord> list() {
        try {
            return Collections.unmodifiableList(loadRecords());
        } catch (Exception error) {
            Log.e(TAG, "Encrypted face identities could not be read", error);
            return Collections.emptyList();
        }
    }

    public synchronized boolean enroll(
            String sourceHistoryId,
            String name,
            float[] embedding
    ) {
        if (sourceHistoryId == null
                || sourceHistoryId.isEmpty()
                || name == null
                || name.trim().isEmpty()) {
            return false;
        }
        try {
            List<FaceIdentityRecord> records = loadRecords();
            records.removeIf(record ->
                    sourceHistoryId.equals(record.getSourceHistoryId()));
            records.add(new FaceIdentityRecord(
                    UUID.randomUUID().toString(),
                    sourceHistoryId,
                    name.trim(),
                    System.currentTimeMillis(),
                    FaceEmbeddingMatcher.normalize(embedding)
            ));
            saveRecords(records);
            return true;
        } catch (Exception error) {
            Log.e(TAG, "Face identity could not be enrolled", error);
            return false;
        }
    }

    public synchronized boolean forgetBySourceHistoryId(String sourceHistoryId) {
        try {
            List<FaceIdentityRecord> records = loadRecords();
            boolean removed = records.removeIf(record ->
                    record.getSourceHistoryId().equals(sourceHistoryId));
            if (removed) {
                saveRecords(records);
            }
            return true;
        } catch (Exception error) {
            Log.e(TAG, "Face identity could not be forgotten", error);
            return false;
        }
    }

    public synchronized boolean deleteAll() {
        return preferences.edit()
                .remove(PREF_CIPHERTEXT)
                .remove(PREF_IV)
                .commit();
    }

    private List<FaceIdentityRecord> loadRecords() throws Exception {
        String encodedCiphertext = preferences.getString(PREF_CIPHERTEXT, null);
        String encodedIv = preferences.getString(PREF_IV, null);
        if (encodedCiphertext == null && encodedIv == null) {
            return new ArrayList<>();
        }
        if (encodedCiphertext == null || encodedIv == null) {
            throw new IllegalStateException("Incomplete encrypted face registry");
        }

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                new GCMParameterSpec(
                        128,
                        Base64.decode(encodedIv, Base64.NO_WRAP)
                )
        );
        return new ArrayList<>(FaceIdentityCodec.decode(cipher.doFinal(
                Base64.decode(encodedCiphertext, Base64.NO_WRAP)
        )));
    }

    private void saveRecords(List<FaceIdentityRecord> records) throws Exception {
        if (records.isEmpty()) {
            if (!deleteAll()) {
                throw new IllegalStateException("Face registry could not be cleared");
            }
            return;
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
        byte[] ciphertext = cipher.doFinal(FaceIdentityCodec.encode(records));
        boolean saved = preferences.edit()
                .putString(
                        PREF_CIPHERTEXT,
                        Base64.encodeToString(ciphertext, Base64.NO_WRAP)
                )
                .putString(
                        PREF_IV,
                        Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)
                )
                .commit();
        if (!saved) {
            throw new IllegalStateException("Face registry could not be saved");
        }
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
