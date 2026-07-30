# Local model catalog

Kiko downloads one pinned artifact per catalog entry. Pinning an immutable
repository revision or numeric release-asset ID and verifying the expected
SHA-256 protects the app from silently receiving different bytes when an upstream
changes.

| Model | Artifact | Size | License | Upstream |
| --- | --- | ---: | --- | --- |
| Gemma 3 1B | `gemma-3-1b-it-q4_0.gguf` (Q4_0) | 1,003,541,152 bytes | Gemma | `google/gemma-3-1b-it-qat-q4_0-gguf` |
| Bonsai 1.7B | `Bonsai-1.7B-Q1_0.gguf` (Q1_0) | 248,302,272 bytes | Apache-2.0 | `prism-ml/Bonsai-1.7B-gguf` |
| Qwen3 0.6B | `Qwen3-0.6B-Q8_0.gguf` (Q8_0) | 639,446,688 bytes | Apache-2.0 | `Qwen/Qwen3-0.6B-GGUF` |
| LFM2.5 350M | `LFM2.5-350M-Q4_K_M.gguf` (Q4_K_M) | 229,312,224 bytes | LFM-1.0 | `LiquidAI/LFM2.5-350M-GGUF` |
| YOLO26n | `yolo26n.onnx` (FP32) | 9,941,957 bytes | AGPL-3.0 | Ultralytics release asset `398736502` |

Total catalog storage is 2,130,544,293 bytes, excluding temporary transfer
overhead.

## Integrity pins

| Model | Repository revision | SHA-256 |
| --- | --- | --- |
| Gemma 3 1B | `d1be121d36172a4b0b964657e2ee859d61138593` | `95e5b8d891cd6a794f66c2a6fb59a41e9562b4660560b854274eceffb628b22a` |
| Bonsai 1.7B | `210a9e99f79cb184909d49595906526eb2b3dd9a` | `3d7c6c90dd98717a203adb22d5eacd2581850e40aa5327e144b97766cae5f7e3` |
| Qwen3 0.6B | `23749fefcc72300e3a2ad315e1317431b06b590a` | `9465e63a22add5354d9bb4b99e90117043c7124007664907259bd16d043bb031` |
| LFM2.5 350M | `bb7ee58b243e4cede04187e323e760b04f8a0091` | `7e6f72643caafc9a68256686638c4d7916f2cec76d1df478d4c3ddcd95a6aed4` |
| YOLO26n | GitHub release asset `398736502` from release `v8.4.0` | `2e947b787d9e787b93a16772a5f55b1d4d8c4d86f53146149c5d6a642442d6f7` |

## Access and licenses

- **Gemma:** Hugging Face marks Google's repository as manually gated. The user
  must sign in, accept the Gemma license, and provide Kiko a personal read token.
  The token is encrypted with Android Keystore and can be removed from the model
  screen.
- **Bonsai and Qwen:** Apache-2.0. Their catalog entries use the model publishers'
  repositories.
- **LFM2.5:** governed by Liquid AI's LFM-1.0 license. The source button exposes
  the upstream model card and license.
- **YOLO26n:** AGPL-3.0, as declared by the ONNX metadata and Ultralytics. Kiko is
  consequently licensed AGPL-3.0-only. The catalog downloads through the numeric
  GitHub release-asset API endpoint with the required octet-stream header rather
  than a mutable filename-only URL. A proprietary or commercial deployment
  requires an applicable Ultralytics Enterprise license and a new review.

The user is responsible for reviewing applicable terms before downloading or
using a model.

## Runtime compatibility

The four GGUF language artifacts are still download-only. GGUF storage is not
proof that one future runtime supports every architecture and quantization:

- Gemma 3 and Qwen3 require a runtime version supporting those architectures.
- LFM2.5 requires LFM2 support.
- Bonsai Q1_0 requires Prism ML's specialized 1-bit llama.cpp kernels.

YOLO26n is runnable now. Kiko uses the pinned MIT-licensed ONNX Runtime Android
1.28.0 API directly on CPU. The reviewed Ultralytics 8.4.38 artifact contract is
one `float32` input named `images`, shaped `1 × 3 × 640 × 640`, and one end-to-end
`float32` output named `output0`, shaped `1 × 300 × 6`. Each output row is
`x1, y1, x2, y2, confidence, class`; the model performs its own top-k end-to-end
selection and requires no native NMS.

`LocalVisionEngine` preserves the camera aspect ratio with a centered
`114,114,114` letterbox, converts RGB pixels to normalized NCHW floats, and
validates the tensor names, types, and fixed shapes before inference.
`Yolo26DetectionParser` treats every output row as untrusted, rejects malformed
boxes, non-finite or out-of-range confidence values, and invalid classes, then
maps the contiguous 80-class COCO label order into `SpanishSceneDescription`.

The model binary is not committed. The user explicitly starts its download in
**Modelos locales**; `ModelDownloadStore` checks its exact length and SHA-256
before `LocalVisionEngine` will open it. No unverified file or heuristic fallback
is used for “¿qué ves?”.

Tool calling changes the selection criteria: general chat quality and parameter
count are insufficient for physical control. `docs/MODEL_RESEARCH.md` records the
current recommendation, alternatives, runtime implications, vision strategy, and
device benchmark gate. The language catalog is a download milestone, not a
commitment that the final action router will use GGUF; a fine-tuned
FunctionGemma path would use LiteRT-LM's `.litertlm` format.
