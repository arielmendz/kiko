package com.kiko.app;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class VisualHistoryStore {
    private static final String DIRECTORY_NAME = "visual-history";
    private static final String IMAGE_SUFFIX = ".jpg";
    private static final String METADATA_SUFFIX = ".meta";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final int JPEG_QUALITY = 92;
    private static final int MAX_METADATA_BYTES = 64 * 1024;
    private static final Pattern RECORD_ID =
            Pattern.compile("[0-9]{13}-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-"
                    + "[0-9a-f]{4}-[0-9a-f]{12}");

    private final File directory;
    private final VisualSubjectStore subjectStore;

    public VisualHistoryStore(Context context) {
        directory = new File(context.getApplicationContext().getFilesDir(), DIRECTORY_NAME);
        subjectStore = new VisualSubjectStore(context);
    }

    public VisualHistoryRecord save(
            Bitmap bitmap,
            long capturedAtEpochMillis,
            String description
    ) throws IOException {
        return save(
                bitmap,
                capturedAtEpochMillis,
                description,
                VisualHistoryRecord.RecognitionStatus.UNKNOWN,
                null,
                null
        );
    }

    public VisualHistoryRecord save(
            Bitmap bitmap,
            long capturedAtEpochMillis,
            String description,
            VisualHistoryRecord.RecognitionStatus recognitionStatus,
            VisualHistoryRecord.SubjectKind subjectKind,
            String subjectName
    ) throws IOException {
        if (bitmap == null || bitmap.isRecycled()) {
            throw new IOException("Captured bitmap is unavailable");
        }
        if ((subjectKind == null) != (subjectName == null)
                || subjectName != null && subjectName.trim().isEmpty()) {
            throw new IOException("Invalid visual subject");
        }
        if (recognitionStatus == null) {
            throw new IOException("Recognition status is required");
        }
        if (!ensureDirectory()) {
            throw new IOException("Visual history directory is unavailable");
        }

        String id = String.format(
                Locale.ROOT,
                "%013d-%s",
                capturedAtEpochMillis,
                UUID.randomUUID().toString()
        );
        File imageFile = new File(directory, id + IMAGE_SUFFIX);
        File metadataFile = new File(directory, id + METADATA_SUFFIX);
        File imageTemp = new File(directory, id + IMAGE_SUFFIX + TEMP_SUFFIX);
        File metadataTemp = new File(directory, id + METADATA_SUFFIX + TEMP_SUFFIX);

        try {
            writeImage(bitmap, imageTemp);
            writeMetadata(
                    recognitionStatus == VisualHistoryRecord.RecognitionStatus.UNKNOWN
                            ? VisualHistoryMetadata.encode(
                                    capturedAtEpochMillis,
                                    description
                            )
                            : VisualHistoryMetadata.encodeRecognition(
                                    capturedAtEpochMillis,
                                    description,
                                    recognitionStatus
                            ),
                    metadataTemp
            );
            moveNewFile(imageTemp, imageFile);
            try {
                moveNewFile(metadataTemp, metadataFile);
            } catch (IOException error) {
                imageFile.delete();
                throw error;
            }
            VisualHistoryRecord record = new VisualHistoryRecord(
                    id,
                    capturedAtEpochMillis,
                    description,
                    recognitionStatus,
                    subjectKind,
                    subjectName,
                    imageFile
            );
            if (subjectKind != null
                    && !subjectStore.set(id, subjectKind, subjectName)) {
                imageFile.delete();
                metadataFile.delete();
                throw new IOException("Visual subject could not be encrypted");
            }
            return record;
        } finally {
            imageTemp.delete();
            metadataTemp.delete();
        }
    }

    public List<VisualHistoryRecord> list() {
        return list(subjectStore.list());
    }

    private List<VisualHistoryRecord> list(
            List<VisualSubjectRecord> subjects
    ) {
        if (!directory.isDirectory()) {
            return Collections.emptyList();
        }
        Map<String, VisualSubjectRecord> subjectsByHistoryId = new HashMap<>();
        for (VisualSubjectRecord subject : subjects) {
            subjectsByHistoryId.put(subject.getHistoryRecordId(), subject);
        }
        File[] metadataFiles = directory.listFiles(
                (ignored, name) -> name.endsWith(METADATA_SUFFIX)
        );
        if (metadataFiles == null || metadataFiles.length == 0) {
            return Collections.emptyList();
        }

        List<VisualHistoryRecord> records = new ArrayList<>();
        for (File metadataFile : metadataFiles) {
            String name = metadataFile.getName();
            String id = name.substring(0, name.length() - METADATA_SUFFIX.length());
            if (!isRecordId(id)) {
                continue;
            }
            File imageFile = new File(directory, id + IMAGE_SUFFIX);
            if (!imageFile.isFile()) {
                continue;
            }
            try {
                VisualHistoryMetadata.Decoded metadata =
                        VisualHistoryMetadata.decode(readMetadata(metadataFile));
                VisualSubjectRecord subject = subjectsByHistoryId.get(id);
                VisualHistoryRecord.SubjectKind subjectKind = subject == null
                        ? metadata.getPersonName() == null
                                ? null : VisualHistoryRecord.SubjectKind.PERSON
                        : subject.getKind();
                String subjectName = subject == null
                        ? metadata.getPersonName()
                        : subject.getName();
                records.add(new VisualHistoryRecord(
                        id,
                        metadata.getCapturedAtEpochMillis(),
                        metadata.getDescription(),
                        metadata.getRecognitionStatus(),
                        subjectKind,
                        subjectName,
                        imageFile
                ));
            } catch (IOException | IllegalArgumentException ignored) {
                // A malformed record stays isolated and cannot break the history screen.
            }
        }
        records.sort((left, right) -> {
            int byTime = Long.compare(
                    right.getCapturedAtEpochMillis(),
                    left.getCapturedAtEpochMillis()
            );
            return byTime != 0 ? byTime : right.getId().compareTo(left.getId());
        });
        return records;
    }

    public boolean setPersonName(String recordId, String personName) {
        return setSubject(
                recordId,
                VisualHistoryRecord.SubjectKind.PERSON,
                personName
        );
    }

    public boolean clearPersonName(String recordId) {
        return clearSubject(recordId);
    }

    public boolean setSubject(
            String recordId,
            VisualHistoryRecord.SubjectKind subjectKind,
            String subjectName
    ) {
        if (!isRecordId(recordId)
                || subjectKind == null
                || subjectName == null
                || subjectName.trim().isEmpty()) {
            return false;
        }
        File imageFile = new File(directory, recordId + IMAGE_SUFFIX);
        File metadataFile = new File(directory, recordId + METADATA_SUFFIX);
        return imageFile.isFile()
                && metadataFile.isFile()
                && subjectStore.set(recordId, subjectKind, subjectName.trim())
                && updateLegacyPersonName(recordId, null);
    }

    public boolean clearSubject(String recordId) {
        return isRecordId(recordId)
                && subjectStore.delete(recordId)
                && updateLegacyPersonName(recordId, null);
    }

    private boolean updateLegacyPersonName(
            String recordId,
            String personName
    ) {
        File imageFile = new File(directory, recordId + IMAGE_SUFFIX);
        File metadataFile = new File(directory, recordId + METADATA_SUFFIX);
        File metadataTemp = new File(
                directory,
                recordId + METADATA_SUFFIX + ".subject" + TEMP_SUFFIX
        );
        if (!imageFile.isFile() || !metadataFile.isFile()) {
            return false;
        }

        try {
            VisualHistoryMetadata.Decoded metadata =
                    VisualHistoryMetadata.decode(readMetadata(metadataFile));
            if (metadata.getPersonName() == null) {
                return true;
            }
            writeMetadata(
                    VisualHistoryMetadata.encode(
                            metadata.getCapturedAtEpochMillis(),
                            metadata.getDescription(),
                            personName
                    ),
                    metadataTemp
            );
            replaceFile(metadataTemp, metadataFile);
            return true;
        } catch (IOException | IllegalArgumentException error) {
            return false;
        } finally {
            metadataTemp.delete();
        }
    }

    synchronized VisualHistoryMaintenanceResult maintain(
            List<FaceIdentityRecord> identities,
            boolean deleteUnrecognized
    ) {
        VisualSubjectLoadResult subjectResult =
                subjectStore.loadForMaintenance();
        if (!subjectResult.isSuccessful()) {
            return VisualHistoryMaintenanceResult.failure();
        }
        List<VisualHistoryRecord> records = list(subjectResult.getRecords());
        VisualHistoryMaintenancePlan plan = VisualHistoryMaintenancePlan.create(
                records,
                identities,
                deleteUnrecognized
        );
        for (VisualHistoryMaintenancePlan.SubjectUpdate update
                : plan.getSubjectUpdates()) {
            if (!setSubject(
                    update.getRecordId(),
                    update.getSubjectKind(),
                    update.getSubjectName()
            )) {
                return VisualHistoryMaintenanceResult.failure();
            }
        }
        int deleted = 0;
        for (VisualHistoryRecord record : plan.getRecordsToDelete()) {
            if (!delete(record)) {
                return VisualHistoryMaintenanceResult.failure();
            }
            deleted++;
        }
        return VisualHistoryMaintenanceResult.success(
                records.size() - deleted,
                deleted,
                plan.getNamedGroupCount()
        );
    }

    public boolean delete(VisualHistoryRecord record) {
        if (record == null || !isRecordId(record.getId())) {
            return false;
        }
        if (!subjectStore.delete(record.getId())) {
            return false;
        }
        File imageFile = new File(directory, record.getId() + IMAGE_SUFFIX);
        File metadataFile = new File(directory, record.getId() + METADATA_SUFFIX);
        boolean imageDeleted = !imageFile.exists() || imageFile.delete();
        boolean metadataDeleted = !metadataFile.exists() || metadataFile.delete();
        return imageDeleted && metadataDeleted;
    }

    public boolean deleteAll() {
        if (!subjectStore.deleteAll()) {
            return false;
        }
        if (!directory.exists()) {
            return true;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }
        boolean deleted = true;
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(IMAGE_SUFFIX)
                    || name.endsWith(METADATA_SUFFIX)
                    || name.endsWith(TEMP_SUFFIX)) {
                deleted &= !file.exists() || file.delete();
            }
        }
        return deleted;
    }

    private boolean ensureDirectory() {
        return directory.isDirectory() || directory.mkdirs();
    }

    private static void writeImage(Bitmap bitmap, File destination) throws IOException {
        try (FileOutputStream output = new FileOutputStream(destination)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                throw new IOException("JPEG compression failed");
            }
            output.flush();
            output.getFD().sync();
        }
    }

    private static void writeMetadata(String metadata, File destination) throws IOException {
        byte[] bytes = metadata.getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream output = new FileOutputStream(destination)) {
            output.write(bytes);
            output.flush();
            output.getFD().sync();
        }
    }

    private static String readMetadata(File file) throws IOException {
        if (file.length() <= 0L || file.length() > MAX_METADATA_BYTES) {
            throw new IOException("Invalid metadata length");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream((int) file.length());
        byte[] buffer = new byte[4096];
        try (FileInputStream input = new FileInputStream(file)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (output.size() + read > MAX_METADATA_BYTES) {
                    throw new IOException("Metadata is too large");
                }
                output.write(buffer, 0, read);
            }
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private static void moveNewFile(File source, File destination) throws IOException {
        if (destination.exists() || !source.renameTo(destination)) {
            throw new IOException("Could not finalize visual history file");
        }
    }

    private static void replaceFile(File source, File destination) throws IOException {
        try {
            Files.move(
                    source.toPath(),
                    destination.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException error) {
            Files.move(
                    source.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private static boolean isRecordId(String id) {
        return id != null && RECORD_ID.matcher(id).matches();
    }
}
