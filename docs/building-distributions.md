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
