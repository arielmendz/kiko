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

import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class PersonMemoryActivity extends Activity {
    private PersonMemoryStore memoryStore;
    private MemoryAdapter adapter;
    private Button deleteAllButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        memoryStore = new PersonMemoryStore(this);
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
        List<PersonMemoryRecord> records = memoryStore.list();
        adapter.setRecords(records);
        deleteAllButton.setEnabled(
                !records.isEmpty() || memoryStore.hasStoredData()
        );
    }

    private void confirmDelete(PersonMemoryRecord record) {
        if (!isDeviceUnlocked()) {
            showUnlockRequired();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(
                        R.string.delete_person_memory_title,
                        record.getDisplayName()
                ))
                .setMessage(R.string.delete_person_memory_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    if (!memoryStore.delete(record.getCanonicalName())) {
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
                    if (!memoryStore.deleteAll()) {
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
        private List<PersonMemoryRecord> records = Collections.emptyList();

        void setRecords(List<PersonMemoryRecord> records) {
            this.records = records;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return records.size();
        }

        @Override
        public PersonMemoryRecord getItem(int position) {
            return records.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getUpdatedAtEpochMillis();
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
            PersonMemoryRecord record = getItem(position);
            row.name.setText(record.getDisplayName());
            row.facts.setText(
                    SpanishPersonMemoryResponses.inspectableSummary(record)
            );
            Date updated = new Date(record.getUpdatedAtEpochMillis());
            row.updated.setText(getString(
                    R.string.person_memory_updated_at,
                    DateFormat.getMediumDateFormat(PersonMemoryActivity.this)
                            .format(updated),
                    DateFormat.getTimeFormat(PersonMemoryActivity.this)
                            .format(updated)
            ));
            row.delete.setOnClickListener(view -> confirmDelete(record));
            return recycled;
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
