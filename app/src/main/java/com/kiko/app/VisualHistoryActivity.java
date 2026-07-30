package com.kiko.app;

import android.app.Activity;
import android.app.AlertDialog;
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
import java.util.List;

public final class VisualHistoryActivity extends Activity {
    private static final int THUMBNAIL_MAX_DIMENSION = 960;

    private VisualHistoryStore historyStore;
    private HistoryAdapter adapter;
    private Button deleteAllButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyStore = new VisualHistoryStore(this);
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
        adapter.setRecords(records);
        deleteAllButton.setEnabled(!records.isEmpty());
    }

    private void confirmDelete(VisualHistoryRecord record) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_visual_history_title)
                .setMessage(record.getDescription())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    if (!historyStore.delete(record)) {
                        showDeleteError();
                    }
                    reloadHistory();
                })
                .show();
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_all_visual_history_title)
                .setMessage(R.string.delete_all_visual_history_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete_all, (dialog, which) -> {
                    if (!historyStore.deleteAll()) {
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
            if (record.getPersonName() == null) {
                row.personName.setVisibility(View.GONE);
            } else {
                row.personName.setText(getString(
                        R.string.visual_history_person_name,
                        record.getPersonName()
                ));
                row.personName.setVisibility(View.VISIBLE);
            }
            row.delete.setOnClickListener(view -> confirmDelete(record));
            return recycled;
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
                image,
                timestamp,
                description,
                personName,
                delete
        );
    }

    private static final class HistoryRow {
        private final View root;
        private final ImageView image;
        private final TextView timestamp;
        private final TextView description;
        private final TextView personName;
        private final Button delete;

        private HistoryRow(
                View root,
                ImageView image,
                TextView timestamp,
                TextView description,
                TextView personName,
                Button delete
        ) {
            this.root = root;
            this.image = image;
            this.timestamp = timestamp;
            this.description = description;
            this.personName = personName;
            this.delete = delete;
        }
    }
}
