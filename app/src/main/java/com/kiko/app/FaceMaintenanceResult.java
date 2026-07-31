package com.kiko.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class FaceMaintenanceResult {
    private final boolean successful;
    private final List<FaceIdentityRecord> identities;

    private FaceMaintenanceResult(
            boolean successful,
            List<FaceIdentityRecord> identities
    ) {
        this.successful = successful;
        this.identities = Collections.unmodifiableList(
                new ArrayList<>(identities)
        );
    }

    static FaceMaintenanceResult success(List<FaceIdentityRecord> identities) {
        return new FaceMaintenanceResult(true, identities);
    }

    static FaceMaintenanceResult failure() {
        return new FaceMaintenanceResult(false, Collections.emptyList());
    }

    boolean isSuccessful() {
        return successful;
    }

    List<FaceIdentityRecord> getIdentities() {
        return identities;
    }
}
