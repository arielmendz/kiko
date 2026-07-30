package com.kiko.app;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.ComponentActivity;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public final class SceneCameraCapture {
    private static final long LIVE_PREVIEW_DURATION_MS = 1_200L;

    public interface Callback {
        void onCaptured(Bitmap bitmap, int rotationDegrees);

        void onError();
    }

    private final ComponentActivity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageCapture imageCapture;
    private Runnable pendingCapture;
    private boolean captureActive;
    private long captureGeneration;

    public SceneCameraCapture(ComponentActivity activity) {
        this.activity = activity;
    }

    public void capture(PreviewView previewView, Callback callback) {
        cancel();
        captureActive = true;
        long generation = ++captureGeneration;
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(activity);
        providerFuture.addListener(
                () -> bindAndCapture(
                        providerFuture,
                        previewView,
                        callback,
                        generation
                ),
                activity.getMainExecutor()
        );
    }

    public void cancel() {
        captureGeneration++;
        captureActive = false;
        releaseUseCases();
    }

    private void bindAndCapture(
            ListenableFuture<ProcessCameraProvider> providerFuture,
            PreviewView previewView,
            Callback callback,
            long generation
    ) {
        if (!isActive(generation)) {
            return;
        }

        try {
            cameraProvider = providerFuture.get();
            if (!cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                fail(callback, generation);
                return;
            }

            preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();

            cameraProvider.bindToLifecycle(
                    activity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
            );
            pendingCapture = () -> takePicture(callback, generation);
            mainHandler.postDelayed(pendingCapture, LIVE_PREVIEW_DURATION_MS);
        } catch (CameraInfoUnavailableException
                 | ExecutionException
                 | InterruptedException
                 | RuntimeException error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            fail(callback, generation);
        }
    }

    private void takePicture(Callback callback, long generation) {
        pendingCapture = null;
        if (!isActive(generation) || imageCapture == null) {
            return;
        }
        imageCapture.takePicture(
                activity.getMainExecutor(),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(ImageProxy image) {
                        if (!isActive(generation)) {
                            image.close();
                            return;
                        }

                        Bitmap bitmap;
                        int rotationDegrees;
                        try {
                            bitmap = image.toBitmap();
                            rotationDegrees =
                                    image.getImageInfo().getRotationDegrees();
                        } catch (RuntimeException error) {
                            image.close();
                            fail(callback, generation);
                            return;
                        }
                        image.close();
                        releaseCamera(generation);
                        callback.onCaptured(bitmap, rotationDegrees);
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        fail(callback, generation);
                    }
                }
        );
    }

    private void fail(Callback callback, long generation) {
        boolean shouldNotify = isActive(generation);
        releaseCamera(generation);
        if (shouldNotify) {
            callback.onError();
        }
    }

    private void releaseCamera(long generation) {
        if (!isActive(generation)) {
            return;
        }
        captureActive = false;
        releaseUseCases();
    }

    private boolean isActive(long generation) {
        return captureActive && captureGeneration == generation;
    }

    private void releaseUseCases() {
        if (pendingCapture != null) {
            mainHandler.removeCallbacks(pendingCapture);
            pendingCapture = null;
        }
        if (cameraProvider != null) {
            if (preview != null) {
                cameraProvider.unbind(preview);
            }
            if (imageCapture != null) {
                cameraProvider.unbind(imageCapture);
            }
        }
        preview = null;
        imageCapture = null;
    }
}
