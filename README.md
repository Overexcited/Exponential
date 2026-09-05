# Eigent Mobile Android

Android build layer for a local-first port of Eigent.

This repository is designed to be pushed directly to GitHub. **The repository does not contain model weights.** GitHub Actions fetches the pinned Eigent source, builds its web UI, packages the Python backend with Chaquopy, builds a pinned llama.cpp Android arm64 runtime, and produces an APK artifact.

## What the workflow builds

1. Eigent v1.0.2 (`e478094...`) is fetched from the official repository.
2. llama.cpp `b10516` (`b95502b`) is fetched from the official repository.
3. `llama-server` is cross-compiled for `arm64-v8a`.
4. The Eigent web frontend is built and embedded as APK assets.
5. The Eigent Python backend and CAMEL are packaged with CPython 3.11 through Chaquopy.
6. The APK starts an Android foreground service which owns the Python backend and llama-server processes.
7. GGUF models are downloaded at runtime into app-private storage; they are never placed in the APK.

The current build is intentionally CPU-first. It targets the arm64 Android path needed for the Vivo V29-class device. GPU/OpenCL/QNN acceleration remains a subsequent hardware-tested optimization rather than an unverified build-time assumption.

## GitHub build

Push this directory to a repository with Actions enabled. The workflow runs on pushes to `main` and can also be started manually from **Actions -> Build Eigent Android**.

The successful run publishes a debug APK as an Actions artifact named `Eigent-Mobile-debug-<commit>`.

## Current runtime boundary

```text
Android Activity
    |
    +--> RuntimeService (foreground service)
            |
            +--> embedded CPython / Eigent backend / CAMEL :5001
            |
            +--> llama-server :8080
                    |
                    +--> app-private GGUF model directory
```

The Activity is not the owner of the inference process. Durable job state is stored under the application's `files/jobs/` directory.

## Important status

This is a **buildable first vertical slice**, not the finished Android product. The workflow is deliberately strict about source versions and native-artifact sanity checks, but the resulting APK still needs hardware validation on the Vivo V29.

The next engineering stages are model lifecycle management, resumable downloads and checksums, official llama.cpp Android JNI integration, GeckoView browser capabilities, Android-native terminal/filesystem capabilities, document/verification infrastructure, and full CAMEL local-model execution testing.

## Licensing

Eigent is Apache-2.0 and llama.cpp is MIT. Their source is fetched by the build workflow. Preserve the upstream license/notice requirements when distributing builds.
