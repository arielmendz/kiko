package com.kiko.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VisualHistoryActivity extends Activity {
    private static final int THUMBNAIL_MAX_DIMENSION = 960;

    private VisualHistoryStore historyStore;
    private FaceIdentityStore faceIdentityStore;
    private PetMemoryStore petMemoryStore;
    private HistoryAdapter adapter;
    private Button deleteAllButton;
    private Map<String, FaceIdentityRecord> identitiesBySource =
            Collections.emptyMap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyStore = new VisualHistoryStore(this);
        faceIdentityStore = new FaceIdentityStore(this);
        petMemoryStore = new PetMemoryStore(this);
        adapter = new HistoryAdapter();
        setContentView(createContentView());
    }

    @Override
    protected void onStart() {
        super.onStart();
        reloadHistory();
    }

    private View createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.kiko_background));
        root.setPadding(dp(20), dp(24), dp(20), dp(16));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        Button back = new Button(this);
        back.setText(R.string.action_back);
        back.setOnClickListener(view -> finish());
        toolbar.addView(back);

        TextView title = textView(
                getString(R.string.visual_history_title),
                28,
                R.color.kiko_text
        );
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        titleParams.setMargins(dp(16), 0, 0, 0);
        toolbar.addView(title, titleParams);
        root.addView(toolbar);

        TextView explanation = textView(
                getString(R.string.visual_history_explanation),
                16,
                R.color.kiko_muted
        );
        explanation.setPadding(0, dp(16), 0, dp(12));
        root.addView(explanation);

        deleteAllButton = new Button(this);
        deleteAllButton.setText(R.string.action_delete_all);
        deleteAllButton.setOnClickListener(view -> confirmDeleteAll());
        root.addView(deleteAllButton);

        TextView empty = textView(
                getString(R.string.visual_history_empty),
                18,
                R.color.kiko_muted
        );
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, dp(48), 0, dp(48));
        root.addView(empty);

        ListView list = new ListView(this);
        list.setAdapter(adapter);
        list.setEmptyView(empty);
        list.setDivider(null);
        list.setDividerHeight(dp(12));
        root.addView(
                list,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );
        return root;
    }

    private void reloadHistory() {
        List<VisualHistoryRecord> records = historyStore.list();
        Map<String, FaceIdentityRecord> identities = new HashMap<>();
        for (FaceIdentityRecord identity : faceIdentityStore.list()) {
            identities.put(identity.getSourceHistoryId(), identity);
        }
        identitiesBySource = identities;
        adapter.setRecords(VisualHistoryGrouping.arrange(
                applyIdentityNames(records, identities)
        ));
        deleteAllButton.setEnabled(
                !records.isEmpty() || !identities.isEmpty()
        );
    }

    private static List<VisualHistoryRecord> applyIdentityNames(
            List<VisualHistoryRecord> records,
            Map<String, FaceIdentityRecord> identities
    ) {
        java.util.ArrayList<VisualHistoryRecord> output =
                new java.util.ArrayList<>();
        for (VisualHistoryRecord record : records) {
            FaceIdentityRecord identity = identities.get(record.getId());
            if (identity == null) {
                output.add(record);
            } else {
                output.add(new VisualHistoryRecord(
                        record.getId(),
                        record.getCapturedAtEpochMillis(),
                        record.getDescription(),
                        record.getRecognitionStatus(),
                        VisualHistoryRecord.SubjectKind.PERSON,
                        identity.getName(),
                        record.getImageFile()
                ));
            }
        }
        return output;
    }

    private void confirmDelete(VisualHistoryRecord record) {
        if (!isDeviceUnlocked()) {
            showOwnerUnlockRequired();
            return;
        }
        FaceIdentityRecord identity =
                identitiesBySource.get(record.getId());
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_visual_history_title)
                .setMessage(identity != null
                        ? getString(
                                R.string.delete_visual_history_identity_message,
                                identity.getName()
                        )
                        : record.getDescription())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    boolean identityDeleted = identity == null
                            || faceIdentityStore.forgetBySourceHistoryId(
                                    record.getId()
                            );
                    if (!identityDeleted) {
                        showIdentityDeleteError();
                    } else if (!historyStore.delete(record)) {
                        showDeleteError();
                    }
                    reloadHistory();
                })
                .show();
    }

    private void confirmDeleteAll() {
        if (!isDeviceUnlocked()) {
            showOwnerUnlockRequired();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_all_visual_history_title)
                .setMessage(R.string.delete_all_visual_history_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete_all, (dialog, which) -> {
                    boolean identitiesDeleted = faceIdentityStore.deleteAll();
                    if (!identitiesDeleted) {
                        showIdentityDeleteError();
                    } else if (!historyStore.deleteAll()) {
                        showDeleteError();
                    }
                    reloadHistory();
                })
                .show();
    }

    private void showDeleteError() {
        Toast.makeText(
                this,
                R.string.visual_history_delete_failed,
                Toast.LENGTH_LONG
        ).show();
    }

    private void confirmForgetPerson(
            VisualHistoryRecord record,
            FaceIdentityRecord identity
    ) {
        if (!isDeviceUnlocked()) {
            showOwnerUnlockRequired();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(
                        R.string.forget_person_title,
                        identity.getName()
                ))
                .setMessage(R.string.forget_person_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_forget_person, (dialog, which) -> {
                    boolean forgotten =
                            faceIdentityStore.forgetBySourceHistoryId(
                                    record.getId()
                            );
                    if (!forgotten) {
                        showIdentityDeleteError();
                    } else if (record.getPersonName() != null
                            && !historyStore.clearPersonName(record.getId())) {
                        showDeleteError();
                    }
                    reloadHistory();
                })
                .show();
    }

    private boolean isDeviceUnlocked() {
        KeyguardManager keyguardManager =
                (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return keyguardManager == null || !keyguardManager.isDeviceLocked();
    }

    private void showOwnerUnlockRequired() {
        Toast.makeText(
                this,
                R.string.owner_unlock_required,
                Toast.LENGTH_LONG
        ).show();
    }

    private void showIdentityDeleteError() {
        Toast.makeText(
                this,
                R.string.face_identity_delete_failed,
                Toast.LENGTH_LONG
        ).show();
    }

    private void choosePetForPhoto(VisualHistoryRecord record) {
        if (!isDeviceUnlocked()) {
            showOwnerUnlockRequired();
            return;
        }
        List<PetMemoryRecord> pets = petMemoryStore.list();
        if (pets.isEmpty()) {
            Toast.makeText(
                    this,
                    R.string.visual_history_no_pets,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }
        String[] labels = new String[pets.size()];
        for (int index = 0; index < pets.size(); index++) {
            PetMemoryRecord pet = pets.get(index);
            labels[index] = getString(
                    R.string.visual_history_pet_choice,
                    pet.getDisplayName(),
                    pet.getKind().name().toLowerCase(java.util.Locale.ROOT)
            );
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.visual_history_choose_pet)
                .setItems(labels, (dialog, which) -> {
                    PetMemoryRecord pet = pets.get(which);
                    if (!historyStore.setSubject(
                            record.getId(),
                            VisualHistoryRecord.SubjectKind.PET,
                            pet.getDisplayName()
                    )) {
                        showSubjectUpdateError();
                    }
                    reloadHistory();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void clearSubjectFromPhoto(VisualHistoryRecord record) {
        if (!isDeviceUnlocked()) {
            showOwnerUnlockRequired();
            return;
        }
        if (!historyStore.clearSubject(record.getId())) {
            showSubjectUpdateError();
        }
        reloadHistory();
    }

    private void showSubjectUpdateError() {
        Toast.makeText(
                this,
                R.string.visual_history_subject_update_failed,
                Toast.LENGTH_LONG
        ).show();
    }

    private TextView textView(String text, int sizeSp, int colorResource) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(getColor(colorResource));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static Bitmap decodeThumbnail(VisualHistoryRecord record) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(record.getImageFile().getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        int sampleSize = 1;
        while (bounds.outWidth / sampleSize > THUMBNAIL_MAX_DIMENSION
                || bounds.outHeight / sampleSize > THUMBNAIL_MAX_DIMENSION) {
            sampleSize *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeFile(record.getImageFile().getAbsolutePath(), options);
    }

    private final class HistoryAdapter extends BaseAdapter {
        private List<VisualHistoryRecord> records = Collections.emptyList();

        void setRecords(List<VisualHistoryRecord> records) {
            this.records = records;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return records.size();
        }

        @Override
        public VisualHistoryRecord getItem(int position) {
            return records.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getCapturedAtEpochMillis();
        }

        @Override
        public View getView(int position, View recycled, ViewGroup parent) {
            HistoryRow row;
            if (recycled == null) {
                row = createHistoryRow();
                recycled = row.root;
                recycled.setTag(row);
            } else {
                row = (HistoryRow) recycled.getTag();
            }

            VisualHistoryRecord record = getItem(position);
            if (VisualHistoryGrouping.startsGroup(records, position)) {
                row.groupHeader.setText(groupHeader(record, position));
                row.groupHeader.setVisibility(View.VISIBLE);
            } else {
                row.groupHeader.setVisibility(View.GONE);
            }
            row.image.setImageDrawable(null);
            Bitmap thumbnail = decodeThumbnail(record);
            if (thumbnail != null) {
                row.image.setImageBitmap(thumbnail);
            } else {
                row.image.setImageResource(android.R.drawable.ic_menu_report_image);
            }
            row.timestamp.setText(DateFormat.getMediumDateFormat(
                    VisualHistoryActivity.this
            ).format(new Date(record.getCapturedAtEpochMillis()))
                    + " · "
                    + DateFormat.getTimeFormat(
                            VisualHistoryActivity.this
                    ).format(new Date(record.getCapturedAtEpochMillis())));
            row.description.setText(record.getDescription());
            FaceIdentityRecord identity =
                    identitiesBySource.get(record.getId());
            String displayedName = identity != null
                    ? identity.getName()
                    : record.getSubjectName();
            VisualHistoryRecord.SubjectKind displayedKind = identity != null
                    ? VisualHistoryRecord.SubjectKind.PERSON
                    : record.getSubjectKind();
            if (displayedName == null) {
                row.personName.setVisibility(View.GONE);
                row.forgetPerson.setVisibility(View.GONE);
            } else {
                row.personName.setText(getString(
                        displayedKind == VisualHistoryRecord.SubjectKind.PET
                                ? R.string.visual_history_pet_name
                                : R.string.visual_history_person_name,
                        displayedName
                ));
                row.personName.setVisibility(View.VISIBLE);
                row.forgetPerson.setVisibility(
                        identity != null ? View.VISIBLE : View.GONE
                );
                if (identity != null) {
                    row.forgetPerson.setOnClickListener(
                            view -> confirmForgetPerson(record, identity)
                    );
                } else {
                    row.forgetPerson.setOnClickListener(null);
                }
            }
            boolean canTagPet = identity == null
                    && displayedKind != VisualHistoryRecord.SubjectKind.PERSON;
            row.tagPet.setText(displayedKind == VisualHistoryRecord.SubjectKind.PET
                    ? R.string.action_change_pet
                    : R.string.action_tag_pet);
            row.tagPet.setVisibility(canTagPet ? View.VISIBLE : View.GONE);
            row.tagPet.setOnClickListener(canTagPet
                    ? view -> choosePetForPhoto(record)
                    : null);
            boolean canClearSubject = identity == null && displayedName != null;
            row.clearSubject.setVisibility(
                    canClearSubject ? View.VISIBLE : View.GONE
            );
            row.clearSubject.setOnClickListener(canClearSubject
                    ? view -> clearSubjectFromPhoto(record)
                    : null);
            row.delete.setOnClickListener(view -> confirmDelete(record));
            return recycled;
        }

        private String groupHeader(VisualHistoryRecord record, int position) {
            int count = VisualHistoryGrouping.groupSize(records, position);
            if (record.getSubjectKind() == VisualHistoryRecord.SubjectKind.PERSON) {
                return getString(
                        R.string.visual_history_person_group,
                        record.getSubjectName(),
                        count
                );
            }
            if (record.getSubjectKind() == VisualHistoryRecord.SubjectKind.PET) {
                return getString(
                        R.string.visual_history_pet_group,
                        record.getSubjectName(),
                        count
                );
            }
            return getString(R.string.visual_history_unknown_group, count);
        }
    }

    private HistoryRow createHistoryRow() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(R.color.kiko_surface));
        background.setCornerRadius(dp(16));
        card.setBackground(background);

        TextView groupHeader = textView("", 20, R.color.kiko_accent);
        groupHeader.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        groupHeader.setPadding(0, 0, 0, dp(10));
        card.addView(groupHeader);

        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackgroundColor(getColor(R.color.kiko_background));
        card.addView(
                image,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(280)
                )
        );

        TextView timestamp = textView("", 14, R.color.kiko_accent);
        timestamp.setPadding(0, dp(12), 0, dp(6));
        card.addView(timestamp);

        TextView description = textView("", 18, R.color.kiko_text);
        description.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(description);

        TextView personName = textView("", 17, R.color.kiko_accent);
        personName.setPadding(0, dp(8), 0, 0);
        personName.setVisibility(View.GONE);
        card.addView(personName);

        Button forgetPerson = new Button(this);
        forgetPerson.setText(R.string.action_forget_person);
        forgetPerson.setVisibility(View.GONE);
        LinearLayout.LayoutParams forgetParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        forgetParams.gravity = Gravity.END;
        card.addView(forgetPerson, forgetParams);

        Button tagPet = new Button(this);
        tagPet.setText(R.string.action_tag_pet);
        LinearLayout.LayoutParams tagPetParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tagPetParams.gravity = Gravity.END;
        card.addView(tagPet, tagPetParams);

        Button clearSubject = new Button(this);
        clearSubject.setText(R.string.action_clear_photo_name);
        clearSubject.setVisibility(View.GONE);
        LinearLayout.LayoutParams clearSubjectParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        clearSubjectParams.gravity = Gravity.END;
        card.addView(clearSubject, clearSubjectParams);

        Button delete = new Button(this);
        delete.setText(R.string.action_delete);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        deleteParams.gravity = Gravity.END;
        card.addView(delete, deleteParams);
        return new HistoryRow(
                card,
                groupHeader,
                image,
                timestamp,
                description,
                personName,
                forgetPerson,
                tagPet,
                clearSubject,
                delete
        );
    }

    private static final class HistoryRow {
        private final View root;
        private final TextView groupHeader;
        private final ImageView image;
        private final TextView timestamp;
        private final TextView description;
        private final TextView personName;
        private final Button forgetPerson;
        private final Button tagPet;
        private final Button clearSubject;
        private final Button delete;

        private HistoryRow(
                View root,
                TextView groupHeader,
                ImageView image,
                TextView timestamp,
                TextView description,
                TextView personName,
                Button forgetPerson,
                Button tagPet,
                Button clearSubject,
                Button delete
        ) {
            this.root = root;
            this.groupHeader = groupHeader;
            this.image = image;
            this.timestamp = timestamp;
            this.description = description;
            this.personName = personName;
            this.forgetPerson = forgetPerson;
            this.tagPet = tagPet;
            this.clearSubject = clearSubject;
            this.delete = delete;
        }
    }
}
