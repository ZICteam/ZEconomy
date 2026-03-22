# Contributing

## Environment

- Java `17`
- Minecraft Forge `1.20.1`
- Gradle wrapper from this repository

## Before Opening a Pull Request

1. Build the project locally.
2. Verify the mod still loads.
3. Keep changes focused.
4. Do not commit generated build output.

Build command:

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

## Style

- Keep package naming under `io.zicteam.zeconomy`
- Prefer small, explicit changes
- Do not add compatibility shims unless they are intentionally required
- Keep public API changes documented in `README.md`, `API.md`, and `examples/integration-example/README.md`
