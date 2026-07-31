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

public final class PersonMemoryStore {
    private static final String TAG = "KikoPersonMemory";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "kiko_person_memories";
    private static final String PREFS_NAME = "person_memories";
    private static final String PREF_CIPHERTEXT = "memory_ciphertext";
    private static final String PREF_IV = "memory_iv";

    private final SharedPreferences preferences;

    public PersonMemoryStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    public synchronized List<PersonMemoryRecord> list() {
        try {
            List<PersonMemoryRecord> records = loadRecords();
            records.sort((left, right) -> left.getDisplayName().compareToIgnoreCase(
                    right.getDisplayName()
            ));
            return Collections.unmodifiableList(records);
        } catch (Exception error) {
            Log.e(TAG, "Encrypted person memories could not be read", error);
            return Collections.emptyList();
        }
    }

    public boolean hasStoredData() {
        return preferences.contains(PREF_CIPHERTEXT)
                || preferences.contains(PREF_IV);
    }

    public synchronized PersonMemoryRecord find(String personName) {
        String canonicalName = PersonMemoryRecord.canonicalizeName(personName);
        for (PersonMemoryRecord record : list()) {
            if (record.getCanonicalName().equals(canonicalName)) {
                return record;
            }
        }
        return null;
    }

    public synchronized PersonMemoryRecord apply(PersonMemoryCommand command) {
        if (command == null || !command.isUpdate()) {
            return null;
        }
        try {
            List<PersonMemoryRecord> records = loadRecords();
            String canonicalName = PersonMemoryRecord.canonicalizeName(
                    command.getPersonName()
            );
            PersonMemoryRecord current = null;
            for (PersonMemoryRecord record : records) {
                if (record.getCanonicalName().equals(canonicalName)) {
                    current = record;
                    break;
                }
            }
            long now = System.currentTimeMillis();
            if (current == null) {
                current = PersonMemoryRecord.empty(command.getPersonName(), now);
            } else {
                records.remove(current);
            }
            PersonMemoryRecord updated = current.apply(command, now);
            records.add(updated);
            saveRecords(records);
            return updated;
        } catch (Exception error) {
            Log.e(TAG, "Person memory could not be updated", error);
            return null;
        }
    }

    public synchronized boolean delete(String canonicalName) {
        try {
            List<PersonMemoryRecord> records = loadRecords();
            boolean removed = records.removeIf(record ->
                    record.getCanonicalName().equals(canonicalName));
            if (removed) {
                saveRecords(records);
            }
            return true;
        } catch (Exception error) {
            Log.e(TAG, "Person memory could not be deleted", error);
            return false;
        }
    }

    public synchronized boolean deleteAll() {
        return preferences.edit()
                .remove(PREF_CIPHERTEXT)
                .remove(PREF_IV)
                .commit();
    }

    synchronized MemoryMaintenanceResult maintain() {
        try {
            List<PersonMemoryRecord> records = loadRecords();
            MemoryConsolidationResult<PersonMemoryRecord> result =
                    StructuredMemoryConsolidator.consolidatePeople(records);
            if (result.changed()) {
                saveRecords(result.getRecords());
            }
            return MemoryMaintenanceResult.success(
                    result.getRecordsBefore(),
                    result.getRecords().size(),
                    result.getDuplicateRecordsMerged(),
                    result.getDuplicateLikesRemoved()
            );
        } catch (Exception error) {
            Log.e(TAG, "Person memory maintenance failed", error);
            return MemoryMaintenanceResult.failure();
        }
    }

    private List<PersonMemoryRecord> loadRecords() throws Exception {
        String encodedCiphertext = preferences.getString(PREF_CIPHERTEXT, null);
        String encodedIv = preferences.getString(PREF_IV, null);
        if (encodedCiphertext == null && encodedIv == null) {
            return new ArrayList<>();
        }
        if (encodedCiphertext == null || encodedIv == null) {
            throw new IllegalStateException("Incomplete encrypted person memory");
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
        return new ArrayList<>(PersonMemoryCodec.decode(cipher.doFinal(
                Base64.decode(encodedCiphertext, Base64.NO_WRAP)
        )));
    }

    private void saveRecords(List<PersonMemoryRecord> records) throws Exception {
        if (records.isEmpty()) {
            if (!deleteAll()) {
                throw new IllegalStateException("Person memory could not be cleared");
            }
            return;
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
        byte[] ciphertext = cipher.doFinal(PersonMemoryCodec.encode(records));
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
            throw new IllegalStateException("Person memory could not be saved");
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
