# Build status

## CI target

- Android ABI: `arm64-v8a`
- Android compile/target SDK: 35
- Minimum SDK: 24
- JDK: 17
- Gradle: 8.11.1
- Android Gradle Plugin: 8.9.2
- Kotlin: 2.0.21
- Chaquopy: 17.0.0
- Python: 3.11
- Eigent: v1.0.2, commit `e478094...`
- llama.cpp: b10516, commit `b95502b`

## CI gates

The workflow checks the pinned upstream commits, native ELF identity, non-empty APK, APK package metadata, and presence of the embedded Eigent web entry point and arm64 native runtime.

A successful CI run means GitHub produced the APK. It does **not** yet prove that every Python dependency and every Eigent capability is functional on the Vivo V29; those require device tests.
