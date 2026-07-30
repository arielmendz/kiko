package com.kiko.app;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Locale;

public final class ModelDownloadStore {
    private static final String PREFS_NAME = "model_downloads";
    private static final String DOWNLOAD_ID_PREFIX = "download_id_";
    private static final String VERIFIED_HASH_PREFIX = "verified_hash_";
    private static final String LAST_ERROR_PREFIX = "last_error_";
    private static final long REQUIRED_FREE_SPACE_PADDING = 64L * 1024L * 1024L;

    private final Context context;
    private final DownloadManager downloadManager;
    private final SharedPreferences preferences;

    public ModelDownloadStore(Context context) {
        this.context = context.getApplicationContext();
        downloadManager = context.getSystemService(DownloadManager.class);
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public File getModelsDirectory() {
        return context.getExternalFilesDir("models");
    }

    public File getModelFile(ModelSpec model) {
        File directory = getModelsDirectory();
        return directory == null ? null : new File(directory, model.getFilename());
    }

    public File getPartialFile(ModelSpec model) {
        File directory = getModelsDirectory();
        return directory == null ? null : new File(directory, model.getFilename() + ".part");
    }

    public boolean hasEnoughSpace(ModelSpec model) {
        File directory = getModelsDirectory();
        if (directory == null && !createModelsDirectory()) {
            return false;
        }
        StatFs storage = new StatFs(getModelsDirectory().getAbsolutePath());
        return storage.getAvailableBytes()
                >= model.getByteSize() + REQUIRED_FREE_SPACE_PADDING;
    }

    public long enqueue(ModelSpec model, String token) throws IOException {
        if (!createModelsDirectory()) {
            throw new IOException("Model storage is unavailable");
        }

        File partialFile = getPartialFile(model);
        if (partialFile.exists() && !partialFile.delete()) {
            throw new IOException("Old partial download could not be removed");
        }

        DownloadManager.Request request = new DownloadManager.Request(
                Uri.parse(model.getDownloadUrl())
        )
                .setTitle(model.getDisplayName())
                .setDescription(context.getString(R.string.download_notification_description))
                .setMimeType("application/octet-stream")
                .setAllowedOverRoaming(false)
                .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationInExternalFilesDir(
                        context,
                        "models",
                        model.getFilename() + ".part"
                );

        if (model.isGated()) {
            if (token == null || token.trim().isEmpty()) {
                throw new IllegalArgumentException("A Hugging Face token is required");
            }
            request.addRequestHeader("Authorization", "Bearer " + token.trim());
        }

        long downloadId = downloadManager.enqueue(request);
        preferences.edit()
                .putLong(DOWNLOAD_ID_PREFIX + model.getId(), downloadId)
                .remove(VERIFIED_HASH_PREFIX + model.getId())
                .remove(LAST_ERROR_PREFIX + model.getId())
                .apply();
        return downloadId;
    }

    public DownloadSnapshot getSnapshot(ModelSpec model) {
        File finalFile = getModelFile(model);
        String verifiedHash = preferences.getString(
                VERIFIED_HASH_PREFIX + model.getId(),
                null
        );
        if (finalFile != null
                && finalFile.isFile()
                && finalFile.length() == model.getByteSize()
                && model.getSha256().equals(verifiedHash)) {
            return DownloadSnapshot.downloaded(model.getByteSize());
        }

        long downloadId = preferences.getLong(
                DOWNLOAD_ID_PREFIX + model.getId(),
                -1L
        );
        if (downloadId < 0L) {
            if (finalFile != null && finalFile.exists()) {
                return DownloadSnapshot.corrupt();
            }
            int lastError = preferences.getInt(
                    LAST_ERROR_PREFIX + model.getId(),
                    0
            );
            if (lastError != 0) {
                return DownloadSnapshot.failed(lastError);
            }
            return DownloadSnapshot.notDownloaded();
        }

        try (Cursor cursor = downloadManager.query(
                new DownloadManager.Query().setFilterById(downloadId)
        )) {
            if (!cursor.moveToFirst()) {
                clearDownloadId(model);
                return DownloadSnapshot.failed(DownloadManager.ERROR_UNKNOWN);
            }

            int status = cursor.getInt(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            );
            long downloaded = cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                    )
            );
            long total = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            );
            int reason = cursor.getInt(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
            );

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                return DownloadSnapshot.readyToVerify(downloaded);
            }
            if (status == DownloadManager.STATUS_FAILED) {
                recordFailure(model, reason);
                return DownloadSnapshot.failed(reason);
            }
            if (status == DownloadManager.STATUS_PAUSED) {
                return DownloadSnapshot.paused(downloaded, total, reason);
            }
            return DownloadSnapshot.downloading(downloaded, total);
        }
    }

    public boolean verifyAndFinalize(ModelSpec model) throws Exception {
        File partialFile = getPartialFile(model);
        File finalFile = getModelFile(model);
        if (partialFile == null || finalFile == null || !partialFile.isFile()) {
            clearDownloadId(model);
            return false;
        }
        if (partialFile.length() != model.getByteSize()) {
            partialFile.delete();
            clearDownloadId(model);
            return false;
        }

        String actualHash = calculateSha256(partialFile);
        if (!model.getSha256().equals(actualHash)) {
            partialFile.delete();
            clearDownloadId(model);
            return false;
        }

        if (finalFile.exists() && !finalFile.delete()) {
            throw new IOException("Old model file could not be replaced");
        }
        if (!partialFile.renameTo(finalFile)) {
            throw new IOException("Verified model file could not be finalized");
        }

        preferences.edit()
                .putString(VERIFIED_HASH_PREFIX + model.getId(), actualHash)
                .remove(DOWNLOAD_ID_PREFIX + model.getId())
                .remove(LAST_ERROR_PREFIX + model.getId())
                .apply();
        return true;
    }

    public void cancel(ModelSpec model) {
        long downloadId = preferences.getLong(
                DOWNLOAD_ID_PREFIX + model.getId(),
                -1L
        );
        if (downloadId >= 0L) {
            downloadManager.remove(downloadId);
        }
        clearDownloadId(model);
        preferences.edit()
                .remove(LAST_ERROR_PREFIX + model.getId())
                .apply();
        File partialFile = getPartialFile(model);
        if (partialFile != null) {
            partialFile.delete();
        }
    }

    public boolean delete(ModelSpec model) {
        cancel(model);
        preferences.edit()
                .remove(VERIFIED_HASH_PREFIX + model.getId())
                .remove(LAST_ERROR_PREFIX + model.getId())
                .apply();
        File finalFile = getModelFile(model);
        return finalFile == null || !finalFile.exists() || finalFile.delete();
    }

    private boolean createModelsDirectory() {
        File directory = getModelsDirectory();
        return directory != null && (directory.isDirectory() || directory.mkdirs());
    }

    private void clearDownloadId(ModelSpec model) {
        preferences.edit()
                .remove(DOWNLOAD_ID_PREFIX + model.getId())
                .apply();
    }

    private void recordFailure(ModelSpec model, int reason) {
        preferences.edit()
                .remove(DOWNLOAD_ID_PREFIX + model.getId())
                .putInt(LAST_ERROR_PREFIX + model.getId(), reason)
                .apply();
    }

    private static String calculateSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[1024 * 1024];
        try (BufferedInputStream input = new BufferedInputStream(
                new FileInputStream(file)
        )) {
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        StringBuilder hash = new StringBuilder(64);
        for (byte value : digest.digest()) {
            hash.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        }
        return hash.toString();
    }

    public static final class DownloadSnapshot {
        public enum State {
            NOT_DOWNLOADED,
            DOWNLOADING,
            PAUSED,
            READY_TO_VERIFY,
            DOWNLOADED,
            FAILED,
            CORRUPT
        }

        private final State state;
        private final long downloadedBytes;
        private final long totalBytes;
        private final int reason;

        private DownloadSnapshot(
                State state,
                long downloadedBytes,
                long totalBytes,
                int reason
        ) {
            this.state = state;
            this.downloadedBytes = downloadedBytes;
            this.totalBytes = totalBytes;
            this.reason = reason;
        }

        public static DownloadSnapshot notDownloaded() {
            return new DownloadSnapshot(State.NOT_DOWNLOADED, 0L, 0L, 0);
        }

        public static DownloadSnapshot downloading(long downloaded, long total) {
            return new DownloadSnapshot(State.DOWNLOADING, downloaded, total, 0);
        }

        public static DownloadSnapshot paused(long downloaded, long total, int reason) {
            return new DownloadSnapshot(State.PAUSED, downloaded, total, reason);
        }

        public static DownloadSnapshot readyToVerify(long downloaded) {
            return new DownloadSnapshot(
                    State.READY_TO_VERIFY,
                    downloaded,
                    downloaded,
                    0
            );
        }

        public static DownloadSnapshot downloaded(long total) {
            return new DownloadSnapshot(State.DOWNLOADED, total, total, 0);
        }

        public static DownloadSnapshot failed(int reason) {
            return new DownloadSnapshot(State.FAILED, 0L, 0L, reason);
        }

        public static DownloadSnapshot corrupt() {
            return new DownloadSnapshot(State.CORRUPT, 0L, 0L, 0);
        }

        public State getState() {
            return state;
        }

        public long getDownloadedBytes() {
            return downloadedBytes;
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        public int getReason() {
            return reason;
        }

        public int getProgressPercent() {
            if (totalBytes <= 0L) {
                return 0;
            }
            return (int) Math.min(100L, downloadedBytes * 100L / totalBytes);
        }
    }
}
