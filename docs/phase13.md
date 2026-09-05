# Eigent Mobile Android - Phase 13: First Runtime Build

## Purpose

This phase converts the Phase 1 prototype from Activity-owned processes into an Android-runtime-owned first vertical slice.

## Implemented

- `RuntimeService` owns the Python backend and llama-server process.
- `MainActivity` starts the service but no longer owns or destroys llama-server.
- `JobEngine` persists job state atomically under `files/jobs/<job-id>/state.json`.
- `NotificationController` exposes an ongoing foreground notification.
- `START_STICKY` is used for runtime recovery semantics.
- `onTaskRemoved()` deliberately leaves the runtime service alive.
- Runtime diagnostics expose process/API/ABI/storage state.
- Model downloads use `.part` then atomic rename instead of writing directly to the final GGUF.
- `POST_NOTIFICATIONS` and a declared `dataSync` foreground-service type are present.
- CAMEL is aligned with the v1.0.2 upstream backend baseline (`0.2.91a5`) rather than the prototype's accidental `a7`.
- A native-library alignment inspection is added to CI as an early 16-KB compatibility gate.

## Important boundary

This is intentionally the first executable vertical slice, not the final architecture. The llama-server executable is still launched as a native child process because the Phase 1 prototype already supplies that artifact. The next implementation step is to replace this with the official llama.cpp Android JNI binding where it gives better lifecycle/memory control, while retaining an OpenAI-compatible local boundary for CAMEL compatibility.

## Verification required on hardware

1. Install APK on the Vivo V29.
2. Start runtime and confirm notification.
3. Confirm `127.0.0.1:5001/health` remains available.
4. Confirm llama-server starts and exposes the local model endpoint.
5. Start an actual local model request.
6. Press Home and turn the screen off.
7. Confirm inference/job progress continues.
8. Swipe away the Activity/task and confirm runtime continues.
9. Force-stop the application and confirm that this intentionally stops it.
10. Kill the process using ADB and confirm durable state is recovered after relaunch.
11. Reboot and confirm the chosen recovery policy.
12. Run the same tests under thermal load and with the screen off.
13. Run native alignment tests on a 16-KB test environment.

## Current limitations

- Model manager is still prototype-level: no resumable HTTP Range/ETag, checksum verification, GGUF metadata extraction, memory admission, transactional load/unload, or benchmark database yet.
- The Python backend is still packaged by the build workflow rather than independently proven as a fully offline Android distribution.
- Browser, document intelligence, verification, evidence provenance and the full Reliability Controller are not yet connected.
- `dataSync` is being used only as the current execution classification for this first slice. Long-lived AI execution policy must be validated against the target Android release rather than assumed to be immortal.
