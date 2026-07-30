package com.kiko.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public final class LocalVisionEngine implements AutoCloseable {
    private static final String TAG = "KikoVision";
    private static final String INPUT_NAME = "images";
    private static final String OUTPUT_NAME = "output0";
    private static final int MODEL_SIZE = 640;
    private static final int OUTPUT_DETECTIONS = 300;
    private static final int OUTPUT_VALUES_PER_DETECTION = 6;
    private static final long[] INPUT_SHAPE = {1, 3, MODEL_SIZE, MODEL_SIZE};
    private static final long[] OUTPUT_SHAPE = {
            1,
            OUTPUT_DETECTIONS,
            OUTPUT_VALUES_PER_DETECTION
    };
    private static final int LETTERBOX_COLOR = Color.rgb(114, 114, 114);

    public interface Callback {
        void onDescription(
                String description,
                boolean personDetected,
                String historyRecordId
        );

        void onModelMissing(boolean historySaved);

        void onError(boolean historySaved);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ModelDownloadStore downloads;
    private final VisualHistoryStore visualHistory;
    private final String modelMissingDescription;
    private final String personQuestion;
    private final String visionErrorDescription;
    private volatile boolean closed;

    public LocalVisionEngine(Context context) {
        downloads = new ModelDownloadStore(context);
        visualHistory = new VisualHistoryStore(context);
        modelMissingDescription = context.getString(
                R.string.scene_vision_model_missing_response
        );
        personQuestion = context.getString(R.string.scene_person_question);
        visionErrorDescription = context.getString(
                R.string.scene_vision_error_response
        );
    }

    public boolean isModelReady() {
        ModelSpec model = ModelCatalog.findById(ModelCatalog.VISION_MODEL_ID);
        return model != null
                && downloads.getSnapshot(model).getState()
                == ModelDownloadStore.DownloadSnapshot.State.DOWNLOADED;
    }

    public void describe(
            Bitmap bitmap,
            int rotationDegrees,
            long capturedAtEpochMillis,
            Callback callback
    ) {
        if (closed) {
            bitmap.recycle();
            callback.onError(false);
            return;
        }

        try {
            executor.execute(() -> analyze(
                    bitmap,
                    rotationDegrees,
                    capturedAtEpochMillis,
                    callback
            ));
        } catch (RejectedExecutionException error) {
            bitmap.recycle();
            if (!closed) {
                callback.onError(false);
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        executor.shutdownNow();
    }

    private void analyze(
            Bitmap source,
            int rotationDegrees,
            long capturedAtEpochMillis,
            Callback callback
    ) {
        Bitmap oriented = null;
        Bitmap modelBitmap = null;
        try {
            oriented = rotate(source, rotationDegrees);
            ModelSpec model = ModelCatalog.findById(ModelCatalog.VISION_MODEL_ID);
            if (model == null || !isModelReady()) {
                postModelMissing(
                        callback,
                        saveHistory(
                                oriented,
                                capturedAtEpochMillis,
                                modelMissingDescription
                        ) != null
                );
                return;
            }

            File modelFile = downloads.getModelFile(model);
            if (modelFile == null) {
                postModelMissing(
                        callback,
                        saveHistory(
                                oriented,
                                capturedAtEpochMillis,
                                modelMissingDescription
                        ) != null
                );
                return;
            }

            modelBitmap = letterbox(oriented);
            List<SceneLabel> labels;
            OrtEnvironment environment = OrtEnvironment.getEnvironment();
            try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
                options.setIntraOpNumThreads(
                        Math.min(4, Runtime.getRuntime().availableProcessors())
                );
                try (OrtSession session = environment.createSession(
                        modelFile.getAbsolutePath(),
                        options
                )) {
                    validateModelContract(session);
                    FloatBuffer input = bitmapToNormalizedChw(modelBitmap);
                    try (OnnxTensor inputTensor = OnnxTensor.createTensor(
                            environment,
                            input,
                            INPUT_SHAPE
                    ); OrtSession.Result result = session.run(
                            Collections.singletonMap(INPUT_NAME, inputTensor)
                    )) {
                        OnnxValue outputValue = result.get(0);
                        if (!(outputValue instanceof OnnxTensor)) {
                            throw new IllegalStateException(
                                    "Unexpected YOLO output value"
                            );
                        }
                        FloatBuffer output = ((OnnxTensor) outputValue).getFloatBuffer();
                        labels = Yolo26DetectionParser.parse(
                                output,
                                OUTPUT_DETECTIONS,
                                OUTPUT_VALUES_PER_DETECTION
                        );
                    }
                }
            }

            boolean personDetected =
                    SpanishSceneDescription.containsPerson(labels);
            String description = personDetected
                    ? personQuestion
                    : SpanishSceneDescription.describe(labels);
            VisualHistoryRecord historyRecord = saveHistory(
                    oriented,
                    capturedAtEpochMillis,
                    description
            );
            mainHandler.post(() -> {
                if (!closed) {
                    callback.onDescription(
                            description,
                            personDetected,
                            historyRecord == null ? null : historyRecord.getId()
                    );
                }
            });
        } catch (Exception | LinkageError error) {
            Log.e(TAG, "Local scene analysis failed", error);
            Bitmap historyBitmap = oriented != null ? oriented : source;
            boolean historySaved = saveHistory(
                    historyBitmap,
                    capturedAtEpochMillis,
                    visionErrorDescription
            ) != null;
            mainHandler.post(() -> {
                if (!closed) {
                    callback.onError(historySaved);
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

    private VisualHistoryRecord saveHistory(
            Bitmap bitmap,
            long capturedAtEpochMillis,
            String description
    ) {
        try {
            return visualHistory.save(bitmap, capturedAtEpochMillis, description);
        } catch (Exception error) {
            Log.e(TAG, "Could not save visual history capture", error);
            return null;
        }
    }

    private void postModelMissing(Callback callback, boolean historySaved) {
        mainHandler.post(() -> {
            if (!closed) {
                callback.onModelMissing(historySaved);
            }
        });
    }

    private static void validateModelContract(OrtSession session) throws Exception {
        Map<String, NodeInfo> inputs = session.getInputInfo();
        Map<String, NodeInfo> outputs = session.getOutputInfo();
        if (inputs.size() != 1 || outputs.size() != 1) {
            throw new IllegalStateException("Unexpected YOLO input/output count");
        }
        validateTensor(inputs.get(INPUT_NAME), INPUT_SHAPE, "input");
        validateTensor(outputs.get(OUTPUT_NAME), OUTPUT_SHAPE, "output");
    }

    private static void validateTensor(
            NodeInfo node,
            long[] expectedShape,
            String role
    ) {
        if (node == null || !(node.getInfo() instanceof TensorInfo)) {
            throw new IllegalStateException("Missing YOLO " + role + " tensor");
        }
        TensorInfo info = (TensorInfo) node.getInfo();
        if (info.type != OnnxJavaType.FLOAT
                || !Arrays.equals(info.getShape(), expectedShape)) {
            throw new IllegalStateException("Unexpected YOLO " + role + " tensor");
        }
    }

    private static FloatBuffer bitmapToNormalizedChw(Bitmap bitmap) {
        int[] pixels = new int[MODEL_SIZE * MODEL_SIZE];
        bitmap.getPixels(
                pixels,
                0,
                MODEL_SIZE,
                0,
                0,
                MODEL_SIZE,
                MODEL_SIZE
        );

        ByteBuffer storage = ByteBuffer.allocateDirect(
                MODEL_SIZE * MODEL_SIZE * 3 * Float.BYTES
        )
                .order(ByteOrder.nativeOrder());
        FloatBuffer floats = storage.asFloatBuffer();
        for (int pixel : pixels) {
            floats.put(((pixel >> 16) & 0xff) / 255f);
        }
        for (int pixel : pixels) {
            floats.put(((pixel >> 8) & 0xff) / 255f);
        }
        for (int pixel : pixels) {
            floats.put((pixel & 0xff) / 255f);
        }
        floats.rewind();
        return floats;
    }

    private static Bitmap letterbox(Bitmap source) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            throw new IllegalArgumentException("Invalid source bitmap");
        }

        float scale = Math.min(
                (float) MODEL_SIZE / sourceWidth,
                (float) MODEL_SIZE / sourceHeight
        );
        int targetWidth = Math.round(sourceWidth * scale);
        int targetHeight = Math.round(sourceHeight * scale);
        int left = (MODEL_SIZE - targetWidth) / 2;
        int top = (MODEL_SIZE - targetHeight) / 2;

        Bitmap output = Bitmap.createBitmap(
                MODEL_SIZE,
                MODEL_SIZE,
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(output);
        canvas.drawColor(LETTERBOX_COLOR);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(
                source,
                new Rect(0, 0, sourceWidth, sourceHeight),
                new RectF(left, top, left + targetWidth, top + targetHeight),
                paint
        );
        return output;
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
