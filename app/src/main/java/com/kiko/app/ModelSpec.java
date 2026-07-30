package com.kiko.app;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ModelSpec {
    private static final String HUGGING_FACE_BASE = "https://huggingface.co/";

    private final String id;
    private final String displayName;
    private final String parameters;
    private final String quantization;
    private final String repository;
    private final String revision;
    private final String filename;
    private final long byteSize;
    private final String sha256;
    private final String license;
    private final boolean gated;
    private final String description;
    private final ModelPurpose purpose;
    private final String sourceUrl;
    private final String downloadUrl;
    private final Map<String, String> downloadHeaders;

    public ModelSpec(
            String id,
            String displayName,
            String parameters,
            String quantization,
            String repository,
            String revision,
            String filename,
            long byteSize,
            String sha256,
            String license,
            boolean gated,
            String description
    ) {
        this.id = Objects.requireNonNull(id);
        this.displayName = Objects.requireNonNull(displayName);
        this.parameters = Objects.requireNonNull(parameters);
        this.quantization = Objects.requireNonNull(quantization);
        this.repository = Objects.requireNonNull(repository);
        this.revision = Objects.requireNonNull(revision);
        this.filename = Objects.requireNonNull(filename);
        this.byteSize = byteSize;
        this.sha256 = Objects.requireNonNull(sha256);
        this.license = Objects.requireNonNull(license);
        this.gated = gated;
        this.description = Objects.requireNonNull(description);
        purpose = ModelPurpose.LANGUAGE;
        sourceUrl = HUGGING_FACE_BASE + repository;
        downloadUrl = HUGGING_FACE_BASE
                + repository
                + "/resolve/"
                + revision
                + "/"
                + filename
                + "?download=true";
        downloadHeaders = Collections.emptyMap();
    }

    public static ModelSpec directArtifact(
            String id,
            String displayName,
            String parameters,
            String quantization,
            String repository,
            String revision,
            String filename,
            long byteSize,
            String sha256,
            String license,
            String description,
            ModelPurpose purpose,
            String sourceUrl,
            String downloadUrl,
            Map<String, String> downloadHeaders
    ) {
        return new ModelSpec(
                id,
                displayName,
                parameters,
                quantization,
                repository,
                revision,
                filename,
                byteSize,
                sha256,
                license,
                false,
                description,
                purpose,
                sourceUrl,
                downloadUrl,
                downloadHeaders
        );
    }

    private ModelSpec(
            String id,
            String displayName,
            String parameters,
            String quantization,
            String repository,
            String revision,
            String filename,
            long byteSize,
            String sha256,
            String license,
            boolean gated,
            String description,
            ModelPurpose purpose,
            String sourceUrl,
            String downloadUrl,
            Map<String, String> downloadHeaders
    ) {
        this.id = Objects.requireNonNull(id);
        this.displayName = Objects.requireNonNull(displayName);
        this.parameters = Objects.requireNonNull(parameters);
        this.quantization = Objects.requireNonNull(quantization);
        this.repository = Objects.requireNonNull(repository);
        this.revision = Objects.requireNonNull(revision);
        this.filename = Objects.requireNonNull(filename);
        this.byteSize = byteSize;
        this.sha256 = Objects.requireNonNull(sha256);
        this.license = Objects.requireNonNull(license);
        this.gated = gated;
        this.description = Objects.requireNonNull(description);
        this.purpose = Objects.requireNonNull(purpose);
        this.sourceUrl = Objects.requireNonNull(sourceUrl);
        this.downloadUrl = Objects.requireNonNull(downloadUrl);
        this.downloadHeaders = Collections.unmodifiableMap(
                new LinkedHashMap<>(Objects.requireNonNull(downloadHeaders))
        );
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getParameters() {
        return parameters;
    }

    public String getQuantization() {
        return quantization;
    }

    public String getRepository() {
        return repository;
    }

    public String getRevision() {
        return revision;
    }

    public String getFilename() {
        return filename;
    }

    public long getByteSize() {
        return byteSize;
    }

    public String getSha256() {
        return sha256;
    }

    public String getLicense() {
        return license;
    }

    public boolean isGated() {
        return gated;
    }

    public String getDescription() {
        return description;
    }

    public ModelPurpose getPurpose() {
        return purpose;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public Map<String, String> getDownloadHeaders() {
        return downloadHeaders;
    }
}
