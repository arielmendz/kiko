package com.kiko.app;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModelCatalog {
    public static final String VISION_MODEL_ID = "yolo26n";
    public static final String FACE_MODEL_ID = "sface";

    private static final List<ModelSpec> MODELS = Collections.unmodifiableList(
            Arrays.asList(
                    new ModelSpec(
                            "gemma-3-1b",
                            "Gemma 3 1B",
                            "1B",
                            "Q4_0",
                            "google/gemma-3-1b-it-qat-q4_0-gguf",
                            "d1be121d36172a4b0b964657e2ee859d61138593",
                            "gemma-3-1b-it-q4_0.gguf",
                            1_003_541_152L,
                            "95e5b8d891cd6a794f66c2a6fb59a41e9562b4660560b854274eceffb628b22a",
                            "Gemma",
                            true,
                            "Modelo instructivo de Google. Requiere aceptar la licencia de Gemma."
                    ),
                    new ModelSpec(
                            "bonsai-1.7b",
                            "Bonsai 1.7B",
                            "1.7B",
                            "Q1_0",
                            "prism-ml/Bonsai-1.7B-gguf",
                            "210a9e99f79cb184909d49595906526eb2b3dd9a",
                            "Bonsai-1.7B-Q1_0.gguf",
                            248_302_272L,
                            "3d7c6c90dd98717a203adb22d5eacd2581850e40aa5327e144b97766cae5f7e3",
                            "Apache-2.0",
                            false,
                            "Modelo oficial de 1 bit de Prism ML basado en Qwen3."
                    ),
                    new ModelSpec(
                            "qwen3-0.6b",
                            "Qwen3 0.6B",
                            "0.6B",
                            "Q8_0",
                            "Qwen/Qwen3-0.6B-GGUF",
                            "23749fefcc72300e3a2ad315e1317431b06b590a",
                            "Qwen3-0.6B-Q8_0.gguf",
                            639_446_688L,
                            "9465e63a22add5354d9bb4b99e90117043c7124007664907259bd16d043bb031",
                            "Apache-2.0",
                            false,
                            "Modelo instructivo multilingüe oficial de Qwen."
                    ),
                    new ModelSpec(
                            "lfm2.5-350m",
                            "LFM2.5 350M",
                            "350M",
                            "Q4_K_M",
                            "LiquidAI/LFM2.5-350M-GGUF",
                            "bb7ee58b243e4cede04187e323e760b04f8a0091",
                            "LFM2.5-350M-Q4_K_M.gguf",
                            229_312_224L,
                            "7e6f72643caafc9a68256686638c4d7916f2cec76d1df478d4c3ddcd95a6aed4",
                            "LFM-1.0",
                            false,
                            "Modelo compacto de Liquid AI diseñado para ejecución en el dispositivo."
                    ),
                    ModelSpec.directArtifact(
                            VISION_MODEL_ID,
                            "YOLO26n",
                            "80 clases · 2.4M",
                            "FP32 ONNX",
                            "Ultralytics assets",
                            "398736502",
                            "yolo26n.onnx",
                            9_941_957L,
                            "2e947b787d9e787b93a16772a5f55b1d4d8c4d86f53146149c5d6a642442d6f7",
                            "AGPL-3.0",
                            "Detector YOLO local usado por «¿qué ves?».",
                            ModelPurpose.VISION,
                            "https://github.com/ultralytics/assets/releases/tag/v8.4.0",
                            "https://api.github.com/repos/ultralytics/assets/releases/assets/398736502",
                            githubReleaseHeaders()
                    ),
                    ModelSpec.directArtifact(
                            FACE_MODEL_ID,
                            "SFace",
                            "128 dimensiones",
                            "FP32 ONNX",
                            "opencv/face_recognition_sface",
                            "89e1f6f89ab68a12ab974b5b65162abf464a461f",
                            "face_recognition_sface_2021dec.onnx",
                            38_696_353L,
                            "0ba9fbfa01b5270c96627c4ef784da859931e02f04419c829e83484087c34e79",
                            "Apache-2.0",
                            "Embeddings faciales locales para reconocer solo personas confirmadas.",
                            ModelPurpose.FACE_RECOGNITION,
                            "https://huggingface.co/opencv/face_recognition_sface",
                            "https://huggingface.co/opencv/face_recognition_sface/resolve/"
                                    + "89e1f6f89ab68a12ab974b5b65162abf464a461f/"
                                    + "face_recognition_sface_2021dec.onnx?download=true",
                            Collections.emptyMap()
                    )
            )
    );

    private ModelCatalog() {
    }

    public static List<ModelSpec> getModels() {
        return MODELS;
    }

    public static ModelSpec findById(String id) {
        for (ModelSpec model : MODELS) {
            if (model.getId().equals(id)) {
                return model;
            }
        }
        return null;
    }

    private static Map<String, String> githubReleaseHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/octet-stream");
        headers.put("X-GitHub-Api-Version", "2022-11-28");
        return headers;
    }
}
