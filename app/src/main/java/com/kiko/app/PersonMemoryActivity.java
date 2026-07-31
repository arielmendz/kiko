package com.kiko.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class PersonMemoryActivity extends Activity {
    private PersonMemoryStore memoryStore;
    private PetMemoryStore petMemoryStore;
    private MemoryAdapter adapter;
    private Button deleteAllButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        memoryStore = new PersonMemoryStore(this);
        petMemoryStore = new PetMemoryStore(this);
        adapter = new MemoryAdapter();
        setContentView(createContentView());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isDeviceUnlocked()) {
            showUnlockRequired();
            finish();
            return;
        }
        reload();
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
                getString(R.string.person_memories_title),
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
                getString(R.string.person_memories_explanation),
                16,
                R.color.kiko_muted
        );
        explanation.setPadding(0, dp(16), 0, dp(12));
        root.addView(explanation);

        deleteAllButton = new Button(this);
        deleteAllButton.setText(R.string.action_delete_all_memories);
        deleteAllButton.setOnClickListener(view -> confirmDeleteAll());
        root.addView(deleteAllButton);

        TextView empty = textView(
                getString(R.string.person_memories_empty),
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
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        return root;
    }

    private void reload() {
        List<MemoryEntry> entries = new ArrayList<>();
        for (PersonMemoryRecord person : memoryStore.list()) {
            entries.add(MemoryEntry.forPerson(person));
        }
        for (PetMemoryRecord pet : petMemoryStore.list()) {
            entries.add(MemoryEntry.forPet(pet));
        }
        entries.sort((left, right) -> left.displayName.compareToIgnoreCase(
                right.displayName
        ));
        adapter.setEntries(entries);
        deleteAllButton.setEnabled(
                !entries.isEmpty()
                        || memoryStore.hasStoredData()
                        || petMemoryStore.hasStoredData()
        );
    }

    private void confirmDelete(MemoryEntry entry) {
        if (!isDeviceUnlocked()) {
            showUnlockRequired();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(
                        entry.pet
                                ? R.string.delete_pet_memory_title
                                : R.string.delete_person_memory_title,
                        entry.displayName
                ))
                .setMessage(entry.pet
                        ? R.string.delete_pet_memory_message
                        : R.string.delete_person_memory_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    boolean deleted = entry.pet
                            ? petMemoryStore.delete(entry.canonicalName)
                            : memoryStore.delete(entry.canonicalName);
                    if (!deleted) {
                        showDeleteError();
                    }
                    reload();
                })
                .show();
    }

    private void confirmDeleteAll() {
        if (!isDeviceUnlocked()) {
            showUnlockRequired();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_all_person_memories_title)
                .setMessage(R.string.delete_all_person_memories_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete_all, (dialog, which) -> {
                    boolean peopleDeleted = memoryStore.deleteAll();
                    boolean petsDeleted = petMemoryStore.deleteAll();
                    if (!peopleDeleted || !petsDeleted) {
                        showDeleteError();
                    }
                    reload();
                })
                .show();
    }

    private boolean isDeviceUnlocked() {
        KeyguardManager keyguardManager =
                (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return keyguardManager == null || !keyguardManager.isDeviceLocked();
    }

    private void showUnlockRequired() {
        Toast.makeText(
                this,
                R.string.person_memory_unlock_required,
                Toast.LENGTH_LONG
        ).show();
    }

    private void showDeleteError() {
        Toast.makeText(
                this,
                R.string.person_memory_delete_failed,
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

    private final class MemoryAdapter extends BaseAdapter {
        private List<MemoryEntry> entries = Collections.emptyList();

        void setEntries(List<MemoryEntry> entries) {
            this.entries = entries;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public MemoryEntry getItem(int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).updatedAtEpochMillis;
        }

        @Override
        public View getView(int position, View recycled, ViewGroup parent) {
            MemoryRow row;
            if (recycled == null) {
                row = createRow();
                recycled = row.root;
                recycled.setTag(row);
            } else {
                row = (MemoryRow) recycled.getTag();
            }
            MemoryEntry entry = getItem(position);
            row.name.setText(entry.displayName);
            row.facts.setText(entry.summary);
            Date updated = new Date(entry.updatedAtEpochMillis);
            row.updated.setText(getString(
                    R.string.person_memory_updated_at,
                    DateFormat.getMediumDateFormat(PersonMemoryActivity.this)
                            .format(updated),
                    DateFormat.getTimeFormat(PersonMemoryActivity.this)
                            .format(updated)
            ));
            row.delete.setOnClickListener(view -> confirmDelete(entry));
            return recycled;
        }
    }

    private static final class MemoryEntry {
        private final String canonicalName;
        private final String displayName;
        private final String summary;
        private final long updatedAtEpochMillis;
        private final boolean pet;

        private MemoryEntry(
                String canonicalName,
                String displayName,
                String summary,
                long updatedAtEpochMillis,
                boolean pet
        ) {
            this.canonicalName = canonicalName;
            this.displayName = displayName;
            this.summary = summary;
            this.updatedAtEpochMillis = updatedAtEpochMillis;
            this.pet = pet;
        }

        static MemoryEntry forPerson(PersonMemoryRecord record) {
            return new MemoryEntry(
                    record.getCanonicalName(),
                    record.getDisplayName(),
                    SpanishPersonMemoryResponses.inspectableSummary(record),
                    record.getUpdatedAtEpochMillis(),
                    false
            );
        }

        static MemoryEntry forPet(PetMemoryRecord record) {
            return new MemoryEntry(
                    record.getCanonicalName(),
                    record.getDisplayName(),
                    SpanishPetMemoryResponses.inspectableSummary(record),
                    record.getUpdatedAtEpochMillis(),
                    true
            );
        }
    }

    private MemoryRow createRow() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(R.color.kiko_surface));
        background.setCornerRadius(dp(16));
        card.setBackground(background);

        TextView name = textView("", 22, R.color.kiko_text);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(name);
        TextView facts = textView("", 17, R.color.kiko_text);
        facts.setPadding(0, dp(8), 0, dp(8));
        card.addView(facts);
        TextView updated = textView("", 14, R.color.kiko_accent);
        card.addView(updated);

        Button delete = new Button(this);
        delete.setText(R.string.action_delete);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        deleteParams.gravity = Gravity.END;
        card.addView(delete, deleteParams);
        return new MemoryRow(card, name, facts, updated, delete);
    }

    private static final class MemoryRow {
        private final View root;
        private final TextView name;
        private final TextView facts;
        private final TextView updated;
        private final Button delete;

        private MemoryRow(
                View root,
                TextView name,
                TextView facts,
                TextView updated,
                Button delete
        ) {
            this.root = root;
            this.name = name;
            this.facts = facts;
            this.updated = updated;
            this.delete = delete;
        }
    }
}
