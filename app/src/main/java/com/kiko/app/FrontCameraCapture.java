package com.kiko.app;

import android.graphics.Bitmap;

import androidx.activity.ComponentActivity;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public final class FrontCameraCapture {
    public interface Callback {
        void onCaptured(Bitmap bitmap, int rotationDegrees);

        void onError();
    }

    private final ComponentActivity activity;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private boolean captureActive;

    public FrontCameraCapture(ComponentActivity activity) {
        this.activity = activity;
    }

    public void capture(Callback callback) {
        cancel();
        captureActive = true;
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(activity);
        providerFuture.addListener(
                () -> bindAndCapture(providerFuture, callback),
                activity.getMainExecutor()
        );
    }

    public void cancel() {
        captureActive = false;
        if (cameraProvider != null && imageCapture != null) {
            cameraProvider.unbind(imageCapture);
        }
        imageCapture = null;
    }

    private void bindAndCapture(
            ListenableFuture<ProcessCameraProvider> providerFuture,
            Callback callback
    ) {
        if (!captureActive) {
            return;
        }

        try {
            cameraProvider = providerFuture.get();
            if (!cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                fail(callback);
                return;
            }

            imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();

            cameraProvider.bindToLifecycle(
                    activity,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageCapture
            );
            imageCapture.takePicture(
                    activity.getMainExecutor(),
                    new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(ImageProxy image) {
                            if (!captureActive) {
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
                                fail(callback);
                                return;
                            }
                            image.close();
                            releaseCamera();
                            callback.onCaptured(bitmap, rotationDegrees);
                        }

                        @Override
                        public void onError(ImageCaptureException exception) {
                            fail(callback);
                        }
                    }
            );
        } catch (CameraInfoUnavailableException
                 | ExecutionException
                 | InterruptedException
                 | RuntimeException error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            fail(callback);
        }
    }

    private void fail(Callback callback) {
        boolean shouldNotify = captureActive;
        releaseCamera();
        if (shouldNotify) {
            callback.onError();
        }
    }

    private void releaseCamera() {
        captureActive = false;
        if (cameraProvider != null && imageCapture != null) {
            cameraProvider.unbind(imageCapture);
        }
        imageCapture = null;
    }
}
