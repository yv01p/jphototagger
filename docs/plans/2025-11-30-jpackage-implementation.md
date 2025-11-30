# JPackage Distribution Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add jpackage-based distribution that creates portable app-images for Linux, Windows, and macOS, with GitHub Actions CI/CD publishing to GitHub Releases.

**Architecture:** Add a `jpackage` Gradle task to root build that collects JARs from `:Program:installDist` and invokes jpackage with OS-detected settings. A separate GitHub Actions workflow handles multi-platform builds and releases.

**Tech Stack:** Gradle Kotlin DSL, jpackage (JDK 21), GitHub Actions, GitHub Releases

---

## Task 1: Add jpackage Task to Root Build

**Files:**
- Modify: `build.gradle.kts:67-72` (after `buildAll` task)

**Step 1: Add OS detection and jpackage task**

Add the following after line 71 (after the `buildAll` task closing brace):

```kotlin
// OS detection for jpackage
val currentOs: String = System.getProperty("os.name").lowercase().let { osName ->
    when {
        osName.contains("linux") -> "linux"
        osName.contains("windows") -> "windows"
        osName.contains("mac") || osName.contains("darwin") -> "macos"
        else -> error("Unsupported OS: $osName")
    }
}

// jpackage task for creating native app-images
tasks.register<Exec>("jpackage") {
    description = "Creates a native app-image using jpackage"
    group = "distribution"
    dependsOn(":Program:installDist")

    val jpackageVersion = project.findProperty("version")?.toString() ?: project.version.toString()
    val inputDir = project.file("Program/build/install/Program/lib")
    val outputDir = project.file("build/jpackage")
    val resourceDir = project.file("packaging")

    val mainJar = "Program-${project.version}.jar"
    val javaOptions = listOf(
        "-XX:+UseZGC",
        "-XX:+UseStringDeduplication",
        "-Xmx1g",
        "-Xms256m"
    )

    doFirst {
        outputDir.mkdirs()
        // Clean previous build
        project.file("$outputDir/JPhotoTagger").deleteRecursively()
        if (currentOs == "macos") {
            project.file("$outputDir/JPhotoTagger.app").deleteRecursively()
        }
    }

    workingDir(project.projectDir)

    val jpackageArgs = mutableListOf(
        "jpackage",
        "--type", "app-image",
        "--name", "JPhotoTagger",
        "--app-version", jpackageVersion.removePrefix("v"),
        "--vendor", "JPhotoTagger",
        "--description", "Photo metadata management application",
        "--input", inputDir.absolutePath,
        "--main-jar", mainJar,
        "--main-class", "org.jphototagger.program.Main",
        "--dest", outputDir.absolutePath
    )

    // Add Java options
    javaOptions.forEach { opt ->
        jpackageArgs.addAll(listOf("--java-options", opt))
    }

    // Add platform-specific icon if it exists
    val iconFile = when (currentOs) {
        "linux" -> resourceDir.resolve("JPhotoTagger.png")
        "windows" -> resourceDir.resolve("JPhotoTagger.ico")
        "macos" -> resourceDir.resolve("JPhotoTagger.icns")
        else -> null
    }
    if (iconFile?.exists() == true) {
        jpackageArgs.addAll(listOf("--icon", iconFile.absolutePath))
    }

    commandLine(jpackageArgs)

    doLast {
        val appDir = if (currentOs == "macos") {
            outputDir.resolve("JPhotoTagger.app")
        } else {
            outputDir.resolve("JPhotoTagger")
        }
        println("App-image created at: ${appDir.absolutePath}")
    }
}
```

**Step 2: Run the task to verify it works**

Run:
```bash
./gradlew jpackage
```

Expected: BUILD SUCCESSFUL, output at `build/jpackage/JPhotoTagger/`

**Step 3: Verify the app-image launches**

Run:
```bash
./build/jpackage/JPhotoTagger/bin/JPhotoTagger &
sleep 3
pkill -f JPhotoTagger || true
```

Expected: Application window appears briefly

**Step 4: Commit**

```bash
git add build.gradle.kts
git commit -m "feat(build): add jpackage task for native app-image creation

Adds Gradle task to create portable app-images using jpackage.
Supports Linux, Windows, and macOS with OS auto-detection.
Version can be overridden via -Pversion parameter."
```

---

## Task 2: Create GitHub Actions Release Workflow

**Files:**
- Create: `.github/workflows/release.yml`

**Step 1: Create the release workflow file**

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'
  workflow_dispatch:
    inputs:
      prerelease:
        description: 'Mark as pre-release'
        required: false
        default: true
        type: boolean

permissions:
  contents: write

jobs:
  build-linux:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Get version
        id: version
        run: |
          if [[ "${{ github.ref_type }}" == "tag" ]]; then
            VERSION="${{ github.ref_name }}"
          else
            VERSION="dev-$(date +%Y%m%d-%H%M%S)"
          fi
          echo "version=$VERSION" >> $GITHUB_OUTPUT
          echo "Building version: $VERSION"

      - name: Build app-image
        run: ./gradlew jpackage -Pversion=${{ steps.version.outputs.version }}

      - name: Create zip archive
        run: |
          cd build/jpackage
          zip -r JPhotoTagger-${{ steps.version.outputs.version }}-linux.zip JPhotoTagger

      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: linux-build
          path: build/jpackage/JPhotoTagger-${{ steps.version.outputs.version }}-linux.zip

  build-windows:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Get version
        id: version
        shell: bash
        run: |
          if [[ "${{ github.ref_type }}" == "tag" ]]; then
            VERSION="${{ github.ref_name }}"
          else
            VERSION="dev-$(date +%Y%m%d-%H%M%S)"
          fi
          echo "version=$VERSION" >> $GITHUB_OUTPUT
          echo "Building version: $VERSION"

      - name: Build app-image
        run: ./gradlew jpackage -Pversion=${{ steps.version.outputs.version }}

      - name: Create zip archive
        shell: bash
        run: |
          cd build/jpackage
          7z a JPhotoTagger-${{ steps.version.outputs.version }}-windows.zip JPhotoTagger

      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: windows-build
          path: build/jpackage/JPhotoTagger-${{ steps.version.outputs.version }}-windows.zip

  build-macos:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Get version
        id: version
        run: |
          if [[ "${{ github.ref_type }}" == "tag" ]]; then
            VERSION="${{ github.ref_name }}"
          else
            VERSION="dev-$(date +%Y%m%d-%H%M%S)"
          fi
          echo "version=$VERSION" >> $GITHUB_OUTPUT
          echo "Building version: $VERSION"

      - name: Build app-image
        run: ./gradlew jpackage -Pversion=${{ steps.version.outputs.version }}

      - name: Create zip archive
        run: |
          cd build/jpackage
          zip -r JPhotoTagger-${{ steps.version.outputs.version }}-macos.zip JPhotoTagger.app

      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: macos-build
          path: build/jpackage/JPhotoTagger-${{ steps.version.outputs.version }}-macos.zip

  release:
    needs: [build-linux, build-windows, build-macos]
    runs-on: ubuntu-latest
    steps:
      - name: Get version
        id: version
        run: |
          if [[ "${{ github.ref_type }}" == "tag" ]]; then
            VERSION="${{ github.ref_name }}"
            PRERELEASE="false"
          else
            VERSION="dev-$(date +%Y%m%d-%H%M%S)"
            PRERELEASE="true"
          fi
          echo "version=$VERSION" >> $GITHUB_OUTPUT
          echo "prerelease=$PRERELEASE" >> $GITHUB_OUTPUT

      - name: Download Linux artifact
        uses: actions/download-artifact@v4
        with:
          name: linux-build
          path: artifacts

      - name: Download Windows artifact
        uses: actions/download-artifact@v4
        with:
          name: windows-build
          path: artifacts

      - name: Download macOS artifact
        uses: actions/download-artifact@v4
        with:
          name: macos-build
          path: artifacts

      - name: List artifacts
        run: ls -la artifacts/

      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          tag_name: ${{ steps.version.outputs.version }}
          name: JPhotoTagger ${{ steps.version.outputs.version }}
          prerelease: ${{ steps.version.outputs.prerelease == 'true' || github.event.inputs.prerelease == 'true' }}
          generate_release_notes: true
          files: |
            artifacts/*.zip
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

**Step 2: Verify YAML syntax**

Run:
```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml'))" && echo "YAML is valid"
```

Expected: `YAML is valid`

**Step 3: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: add release workflow for multi-platform jpackage builds

Creates app-images for Linux, Windows, and macOS using GitHub Actions.
Triggers on version tags (v*) for official releases or manual dispatch
for pre-releases. Publishes to GitHub Releases."
```

---

## Task 3: Update Existing Build Workflow to Java 21

**Files:**
- Modify: `.github/workflows/build.yml`

**Step 1: Update JDK version from 8 to 21**

Replace lines 16-20 and 46-50 (both `Set up JDK 8` steps):

Change:
```yaml
      - name: Set up JDK 8
        uses: actions/setup-java@v4
        with:
          java-version: '8'
          distribution: 'temurin'
```

To:
```yaml
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
```

**Step 2: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: update build workflow to Java 21

Project now requires Java 21 for virtual threads and jpackage."
```

---

## Task 4: Create Distribution Documentation

**Files:**
- Create: `docs/building-distributions.md`

**Step 1: Write the documentation**

```markdown
# Building Distribution Packages

This guide explains how to build JPhotoTagger distribution packages for Linux, Windows, and macOS.

## Prerequisites

- **Java 21+** (includes jpackage tool)
- **Gradle** (wrapper included in repository)

The Gradle wrapper will automatically download the correct Gradle version.

## Local Build Instructions

You can only build packages for your current operating system. Cross-compilation is not supported by jpackage.

### Linux

```bash
./gradlew jpackage
```

Output: `build/jpackage/JPhotoTagger/`

### Windows

```cmd
gradlew.bat jpackage
```

Output: `build\jpackage\JPhotoTagger\`

### macOS

```bash
./gradlew jpackage
```

Output: `build/jpackage/JPhotoTagger.app/`

### Version Override

To build with a specific version number:

```bash
./gradlew jpackage -Pversion=2.0.0
```

## Running the App-Image

### Linux

```bash
./build/jpackage/JPhotoTagger/bin/JPhotoTagger
```

### Windows

```cmd
build\jpackage\JPhotoTagger\JPhotoTagger.exe
```

Or double-click `JPhotoTagger.exe` in File Explorer.

### macOS

```bash
open build/jpackage/JPhotoTagger.app
```

Or double-click `JPhotoTagger.app` in Finder.

## CI/CD Releases

Releases are built automatically using GitHub Actions.

### Creating an Official Release

1. Tag the commit with a version:
   ```bash
   git tag v2.0.0
   git push origin v2.0.0
   ```

2. GitHub Actions will:
   - Build app-images for Linux, Windows, and macOS
   - Create a GitHub Release with all three packages

### Creating a Test Build

1. Go to **Actions** → **Release** workflow
2. Click **Run workflow**
3. Check "Mark as pre-release" (default)
4. Click **Run workflow**

Test builds are marked as pre-release and have `dev-YYYYMMDD-HHMMSS` version format.

## Customization

### Adding Application Icons

Create a `packaging/` directory in the project root with platform-specific icons:

| Platform | File | Format | Recommended Size |
|----------|------|--------|------------------|
| Linux | `JPhotoTagger.png` | PNG | 256x256 or larger |
| Windows | `JPhotoTagger.ico` | ICO | Multi-resolution (16-256px) |
| macOS | `JPhotoTagger.icns` | ICNS | Multi-resolution |

The build will automatically detect and use these icons.

**Creating icons:**

- **PNG to ICO (Windows):** Use [ImageMagick](https://imagemagick.org/):
  ```bash
  convert icon-256.png -define icon:auto-resize=256,128,64,48,32,16 JPhotoTagger.ico
  ```

- **PNG to ICNS (macOS):** Use `iconutil` on macOS:
  ```bash
  mkdir JPhotoTagger.iconset
  sips -z 16 16 icon.png --out JPhotoTagger.iconset/icon_16x16.png
  sips -z 32 32 icon.png --out JPhotoTagger.iconset/icon_16x16@2x.png
  sips -z 32 32 icon.png --out JPhotoTagger.iconset/icon_32x32.png
  sips -z 64 64 icon.png --out JPhotoTagger.iconset/icon_32x32@2x.png
  sips -z 128 128 icon.png --out JPhotoTagger.iconset/icon_128x128.png
  sips -z 256 256 icon.png --out JPhotoTagger.iconset/icon_128x128@2x.png
  sips -z 256 256 icon.png --out JPhotoTagger.iconset/icon_256x256.png
  sips -z 512 512 icon.png --out JPhotoTagger.iconset/icon_256x256@2x.png
  sips -z 512 512 icon.png --out JPhotoTagger.iconset/icon_512x512.png
  sips -z 1024 1024 icon.png --out JPhotoTagger.iconset/icon_512x512@2x.png
  iconutil -c icns JPhotoTagger.iconset
  ```

### Bundling Extra Files

To include additional files (manual, scripts, etc.) in the app-image:

1. Create a resources directory for jpackage:
   ```bash
   mkdir -p packaging/resources
   ```

2. Add files to bundle:
   ```bash
   cp dist_files/manual/Manual_de.pdf packaging/resources/
   cp -r dist_files/scripts packaging/resources/
   ```

3. Modify `build.gradle.kts` jpackage task to add:
   ```kotlin
   "--resource-dir", resourceDir.resolve("resources").absolutePath
   ```

### Adding Native Installers

By default, the build creates portable app-images. To create native installers instead:

#### Prerequisites by Platform

| Platform | Installer Type | Required Tools |
|----------|---------------|----------------|
| Linux | `.deb` | `dpkg-deb` (installed by default on Debian/Ubuntu) |
| Linux | `.rpm` | `rpm-build` (`sudo apt install rpm` or `sudo dnf install rpm-build`) |
| Windows | `.msi` | [WiX Toolset 3.0+](https://wixtoolset.org/releases/) |
| Windows | `.exe` | [WiX Toolset](https://wixtoolset.org/) or [Inno Setup](https://jrsoftware.org/isinfo.php) |
| macOS | `.dmg` | Xcode Command Line Tools (`xcode-select --install`) |
| macOS | `.pkg` | Xcode Command Line Tools |

#### Modifying the Build

Add an `installerType` property to the jpackage task in `build.gradle.kts`:

```kotlin
val installerType = project.findProperty("installerType")?.toString() ?: "app-image"

// In jpackageArgs:
"--type", installerType,
```

Then build with:

```bash
# Linux .deb
./gradlew jpackage -PinstallerType=deb

# Linux .rpm
./gradlew jpackage -PinstallerType=rpm

# Windows .msi
gradlew.bat jpackage -PinstallerType=msi

# macOS .dmg
./gradlew jpackage -PinstallerType=dmg
```

#### CI Considerations

To build native installers in CI, you need to install the required tools:

**Linux (.deb is available by default, for .rpm):**
```yaml
- name: Install RPM tools
  run: sudo apt-get install -y rpm
```

**Windows (.msi requires WiX):**
```yaml
- name: Install WiX Toolset
  run: choco install wixtoolset -y
```

**macOS (.dmg/.pkg work by default with Xcode CLI tools)**

### Changing JVM Options

Edit the `javaOptions` list in `build.gradle.kts`:

```kotlin
val javaOptions = listOf(
    "-XX:+UseZGC",
    "-XX:+UseStringDeduplication",
    "-Xmx2g",        // Increase max heap
    "-Xms512m"       // Increase initial heap
)
```

## Troubleshooting

### "jpackage: command not found"

Ensure you have JDK 21+ installed (not just JRE). The jpackage tool is included in the JDK.

```bash
java -version   # Should show 21+
which jpackage  # Should show path to jpackage
```

### Windows Defender blocks the app

Windows may block unsigned applications. Users need to click "More info" → "Run anyway" on first launch.

For production releases, consider code signing with a certificate.

### macOS Gatekeeper blocks the app

Unsigned apps are blocked by default. Users can:

1. Right-click the app → Open → Open (first launch only)
2. Or: System Preferences → Security & Privacy → "Open Anyway"

For production releases, consider Apple Developer notarization.

### Build fails with "installDist" error

Run the full build first:

```bash
./gradlew build
./gradlew jpackage
```
```

**Step 2: Commit**

```bash
git add docs/building-distributions.md
git commit -m "docs: add distribution building guide

Covers local builds, CI/CD releases, and customization options
including icons, bundled files, and native installers."
```

---

## Task 5: Add packaging Directory to .gitignore

**Files:**
- Modify: `.gitignore`

**Step 1: Add packaging directory exclusion**

Add to `.gitignore`:

```
# jpackage output
build/jpackage/
```

**Step 2: Commit**

```bash
git add .gitignore
git commit -m "chore: ignore jpackage output directory"
```

---

## Task 6: Test Full Workflow Locally

**Step 1: Clean and build**

Run:
```bash
./gradlew clean jpackage
```

Expected: BUILD SUCCESSFUL

**Step 2: Verify output structure**

Run:
```bash
ls -la build/jpackage/JPhotoTagger/
ls -la build/jpackage/JPhotoTagger/bin/
ls build/jpackage/JPhotoTagger/lib/app/ | head -10
```

Expected:
- `bin/` contains `JPhotoTagger` launcher
- `lib/app/` contains all JAR files
- `lib/runtime/` contains bundled JRE

**Step 3: Test version override**

Run:
```bash
./gradlew jpackage -Pversion=2.0.0-test
ls build/jpackage/
```

Expected: JPhotoTagger directory exists, version used in JAR names

**Step 4: Launch application**

Run:
```bash
./build/jpackage/JPhotoTagger/bin/JPhotoTagger &
sleep 5
pkill -f JPhotoTagger || true
```

Expected: Application launches successfully

---

## Task 7: Push Changes and Verify CI

**Step 1: Push to remote**

Run:
```bash
git push origin master
```

**Step 2: Verify build workflow passes**

Go to: `https://github.com/yv01p/jphototagger/actions`

Expected: Build workflow runs with Java 21 and passes

**Step 3: Test release workflow with manual dispatch**

1. Go to Actions → Release workflow
2. Click "Run workflow"
3. Select branch: master
4. Check "Mark as pre-release"
5. Click "Run workflow"

Expected: All three platform builds succeed, pre-release created

---

## Summary

| Task | Files | Action |
|------|-------|--------|
| 1 | `build.gradle.kts` | Add jpackage task |
| 2 | `.github/workflows/release.yml` | Create release workflow |
| 3 | `.github/workflows/build.yml` | Update to Java 21 |
| 4 | `docs/building-distributions.md` | Create documentation |
| 5 | `.gitignore` | Ignore jpackage output |
| 6 | - | Test locally |
| 7 | - | Push and verify CI |
