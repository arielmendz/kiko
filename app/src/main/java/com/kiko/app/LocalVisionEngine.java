package com.kiko.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public final class LocalVisionEngine implements AutoCloseable {
    private static final int OUTPUT_CATEGORIES = 1;
    private static final int OUTPUT_SCORES = 2;
    private static final int OUTPUT_COUNT = 3;
    public interface Callback {
        void onDescription(String description);

        void onModelMissing();

        void onError();
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ModelDownloadStore downloads;
    private volatile boolean closed;

    public LocalVisionEngine(Context context) {
        downloads = new ModelDownloadStore(context);
    }

    public boolean isModelReady() {
        ModelSpec model = ModelCatalog.findById(ModelCatalog.VISION_MODEL_ID);
        return model != null
                && downloads.getSnapshot(model).getState()
                == ModelDownloadStore.DownloadSnapshot.State.DOWNLOADED;
    }

    public void describe(Bitmap bitmap, int rotationDegrees, Callback callback) {
        if (closed) {
            bitmap.recycle();
            callback.onError();
            return;
        }

        try {
            executor.execute(() -> analyze(bitmap, rotationDegrees, callback));
        } catch (RejectedExecutionException error) {
            bitmap.recycle();
            if (!closed) {
                callback.onError();
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        executor.shutdownNow();
    }

    private void analyze(Bitmap source, int rotationDegrees, Callback callback) {
        Bitmap oriented = null;
        Bitmap modelBitmap = null;
        try {
            ModelSpec model = ModelCatalog.findById(ModelCatalog.VISION_MODEL_ID);
            if (model == null || !isModelReady()) {
                postModelMissing(callback);
                return;
            }

            File modelFile = downloads.getModelFile(model);
            if (modelFile == null) {
                postModelMissing(callback);
                return;
            }

            oriented = rotate(source, rotationDegrees);
            List<SceneLabel> labels;
            try (Interpreter interpreter = new Interpreter(
                    modelFile,
                    new Interpreter.Options()
                            .setNumThreads(Math.min(4, Runtime.getRuntime()
                                    .availableProcessors()))
                            .setUseXNNPACK(true)
            )) {
                interpreter.allocateTensors();
                Tensor inputTensor = interpreter.getInputTensor(0);
                int[] inputShape = inputTensor.shape();
                validateModelContract(interpreter, inputTensor, inputShape);

                int inputHeight = inputShape[1];
                int inputWidth = inputShape[2];
                modelBitmap = Bitmap.createScaledBitmap(
                        oriented,
                        inputWidth,
                        inputHeight,
                        true
                );

                ByteBuffer input = bitmapToRgb(modelBitmap, inputWidth, inputHeight);
                Map<Integer, Object> outputs = allocateOutputs(interpreter);
                interpreter.runForMultipleInputsOutputs(
                        new Object[]{input},
                        outputs
                );
                labels = CocoDetectionParser.parse(
                        (ByteBuffer) outputs.get(OUTPUT_CATEGORIES),
                        (ByteBuffer) outputs.get(OUTPUT_SCORES),
                        (ByteBuffer) outputs.get(OUTPUT_COUNT)
                );
            }

            String description = SpanishSceneDescription.describe(labels);
            mainHandler.post(() -> {
                if (!closed) {
                    callback.onDescription(description);
                }
            });
        } catch (RuntimeException | LinkageError error) {
            mainHandler.post(() -> {
                if (!closed) {
                    callback.onError();
                }
            });
        } finally {
            recycleIfDistinct(modelBitmap, source, oriented);
            recycleIfDistinct(oriented, source, null);
            if (!source.isRecycled()) {
                source.recycle();
            }
        }
    }

    private void postModelMissing(Callback callback) {
        mainHandler.post(() -> {
            if (!closed) {
                callback.onModelMissing();
            }
        });
    }

    private static void validateModelContract(
            Interpreter interpreter,
            Tensor inputTensor,
            int[] inputShape
    ) {
        if (interpreter.getInputTensorCount() != 1
                || interpreter.getOutputTensorCount() != 4
                || inputTensor.dataType() != DataType.UINT8
                || inputShape.length != 4
                || inputShape[0] != 1
                || inputShape[3] != 3) {
            throw new IllegalStateException("Unexpected vision model contract");
        }

        for (int index = 0; index < 4; index++) {
            if (interpreter.getOutputTensor(index).dataType() != DataType.FLOAT32) {
                throw new IllegalStateException("Unexpected vision output type");
            }
        }
    }

    private static Map<Integer, Object> allocateOutputs(Interpreter interpreter) {
        Map<Integer, Object> outputs = new HashMap<>();
        for (int index = 0; index < interpreter.getOutputTensorCount(); index++) {
            outputs.put(
                    index,
                    ByteBuffer.allocateDirect(interpreter.getOutputTensor(index).numBytes())
                            .order(ByteOrder.nativeOrder())
            );
        }
        return outputs;
    }

    private static ByteBuffer bitmapToRgb(Bitmap bitmap, int width, int height) {
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        ByteBuffer input = ByteBuffer.allocateDirect(width * height * 3)
                .order(ByteOrder.nativeOrder());
        for (int pixel : pixels) {
            input.put((byte) ((pixel >> 16) & 0xff));
            input.put((byte) ((pixel >> 8) & 0xff));
            input.put((byte) (pixel & 0xff));
        }
        input.rewind();
        return input;
    }

    private static Bitmap rotate(Bitmap source, int rotationDegrees) {
        if (rotationDegrees == 0) {
            return source;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(
                source,
                0,
                0,
                source.getWidth(),
                source.getHeight(),
                matrix,
                true
        );
    }

    private static void recycleIfDistinct(
            Bitmap bitmap,
            Bitmap first,
            Bitmap second
    ) {
        if (bitmap != null && bitmap != first && bitmap != second && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
