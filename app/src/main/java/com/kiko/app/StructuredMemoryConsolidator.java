package com.kiko.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class StructuredMemoryConsolidator {
    private static final int MAX_LIKES = 20;

    private StructuredMemoryConsolidator() {
    }

    static MemoryConsolidationResult<PersonMemoryRecord> consolidatePeople(
            List<PersonMemoryRecord> input
    ) {
        List<PersonMemoryRecord> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparingLong(
                PersonMemoryRecord::getUpdatedAtEpochMillis
        ).reversed());
        Map<String, List<PersonMemoryRecord>> groups = new LinkedHashMap<>();
        for (PersonMemoryRecord record : sorted) {
            groups.computeIfAbsent(
                    record.getCanonicalName(),
                    ignored -> new ArrayList<>()
            ).add(record);
        }

        List<PersonMemoryRecord> output = new ArrayList<>();
        int recordsMerged = 0;
        int likesRemoved = 0;
        for (List<PersonMemoryRecord> group : groups.values()) {
            LikeMerge likes = mergePersonLikes(group);
            if (group.size() > 1 && likes.values.size() <= MAX_LIKES) {
                PersonMemoryRecord latest = group.get(0);
                output.add(new PersonMemoryRecord(
                        latest.getCanonicalName(),
                        latest.getDisplayName(),
                        firstPersonFavorite(group),
                        likes.values,
                        firstPersonAge(group),
                        latest.getUpdatedAtEpochMillis()
                ));
                recordsMerged += group.size() - 1;
                likesRemoved += likes.duplicatesRemoved;
            } else {
                for (PersonMemoryRecord record : group) {
                    LikeMerge cleaned = cleanPersonLikes(record);
                    output.add(new PersonMemoryRecord(
                            record.getCanonicalName(),
                            record.getDisplayName(),
                            record.getFavoriteFood(),
                            cleaned.values,
                            record.getAge(),
                            record.getUpdatedAtEpochMillis()
                    ));
                    likesRemoved += cleaned.duplicatesRemoved;
                }
            }
        }
        return new MemoryConsolidationResult<>(
                output,
                input.size(),
                recordsMerged,
                likesRemoved
        );
    }

    static MemoryConsolidationResult<PetMemoryRecord> consolidatePets(
            List<PetMemoryRecord> input
    ) {
        List<PetMemoryRecord> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparingLong(
                PetMemoryRecord::getUpdatedAtEpochMillis
        ).reversed());
        Map<String, List<PetMemoryRecord>> groups = new LinkedHashMap<>();
        for (PetMemoryRecord record : sorted) {
            groups.computeIfAbsent(
                    record.getCanonicalName(),
                    ignored -> new ArrayList<>()
            ).add(record);
        }

        List<PetMemoryRecord> output = new ArrayList<>();
        int recordsMerged = 0;
        int likesRemoved = 0;
        for (List<PetMemoryRecord> group : groups.values()) {
            LikeMerge likes = mergePetLikes(group);
            if (group.size() > 1 && likes.values.size() <= MAX_LIKES) {
                PetMemoryRecord latest = group.get(0);
                PetMemoryRecord ownerSource = firstPetWithOwner(group);
                output.add(new PetMemoryRecord(
                        latest.getCanonicalName(),
                        latest.getDisplayName(),
                        latest.getKind(),
                        ownerSource == null
                                ? null : ownerSource.getCanonicalOwnerName(),
                        ownerSource == null
                                ? null : ownerSource.getDisplayOwnerName(),
                        firstPetFavorite(group),
                        likes.values,
                        firstPetAge(group),
                        latest.getUpdatedAtEpochMillis()
                ));
                recordsMerged += group.size() - 1;
                likesRemoved += likes.duplicatesRemoved;
            } else {
                for (PetMemoryRecord record : group) {
                    LikeMerge cleaned = cleanPetLikes(record);
                    output.add(new PetMemoryRecord(
                            record.getCanonicalName(),
                            record.getDisplayName(),
                            record.getKind(),
                            record.getCanonicalOwnerName(),
                            record.getDisplayOwnerName(),
                            record.getFavoriteFood(),
                            cleaned.values,
                            record.getAge(),
                            record.getUpdatedAtEpochMillis()
                    ));
                    likesRemoved += cleaned.duplicatesRemoved;
                }
            }
        }
        return new MemoryConsolidationResult<>(
                output,
                input.size(),
                recordsMerged,
                likesRemoved
        );
    }

    private static LikeMerge mergePersonLikes(List<PersonMemoryRecord> records) {
        List<String> values = new ArrayList<>();
        int duplicates = 0;
        String favorite = firstPersonFavorite(records);
        for (PersonMemoryRecord record : records) {
            for (String like : record.getLikes()) {
                if (isDuplicate(values, like)
                        || sameFact(favorite, like)) {
                    duplicates++;
                } else {
                    values.add(like);
                }
            }
        }
        return new LikeMerge(values, duplicates);
    }

    private static LikeMerge mergePetLikes(List<PetMemoryRecord> records) {
        List<String> values = new ArrayList<>();
        int duplicates = 0;
        String favorite = firstPetFavorite(records);
        for (PetMemoryRecord record : records) {
            for (String like : record.getLikes()) {
                if (isDuplicate(values, like)
                        || sameFact(favorite, like)) {
                    duplicates++;
                } else {
                    values.add(like);
                }
            }
        }
        return new LikeMerge(values, duplicates);
    }

    private static LikeMerge cleanPersonLikes(PersonMemoryRecord record) {
        List<PersonMemoryRecord> singleton = new ArrayList<>();
        singleton.add(record);
        return mergePersonLikes(singleton);
    }

    private static LikeMerge cleanPetLikes(PetMemoryRecord record) {
        List<PetMemoryRecord> singleton = new ArrayList<>();
        singleton.add(record);
        return mergePetLikes(singleton);
    }

    private static String firstPersonFavorite(List<PersonMemoryRecord> records) {
        for (PersonMemoryRecord record : records) {
            if (record.getFavoriteFood() != null) {
                return record.getFavoriteFood();
            }
        }
        return null;
    }

    private static Integer firstPersonAge(List<PersonMemoryRecord> records) {
        for (PersonMemoryRecord record : records) {
            if (record.getAge() != null) {
                return record.getAge();
            }
        }
        return null;
    }

    private static String firstPetFavorite(List<PetMemoryRecord> records) {
        for (PetMemoryRecord record : records) {
            if (record.getFavoriteFood() != null) {
                return record.getFavoriteFood();
            }
        }
        return null;
    }

    private static Integer firstPetAge(List<PetMemoryRecord> records) {
        for (PetMemoryRecord record : records) {
            if (record.getAge() != null) {
                return record.getAge();
            }
        }
        return null;
    }

    private static PetMemoryRecord firstPetWithOwner(List<PetMemoryRecord> records) {
        for (PetMemoryRecord record : records) {
            if (record.getCanonicalOwnerName() != null) {
                return record;
            }
        }
        return null;
    }

    private static boolean isDuplicate(List<String> values, String candidate) {
        for (String value : values) {
            if (sameFact(value, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameFact(String left, String right) {
        return left != null
                && right != null
                && PersonMemoryRecord.normalizeFact(left).equals(
                        PersonMemoryRecord.normalizeFact(right)
                );
    }

    private static final class LikeMerge {
        private final List<String> values;
        private final int duplicatesRemoved;

        private LikeMerge(List<String> values, int duplicatesRemoved) {
            this.values = values;
            this.duplicatesRemoved = duplicatesRemoved;
        }
    }
}
