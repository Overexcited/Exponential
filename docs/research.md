# Eigent Android Port - Research Record

Date: 2026-09-05

## Executive conclusion

A fully local Android deployment is technically feasible. The strongest architecture found is:

Android APK -> embedded Eigent Python backend -> CAMEL -> local OpenAI-compatible llama-server -> user-managed GGUF model.

The APK should not contain model weights. Models should be stored separately in app-private storage and managed by an in-app model manager.

## Eigent findings

1. Eigent describes its backend as FastAPI + Uvicorn with CAMEL as the multi-agent framework.
2. The current backend requires Python >=3.11,<3.12.
3. The current backend dependency list includes CAMEL, FastAPI, Uvicorn, HTTPX, NumPy, Qdrant client, PyYAML, OpenTelemetry, and some desktop/runtime-specific dependencies including nodejs-wheel.
4. Eigent has a web-only Vite configuration (`vite.config.web.ts`) explicitly intended for frontend/backend separation without Electron. This is useful for Android because the React UI can be built as static web assets.
5. Eigent's backend has a standalone mode and listens by default on 127.0.0.1:5001. The Electron process normally starts the backend; Android can replace that process-management layer.
6. Eigent already has a llama.cpp provider mapped through CAMEL's OpenAI-compatible model platform. Therefore a local llama-server endpoint is conceptually aligned with the existing model abstraction.
7. Eigent's local deployment documentation explicitly describes fully local use with local models and says a fully offline environment should use only local models and local MCP servers.
8. Desktop browser/login functionality launches Electron and uses CDP. That subsystem cannot simply be carried to Android and needs an Android-specific browser/tool implementation.
9. The current release observed during research is v1.0.2 (July 2026).

## llama.cpp findings

1. llama.cpp has official Android documentation.
2. The official Android documentation describes building Android arm64-v8a binaries with the Android NDK.
3. The official Android example includes a native Android binding for model loading and inference.
4. The official server documentation states that `llama-server` exposes an OpenAI-compatible HTTP API and supports a models directory/router mode.
5. Current llama.cpp documentation supports local GGUF files and direct Hugging Face model downloads.
6. llama.cpp supports 4-bit through 8-bit quantization and CPU/GPU hybrid execution depending on backend.
7. Current Android build workflows in llama.cpp build arm64 Android targets using modern NDKs.
8. A separate 2026 Android project, ServLlama, demonstrates the exact deployment technique needed here: it cross-compiles `llama-server` for Android, packages the executable as a `lib*.so` under jniLibs, then executes it from Android's native library directory. This proves that an Android app can package and manage the same llama-server executable without Termux.
9. ServLlama's model manager keeps GGUF files outside the APK and supports multiple local models. Its architecture is a useful implementation reference.
10. llama.cpp currently supports multimodal models, but Android CPU multimodal performance remains a practical concern; text-only models should be the initial target.

## Python on Android findings

1. Chaquopy is an Android Python SDK and currently supports Python 3.11.
2. Chaquopy 17.0 supports modern Android packaging and devices with 16 KB pages.
3. Chaquopy can install Python packages into an Android application when compatible Android wheels are available.
4. The major risk is not Python itself but native Python dependencies without compatible Android wheels. This must be tested in CI.
5. The initial Android dependency list therefore removes obvious desktop-only runtime dependencies such as nodejs-wheel and debug tooling and tests CAMEL plus the core backend dependencies separately.

## Storage/model-management design

Recommended app-private structure:

`files/models/*.gguf`

The APK contains no model weights. The model manager should provide:

- catalogue/search
- model metadata
- download with progress and resume
- import from Android storage
- delete
- rename
- active model selection
- loaded/unloaded state
- RAM/storage estimates
- compatibility tags for tool calling and agent use

Several models may be stored simultaneously, but only the selected model should normally be loaded into RAM.

## X/social search

Searches were issued against X-indexed results for Eigent, CAMEL, llama.cpp and Android local-LLM deployment. Direct X results were sparse in the search index, so repository/documentation evidence was preferred over relying on social posts for technical claims.

## Security and offline implications

The local API should bind to 127.0.0.1, not 0.0.0.0, by default. This prevents other devices on the LAN from reaching the agent API. If LAN serving is later added, it should be an explicit setting with authentication.

The fully offline mode should disable remote provider defaults, remote MCP endpoints and cloud synchronization. Local MCP servers can remain supported if the user explicitly installs them.

## Principal engineering risks

1. Chaquopy compatibility of the complete Eigent/CAMEL dependency graph.
2. Desktop-only Eigent browser and Electron assumptions.
3. Android process lifetime/background restrictions.
4. Native terminal/tool execution semantics on Android.
5. RAM pressure when loading 4B-14B models.
6. Tool/function calling quality of the selected local model.
7. GPU/NPU acceleration beyond CPU. CPU should be the baseline; Snapdragon/OpenCL/Hexagon backends can be added later.

## Sources

- Eigent repository and README: https://github.com/eigent-ai/eigent
- Eigent web Vite config: https://github.com/eigent-ai/eigent/blob/main/vite.config.web.ts
- Eigent backend pyproject: https://github.com/eigent-ai/eigent/blob/main/backend/pyproject.toml
- Eigent backend standalone main: https://github.com/eigent-ai/eigent/blob/main/backend/main.py
- Eigent local deployment/server documentation: https://github.com/eigent-ai/eigent/blob/main/server/README_EN.md
- Eigent llama.cpp provider discussion/bug audit: https://github.com/eigent-ai/eigent/issues/1578
- llama.cpp Android documentation: https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md
- llama.cpp Android CI: https://github.com/ggml-org/llama.cpp/blob/master/.github/workflows/build-android.yml
- llama.cpp server documentation: https://github.com/ggml-org/llama.cpp/blob/master/tools/server/README.md
- ServLlama: https://github.com/ArkaneFans/servllama
- Chaquopy documentation: https://chaquo.com/chaquopy/documentation/
