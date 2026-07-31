package com.kiko.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
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
import java.util.Map;

final class LocalFaceRecognizer {
    private static final String TAG = "KikoFaces";
    private static final String INPUT_NAME = "data";
    private static final String OUTPUT_NAME = "fc1";
    private static final int MODEL_SIZE = 112;
    private static final int MAX_DETECTOR_DIMENSION = 1024;
    private static final int MAX_FACES = 2;
    private static final float MIN_FACE_CONFIDENCE = 0.40f;
    // Approximates SFace's 112 px template from its 35.24 px eye spacing.
    private static final float CROP_SIZE_IN_EYE_DISTANCES = 3.18f;
    private static final float CROP_CENTER_Y_OFFSET = 0.125f;
    private static final long[] INPUT_SHAPE = {1, 3, MODEL_SIZE, MODEL_SIZE};
    private static final long[] OUTPUT_SHAPE = {
            1,
            FaceIdentityRecord.EMBEDDING_SIZE
    };

    private final ModelDownloadStore downloads;
    private final FaceIdentityStore identities;

    LocalFaceRecognizer(android.content.Context context) {
        downloads = new ModelDownloadStore(context);
        identities = new FaceIdentityStore(context);
    }

    Result recognize(Bitmap source) {
        ModelSpec model = ModelCatalog.findById(ModelCatalog.FACE_MODEL_ID);
        if (model == null
                || downloads.getSnapshot(model).getState()
                != ModelDownloadStore.DownloadSnapshot.State.DOWNLOADED) {
            return Result.modelMissing();
        }
        File modelFile = downloads.getModelFile(model);
        if (modelFile == null) {
            return Result.modelMissing();
        }

        Bitmap faceCrop = null;
        try {
            faceCrop = cropSingleFace(source);
            if (faceCrop == null) {
                return Result.faceUnavailable();
            }
            float[] embedding = extractEmbedding(faceCrop, modelFile);
            FaceEmbeddingMatcher.Match match = FaceEmbeddingMatcher.match(
                    embedding,
                    identities.list()
            );
            return match.isKnown()
                    ? Result.matched(match.getName())
                    : Result.unknown(embedding);
        } catch (Exception | LinkageError error) {
            Log.e(TAG, "Local face recognition failed", error);
            return Result.error();
        } finally {
            if (faceCrop != null && !faceCrop.isRecycled()) {
                faceCrop.recycle();
            }
        }
    }

    private static Bitmap cropSingleFace(Bitmap source) {
        Bitmap detectorBitmap = createDetectorBitmap(source);
        try {
            FaceDetector.Face[] faces = new FaceDetector.Face[MAX_FACES];
            FaceDetector detector = new FaceDetector(
                    detectorBitmap.getWidth(),
                    detectorBitmap.getHeight(),
                    MAX_FACES
            );
            int count = detector.findFaces(detectorBitmap, faces);
            if (count != 1
                    || faces[0] == null
                    || faces[0].confidence() < MIN_FACE_CONFIDENCE) {
                return null;
            }

            PointF midpoint = new PointF();
            faces[0].getMidPoint(midpoint);
            float eyesDistance = faces[0].eyesDistance();
            if (!Float.isFinite(eyesDistance) || eyesDistance <= 0f) {
                return null;
            }
            float side = Math.min(
                    eyesDistance * CROP_SIZE_IN_EYE_DISTANCES,
                    Math.min(detectorBitmap.getWidth(), detectorBitmap.getHeight())
            );
            if (side < MODEL_SIZE / 2f) {
                return null;
            }
            float centerX = midpoint.x;
            float centerY = midpoint.y + eyesDistance * CROP_CENTER_Y_OFFSET;
            float left = clamp(
                    centerX - side / 2f,
                    0f,
                    detectorBitmap.getWidth() - side
            );
            float top = clamp(
                    centerY - side / 2f,
                    0f,
                    detectorBitmap.getHeight() - side
            );

            Bitmap crop = Bitmap.createBitmap(
                    MODEL_SIZE,
                    MODEL_SIZE,
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(crop);
            Paint paint = new Paint(
                    Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG
            );
            canvas.drawBitmap(
                    detectorBitmap,
                    new Rect(
                            Math.round(left),
                            Math.round(top),
                            Math.round(left + side),
                            Math.round(top + side)
                    ),
                    new Rect(0, 0, MODEL_SIZE, MODEL_SIZE),
                    paint
            );
            return crop;
        } finally {
            if (detectorBitmap != source && !detectorBitmap.isRecycled()) {
                detectorBitmap.recycle();
            }
        }
    }

    private static Bitmap createDetectorBitmap(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        float scale = Math.min(
                1f,
                (float) MAX_DETECTOR_DIMENSION / Math.max(width, height)
        );
        int targetWidth = Math.max(2, Math.round(width * scale));
        if ((targetWidth & 1) != 0) {
            targetWidth--;
        }
        int targetHeight = Math.max(1, Math.round(height * scale));
        Bitmap detectorBitmap = Bitmap.createBitmap(
                targetWidth,
                targetHeight,
                Bitmap.Config.RGB_565
        );
        Canvas canvas = new Canvas(detectorBitmap);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(
                source,
                new Rect(0, 0, width, height),
                new Rect(0, 0, targetWidth, targetHeight),
                paint
        );
        return detectorBitmap;
    }

    private static float[] extractEmbedding(Bitmap bitmap, File modelFile)
            throws Exception {
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
                FloatBuffer input = bitmapToRgbChw(bitmap);
                try (OnnxTensor inputTensor = OnnxTensor.createTensor(
                        environment,
                        input,
                        INPUT_SHAPE
                ); OrtSession.Result result = session.run(
                        Collections.singletonMap(INPUT_NAME, inputTensor)
                )) {
                    OnnxValue outputValue = result.get(OUTPUT_NAME).orElse(null);
                    if (!(outputValue instanceof OnnxTensor)) {
                        throw new IllegalStateException(
                                "Unexpected SFace output value"
                        );
                    }
                    FloatBuffer output =
                            ((OnnxTensor) outputValue).getFloatBuffer();
                    float[] embedding =
                            new float[FaceIdentityRecord.EMBEDDING_SIZE];
                    output.get(embedding);
                    return FaceEmbeddingMatcher.normalize(embedding);
                }
            }
        }
    }

    private static void validateModelContract(OrtSession session) throws Exception {
        Map<String, NodeInfo> inputs = session.getInputInfo();
        Map<String, NodeInfo> outputs = session.getOutputInfo();
        validateTensor(inputs.get(INPUT_NAME), INPUT_SHAPE, "input");
        validateTensor(outputs.get(OUTPUT_NAME), OUTPUT_SHAPE, "output");
    }

    private static void validateTensor(
            NodeInfo node,
            long[] expectedShape,
            String role
    ) {
        if (node == null || !(node.getInfo() instanceof TensorInfo)) {
            throw new IllegalStateException("Missing SFace " + role + " tensor");
        }
        TensorInfo info = (TensorInfo) node.getInfo();
        if (info.type != OnnxJavaType.FLOAT
                || !Arrays.equals(info.getShape(), expectedShape)) {
            throw new IllegalStateException(
                    "Unexpected SFace " + role + " tensor"
            );
        }
    }

    private static FloatBuffer bitmapToRgbChw(Bitmap bitmap) {
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
        ).order(ByteOrder.nativeOrder());
        FloatBuffer floats = storage.asFloatBuffer();
        for (int pixel : pixels) {
            floats.put((pixel >> 16) & 0xff);
        }
        for (int pixel : pixels) {
            floats.put((pixel >> 8) & 0xff);
        }
        for (int pixel : pixels) {
            floats.put(pixel & 0xff);
        }
        floats.rewind();
        return floats;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    static final class Result {
        enum Status {
            MATCHED,
            UNKNOWN,
            MODEL_MISSING,
            FACE_UNAVAILABLE,
            ERROR
        }

        private final Status status;
        private final String name;
        private final float[] enrollmentEmbedding;

        private Result(Status status, String name, float[] enrollmentEmbedding) {
            this.status = status;
            this.name = name;
            this.enrollmentEmbedding = enrollmentEmbedding;
        }

        static Result matched(String name) {
            return new Result(Status.MATCHED, name, null);
        }

        static Result unknown(float[] enrollmentEmbedding) {
            return new Result(
                    Status.UNKNOWN,
                    null,
                    Arrays.copyOf(
                            enrollmentEmbedding,
                            enrollmentEmbedding.length
                    )
            );
        }

        static Result modelMissing() {
            return new Result(Status.MODEL_MISSING, null, null);
        }

        static Result faceUnavailable() {
            return new Result(Status.FACE_UNAVAILABLE, null, null);
        }

        static Result error() {
            return new Result(Status.ERROR, null, null);
        }

        Status getStatus() {
            return status;
        }

        String getName() {
            return name;
        }

        float[] getEnrollmentEmbedding() {
            return enrollmentEmbedding == null
                    ? null
                    : Arrays.copyOf(
                            enrollmentEmbedding,
                            enrollmentEmbedding.length
                    );
        }
    }
}
