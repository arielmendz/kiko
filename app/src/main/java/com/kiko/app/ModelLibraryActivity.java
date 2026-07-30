package com.kiko.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ModelLibraryActivity extends Activity {
    private static final String TAG = "KikoModels";
    private static final long REFRESH_INTERVAL_MS = 750L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, ModelCard> cards = new HashMap<>();
    private final Set<String> verifyingModels = new HashSet<>();
    private final Runnable refreshDownloads = this::refreshAllCards;

    private ExecutorService verifier;
    private ModelDownloadStore downloads;
    private HuggingFaceTokenStore tokenStore;
    private Button tokenButton;
    private boolean activityStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        downloads = new ModelDownloadStore(this);
        tokenStore = new HuggingFaceTokenStore(this);
        verifier = Executors.newSingleThreadExecutor();
        setContentView(createContentView());
    }

    @Override
    protected void onStart() {
        super.onStart();
        activityStarted = true;
        updateTokenButton();
        refreshAllCards();
    }

    @Override
    protected void onStop() {
        activityStarted = false;
        handler.removeCallbacks(refreshDownloads);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        verifier.shutdownNow();
        super.onDestroy();
    }

    private View createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.kiko_background));
        root.setPadding(32, 40, 32, 24);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        Button back = new Button(this);
        back.setText(R.string.action_back);
        back.setOnClickListener(view -> finish());
        toolbar.addView(back);

        TextView title = textView(R.string.models_title, 30, R.color.kiko_text);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        titleParams.setMargins(24, 0, 0, 0);
        toolbar.addView(title, titleParams);
        root.addView(toolbar);

        TextView explanation = textView(
                R.string.models_download_only_explanation,
                17,
                R.color.kiko_muted
        );
        explanation.setPadding(0, 24, 0, 16);
        root.addView(explanation);

        tokenButton = new Button(this);
        tokenButton.setOnClickListener(view -> showTokenDialog(null));
        root.addView(tokenButton);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 16, 0, 48);
        for (ModelSpec model : ModelCatalog.getModels()) {
            ModelCard card = createModelCard(model);
            cards.put(model.getId(), card);
            list.addView(card.root);
        }
        scrollView.addView(list);
        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );
        return root;
    }

    private ModelCard createModelCard(ModelSpec model) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(28, 24, 28, 24);
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(R.color.kiko_surface));
        background.setCornerRadius(24f);
        card.setBackground(background);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 20);
        card.setLayoutParams(cardParams);

        TextView name = textView(model.getDisplayName(), 24, R.color.kiko_text);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(name);

        String metadata = getString(
                R.string.model_metadata,
                model.getParameters(),
                model.getQuantization(),
                Formatter.formatShortFileSize(this, model.getByteSize()),
                model.getLicense()
        );
        TextView metadataView = textView(metadata, 15, R.color.kiko_accent);
        metadataView.setPadding(0, 8, 0, 8);
        card.addView(metadataView);

        TextView description = textView(model.getDescription(), 16, R.color.kiko_muted);
        card.addView(description);

        TextView status = textView(R.string.download_not_downloaded, 15, R.color.kiko_text);
        status.setPadding(0, 18, 0, 6);
        card.addView(status);

        ProgressBar progress = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );
        progress.setMax(100);
        progress.setVisibility(View.GONE);
        card.addView(
                progress,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.setPadding(0, 12, 0, 0);

        Button source = new Button(this);
        source.setText(R.string.action_source);
        source.setOnClickListener(view -> openSource(model));
        actions.addView(source);

        Button primary = new Button(this);
        primary.setText(R.string.action_download);
        primary.setOnClickListener(view -> handlePrimaryAction(model));
        actions.addView(primary);
        card.addView(actions);

        return new ModelCard(card, status, progress, primary);
    }

    private void handlePrimaryAction(ModelSpec model) {
        ModelDownloadStore.DownloadSnapshot snapshot = downloads.getSnapshot(model);
        switch (snapshot.getState()) {
            case DOWNLOADING:
            case PAUSED:
            case READY_TO_VERIFY:
                downloads.cancel(model);
                verifyingModels.remove(model.getId());
                refreshAllCards();
                return;
            case DOWNLOADED:
            case CORRUPT:
                confirmDelete(model);
                return;
            default:
                if (model.isGated() && !tokenStore.hasToken()) {
                    showTokenDialog(model);
                } else {
                    startDownload(model);
                }
        }
    }

    private void startDownload(ModelSpec model) {
        if (!downloads.hasEnoughSpace(model)) {
            showMessage(getString(R.string.download_insufficient_space));
            return;
        }

        try {
            String token = model.isGated() ? tokenStore.loadToken() : null;
            downloads.enqueue(model, token);
            refreshAllCards();
        } catch (Exception error) {
            Log.e(TAG, "Could not enqueue " + model.getId(), error);
            showMessage(getString(R.string.download_start_failed));
        }
    }

    private void confirmDelete(ModelSpec model) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_model_title, model.getDisplayName()))
                .setMessage(R.string.delete_model_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    if (!downloads.delete(model)) {
                        showMessage(getString(R.string.delete_model_failed));
                    }
                    refreshAllCards();
                })
                .show();
    }

    private void refreshAllCards() {
        handler.removeCallbacks(refreshDownloads);
        for (ModelSpec model : ModelCatalog.getModels()) {
            refreshCard(model);
        }
        if (activityStarted) {
            handler.postDelayed(refreshDownloads, REFRESH_INTERVAL_MS);
        }
    }

    private void refreshCard(ModelSpec model) {
        ModelCard card = cards.get(model.getId());
        if (card == null) {
            return;
        }

        if (verifyingModels.contains(model.getId())) {
            card.status.setText(R.string.download_verifying);
            card.progress.setIndeterminate(true);
            card.progress.setVisibility(View.VISIBLE);
            card.primary.setText(R.string.action_cancel);
            return;
        }

        ModelDownloadStore.DownloadSnapshot snapshot = downloads.getSnapshot(model);
        card.progress.setIndeterminate(false);
        switch (snapshot.getState()) {
            case DOWNLOADED:
                card.status.setText(R.string.download_ready);
                card.progress.setVisibility(View.GONE);
                card.primary.setText(R.string.action_delete);
                break;
            case DOWNLOADING:
                showDownloadProgress(card, snapshot, R.string.download_in_progress);
                card.primary.setText(R.string.action_cancel_download);
                break;
            case PAUSED:
                showDownloadProgress(card, snapshot, R.string.download_paused);
                card.primary.setText(R.string.action_cancel_download);
                break;
            case READY_TO_VERIFY:
                beginVerification(model);
                break;
            case FAILED:
                card.status.setText(getString(
                        R.string.download_failed,
                        snapshot.getReason()
                ));
                card.progress.setVisibility(View.GONE);
                card.primary.setText(R.string.action_retry);
                break;
            case CORRUPT:
                card.status.setText(R.string.download_corrupt);
                card.progress.setVisibility(View.GONE);
                card.primary.setText(R.string.action_delete);
                break;
            default:
                card.status.setText(model.isGated()
                        ? R.string.download_gated
                        : R.string.download_not_downloaded);
                card.progress.setVisibility(View.GONE);
                card.primary.setText(R.string.action_download);
                break;
        }
    }

    private void showDownloadProgress(
            ModelCard card,
            ModelDownloadStore.DownloadSnapshot snapshot,
            int statusResource
    ) {
        int percent = snapshot.getProgressPercent();
        String downloaded = Formatter.formatShortFileSize(
                this,
                Math.max(0L, snapshot.getDownloadedBytes())
        );
        String total = snapshot.getTotalBytes() > 0L
                ? Formatter.formatShortFileSize(this, snapshot.getTotalBytes())
                : "—";
        card.status.setText(getString(statusResource, percent, downloaded, total));
        card.progress.setProgress(percent);
        card.progress.setVisibility(View.VISIBLE);
    }

    private void beginVerification(ModelSpec model) {
        if (!verifyingModels.add(model.getId())) {
            return;
        }
        refreshCard(model);
        verifier.execute(() -> {
            boolean valid = false;
            try {
                valid = downloads.verifyAndFinalize(model);
            } catch (Exception error) {
                Log.e(TAG, "Could not verify " + model.getId(), error);
            }
            boolean verified = valid;
            handler.post(() -> {
                verifyingModels.remove(model.getId());
                if (!verified) {
                    showMessage(getString(
                            R.string.download_checksum_failed,
                            model.getDisplayName()
                    ));
                }
                refreshAllCards();
            });
        });
    }

    private void showTokenDialog(ModelSpec pendingDownload) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = 48;
        content.setPadding(padding, 0, padding, 0);

        TextView instructions = textView(
                R.string.hf_token_instructions,
                16,
                R.color.kiko_text
        );
        content.addView(instructions);

        Button accessButton = new Button(this);
        accessButton.setText(R.string.action_open_gemma_access);
        accessButton.setOnClickListener(view ->
                openSource(ModelCatalog.findById("gemma-3-1b"))
        );
        content.addView(accessButton);

        EditText tokenInput = new EditText(this);
        tokenInput.setHint(R.string.hf_token_hint);
        tokenInput.setSingleLine(true);
        tokenInput.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        content.addView(tokenInput);

        if (tokenStore.hasToken()) {
            Button clearButton = new Button(this);
            clearButton.setText(R.string.action_clear_token);
            clearButton.setOnClickListener(view -> {
                tokenStore.clearToken();
                updateTokenButton();
                showMessage(getString(R.string.hf_token_cleared));
            });
            content.addView(clearButton);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.hf_token_title)
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String token = tokenInput.getText().toString().trim();
                if (token.isEmpty()) {
                    tokenInput.setError(getString(R.string.hf_token_required));
                    return;
                }
                try {
                    tokenStore.saveToken(token);
                    tokenInput.setText("");
                    updateTokenButton();
                    dialog.dismiss();
                    if (pendingDownload != null) {
                        startDownload(pendingDownload);
                    }
                } catch (Exception error) {
                    Log.e(TAG, "Could not store Hugging Face token", error);
                    showMessage(getString(R.string.hf_token_save_failed));
                }
            });
        });
        dialog.show();
    }

    private void updateTokenButton() {
        tokenButton.setText(tokenStore.hasToken()
                ? R.string.hf_token_configured
                : R.string.hf_token_not_configured);
    }

    private void openSource(ModelSpec model) {
        if (model == null) {
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(model.getSourceUrl())));
    }

    private TextView textView(int textResource, int sizeSp, int colorResource) {
        return textView(getString(textResource), sizeSp, colorResource);
    }

    private TextView textView(String text, int sizeSp, int colorResource) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(getColor(colorResource));
        return view;
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private static final class ModelCard {
        private final View root;
        private final TextView status;
        private final ProgressBar progress;
        private final Button primary;

        private ModelCard(
                View root,
                TextView status,
                ProgressBar progress,
                Button primary
        ) {
            this.root = root;
            this.status = status;
            this.progress = progress;
            this.primary = primary;
        }
    }
}
