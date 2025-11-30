# JPackage Distribution Design

## Overview

Add jpackage-based distribution that creates portable app-images for Linux, Windows, and macOS, with GitHub Actions CI/CD publishing to GitHub Releases.

## Decisions

| Decision | Choice |
|----------|--------|
| Package type | App-image (portable directory) |
| Build environment | GitHub Actions (Linux, Windows, macOS runners) |
| Release trigger | Tag-based (`v*`) for official, manual dispatch for pre-release |
| Version source | Git tag (starting at v2.0.0) |
| Icons | Optional (document how to add) |
| Bundled extras | None (document how to add) |
| Native installers | Not included (document how to add) |

## File Changes

### New Files

```
.github/workflows/release.yml     # CI/CD workflow
docs/building-distributions.md    # Build instructions
```

### Modified Files

```
build.gradle.kts                  # Add jpackage task
```

## Gradle Configuration

Add jpackage task to root `build.gradle.kts`:

- Depends on `:Program:installDist` to collect all JARs
- Detects current OS and configures appropriate jpackage options
- Accepts version override via `-Pversion=X.Y.Z`

**Key jpackage settings:**
```
--type app-image
--name JPhotoTagger
--main-jar Program.jar
--main-class org.jphototagger.program.Main
--java-options "-XX:+UseZGC -XX:+UseStringDeduplication -Xmx1g -Xms256m"
--input <JARs from installDist>
--dest build/jpackage
```

**Usage:**
```bash
# Local build (uses Gradle version)
./gradlew jpackage

# CI build (overrides version from tag)
./gradlew jpackage -Pversion=2.0.0
```

## GitHub Actions Workflow

**Triggers:**
- Tag push: `v*` pattern (e.g., `v2.0.0`) creates official release
- Manual dispatch: Creates pre-release for testing

**Jobs:**

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  build-linux    │  │  build-windows  │  │  build-macos    │
│  ubuntu-latest  │  │  windows-latest │  │  macos-latest   │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                              ▼
                    ┌─────────────────┐
                    │     release     │
                    │  Create GitHub  │
                    │    Release      │
                    └─────────────────┘
```

**Each build job:**
1. Checkout code
2. Set up Java 21 (Temurin)
3. Extract version from git tag (or `dev` for manual)
4. Run `./gradlew jpackage -Pversion=$VERSION`
5. Zip app-image directory
6. Upload as artifact

**Release job:**
1. Download all artifacts
2. Create GitHub Release (pre-release if manual dispatch)
3. Upload zip files as assets

**Asset names:**
- `JPhotoTagger-2.0.0-linux.zip`
- `JPhotoTagger-2.0.0-windows.zip`
- `JPhotoTagger-2.0.0-macos.zip`

## Documentation

`docs/building-distributions.md` will cover:

1. **Prerequisites** - Java 21+, Gradle wrapper
2. **Local Build Instructions** - Per-platform commands and output locations
3. **Running the App-Image** - Launch commands per platform
4. **CI/CD Releases** - Tag-based and manual workflows
5. **Adding Icons** - File formats, placement, Gradle config
6. **Bundling Extra Files** - How to include manual, scripts, etc.
7. **Adding Native Installers** - `.deb`, `.rpm`, `.msi`, `.dmg` setup

## Output Structure

**Linux/Windows app-image:**
```
build/jpackage/JPhotoTagger/
├── bin/
│   └── JPhotoTagger          # Launcher script/exe
├── lib/
│   ├── app/
│   │   ├── Program.jar
│   │   └── *.jar             # All dependency JARs
│   └── runtime/              # Bundled JRE
└── ...
```

**macOS app-image:**
```
build/jpackage/JPhotoTagger.app/
├── Contents/
│   ├── MacOS/
│   │   └── JPhotoTagger      # Launcher
│   ├── Resources/
│   └── runtime/              # Bundled JRE
└── ...
```

## Not Included

- Native installers (`.deb`, `.msi`, `.dmg`) - documented how to add
- Custom application icons - documented how to add
- Bundled extras (manual, scripts, dcraw) - documented how to add
