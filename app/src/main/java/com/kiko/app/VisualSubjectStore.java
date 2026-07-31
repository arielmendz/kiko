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

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class VisualSubjectStore {
    private static final String TAG = "KikoVisualSubjects";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "kiko_visual_subjects";
    private static final String PREFS_NAME = "visual_subjects";
    private static final String PREF_CIPHERTEXT = "registry_ciphertext";
    private static final String PREF_IV = "registry_iv";

    private final SharedPreferences preferences;

    VisualSubjectStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    synchronized List<VisualSubjectRecord> list() {
        try {
            return Collections.unmodifiableList(loadRecords());
        } catch (Exception error) {
            Log.e(TAG, "Encrypted visual subjects could not be read", error);
            return Collections.emptyList();
        }
    }

    synchronized VisualSubjectLoadResult loadForMaintenance() {
        try {
            return VisualSubjectLoadResult.success(loadRecords());
        } catch (Exception error) {
            Log.e(TAG, "Visual subject maintenance validation failed", error);
            return VisualSubjectLoadResult.failure();
        }
    }

    synchronized boolean set(
            String historyRecordId,
            VisualHistoryRecord.SubjectKind kind,
            String name
    ) {
        if (historyRecordId == null
                || historyRecordId.isEmpty()
                || kind == null
                || name == null
                || name.trim().isEmpty()) {
            return false;
        }
        try {
            List<VisualSubjectRecord> records = loadRecords();
            records.removeIf(record -> record.getHistoryRecordId().equals(
                    historyRecordId
            ));
            records.add(new VisualSubjectRecord(
                    historyRecordId,
                    kind,
                    name.trim()
            ));
            saveRecords(records);
            return true;
        } catch (Exception error) {
            Log.e(TAG, "Visual subject could not be saved", error);
            return false;
        }
    }

    synchronized boolean delete(String historyRecordId) {
        try {
            List<VisualSubjectRecord> records = loadRecords();
            boolean removed = records.removeIf(record ->
                    record.getHistoryRecordId().equals(historyRecordId));
            if (removed) {
                saveRecords(records);
            }
            return true;
        } catch (Exception error) {
            Log.e(TAG, "Visual subject could not be deleted", error);
            return false;
        }
    }

    synchronized boolean deleteAll() {
        return preferences.edit()
                .remove(PREF_CIPHERTEXT)
                .remove(PREF_IV)
                .commit();
    }

    private List<VisualSubjectRecord> loadRecords() throws Exception {
        String encodedCiphertext = preferences.getString(PREF_CIPHERTEXT, null);
        String encodedIv = preferences.getString(PREF_IV, null);
        if (encodedCiphertext == null && encodedIv == null) {
            return new ArrayList<>();
        }
        if (encodedCiphertext == null || encodedIv == null) {
            throw new IllegalStateException("Incomplete encrypted visual subjects");
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
        return new ArrayList<>(VisualSubjectCodec.decode(cipher.doFinal(
                Base64.decode(encodedCiphertext, Base64.NO_WRAP)
        )));
    }

    private void saveRecords(List<VisualSubjectRecord> records) throws Exception {
        if (records.isEmpty()) {
            if (!deleteAll()) {
                throw new IllegalStateException("Visual subjects could not be cleared");
            }
            return;
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
        byte[] ciphertext = cipher.doFinal(VisualSubjectCodec.encode(records));
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
            throw new IllegalStateException("Visual subjects could not be saved");
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
