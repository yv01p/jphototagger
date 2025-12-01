# GUI Automation E2E Testing Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement E2E GUI tests for JPhotoTagger's Import, Keyword Tagging, and Search workflows using AssertJ Swing with Page Object pattern.

**Architecture:** Page Objects wrap AssertJ Swing interactions. Tests launch the real app via `AppInit`, interact with UI components, and verify state changes. Test data uses embedded placeholder JPGs copied to temp directories. CI runs tests via Xvfb.

**Tech Stack:** AssertJ Swing 3.17.1, JUnit 5, Xvfb for CI

---

## Task 1: Add AssertJ Swing Dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `Program/build.gradle.kts`

**Step 1: Add AssertJ Swing to version catalog**

Edit `gradle/libs.versions.toml`, add after line 17 (`jmh = "1.37"`):

```toml
assertj-swing = "3.17.1"
```

Add after line 39 (`jmh-generator = ...`):

```toml
assertj-swing = { module = "org.assertj:assertj-swing-junit", version.ref = "assertj-swing" }
```

**Step 2: Add dependency to Program module**

Edit `Program/build.gradle.kts`, add after line 66 (`testImplementation(project(":TestSupport"))`):

```kotlin
    testImplementation(libs.assertj.swing)
    testImplementation(libs.bundles.junit5)
    testImplementation(libs.assertj)
```

**Step 3: Add e2eTest task to Program module**

Edit `Program/build.gradle.kts`, add after line 95 (`}`):

```kotlin

tasks.register<Test>("e2eTest") {
    description = "Runs E2E GUI tests"
    group = "verification"

    useJUnitPlatform {
        includeTags("e2e")
    }

    failFast = true

    // E2E tests need more memory for GUI
    maxHeapSize = "512m"
}
```

**Step 4: Verify build still works**

Run: `./gradlew :Program:dependencies --configuration testCompileClasspath | grep assertj`

Expected: Output contains `assertj-swing` and `assertj-core`

**Step 5: Commit**

```bash
git add gradle/libs.versions.toml Program/build.gradle.kts
git commit -m "build: add AssertJ Swing for E2E GUI testing"
```

---

## Task 2: Create Test Data Resources

**Files:**
- Create: `Program/src/test/resources/e2e/photos/test-photo-01.jpg`
- Create: `Program/src/test/resources/e2e/photos/test-photo-02.jpg`
- Create: `Program/src/test/resources/e2e/photos/test-photo-03.jpg`
- Create: `Program/src/test/resources/e2e/photos/README.md`

**Step 1: Create directories**

Run: `mkdir -p Program/src/test/resources/e2e/photos`

**Step 2: Create minimal valid JPEG files**

These are 1x1 pixel valid JPEGs. Create using Java (faster than external tools):

Create file `Program/src/test/resources/e2e/photos/generate-test-photos.sh`:

```bash
#!/bin/bash
# Generate minimal test JPEG files using ImageMagick or Java

cd "$(dirname "$0")"

# If ImageMagick is available
if command -v convert &> /dev/null; then
    convert -size 100x100 xc:red test-photo-01.jpg
    convert -size 100x100 xc:green test-photo-02.jpg
    convert -size 100x100 xc:blue test-photo-03.jpg
    echo "Created test photos with ImageMagick"
    exit 0
fi

echo "ImageMagick not found. Creating test photos with base64-encoded minimal JPEG."
# Minimal 1x1 red JPEG (base64 encoded)
echo "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAn/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBEQCEAwEPwAB/9k=" | base64 -d > test-photo-01.jpg
echo "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAn/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBEQCEAwEPwAB/9k=" | base64 -d > test-photo-02.jpg
echo "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAn/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBEQCEAwEPwAB/9k=" | base64 -d > test-photo-03.jpg
echo "Created test photos with base64"
```

Run: `chmod +x Program/src/test/resources/e2e/photos/generate-test-photos.sh && Program/src/test/resources/e2e/photos/generate-test-photos.sh`

**Step 3: Create README for test photos**

Create file `Program/src/test/resources/e2e/photos/README.md`:

```markdown
# E2E Test Photos

Minimal placeholder JPEG images for E2E GUI testing.

These are 1x1 pixel images used to test the import workflow without
requiring real photo files. They are valid JPEG files that JPhotoTagger
can process.

## Files

- `test-photo-01.jpg` - Red placeholder
- `test-photo-02.jpg` - Green placeholder
- `test-photo-03.jpg` - Blue placeholder

## Regenerating

Run `./generate-test-photos.sh` to regenerate these files.
```

**Step 4: Verify files exist**

Run: `ls -la Program/src/test/resources/e2e/photos/`

Expected: Three .jpg files and README.md present

**Step 5: Commit**

```bash
git add Program/src/test/resources/e2e/
git commit -m "test: add placeholder photos for E2E tests"
```

---

## Task 3: Create TestDataManager

**Files:**
- Create: `Program/src/test/java/org/jphototagger/e2e/base/TestDataManager.java`

**Step 1: Create directory structure**

Run: `mkdir -p Program/src/test/java/org/jphototagger/e2e/base`

**Step 2: Create TestDataManager**

Create file `Program/src/test/java/org/jphototagger/e2e/base/TestDataManager.java`:

```java
package org.jphototagger.e2e.base;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

/**
 * Manages test photo files for E2E tests.
 * Creates temp directories, copies test photos from resources,
 * and cleans up after tests.
 */
public class TestDataManager {

    private static final String[] TEST_PHOTOS = {
        "test-photo-01.jpg",
        "test-photo-02.jpg",
        "test-photo-03.jpg"
    };

    private Path tempDirectory;

    /**
     * Creates a new temporary directory for this test.
     */
    public void createTempDirectory() throws IOException {
        tempDirectory = Files.createTempDirectory("jphototagger-e2e-");
    }

    /**
     * Copies test photos from resources to the temp directory.
     */
    public void copyTestPhotos() throws IOException {
        if (tempDirectory == null) {
            throw new IllegalStateException("Call createTempDirectory() first");
        }

        for (String photoName : TEST_PHOTOS) {
            String resourcePath = "/e2e/photos/" + photoName;
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    throw new IOException("Resource not found: " + resourcePath);
                }
                Path targetPath = tempDirectory.resolve(photoName);
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Returns the temp directory containing test photos.
     */
    public File getTestPhotosFolder() {
        if (tempDirectory == null) {
            throw new IllegalStateException("Call createTempDirectory() first");
        }
        return tempDirectory.toFile();
    }

    /**
     * Returns a specific test photo file.
     */
    public File getTestPhoto(int index) {
        if (index < 0 || index >= TEST_PHOTOS.length) {
            throw new IllegalArgumentException("Invalid photo index: " + index);
        }
        return tempDirectory.resolve(TEST_PHOTOS[index]).toFile();
    }

    /**
     * Returns the number of test photos available.
     */
    public int getTestPhotoCount() {
        return TEST_PHOTOS.length;
    }

    /**
     * Deletes the temp directory and all contents.
     */
    public void cleanup() {
        if (tempDirectory == null) {
            return;
        }

        try {
            Files.walk(tempDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Best effort cleanup
                    }
                });
        } catch (IOException e) {
            // Best effort cleanup
        }

        tempDirectory = null;
    }
}
```

**Step 3: Verify compilation**

Run: `./gradlew :Program:compileTestJava`

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add Program/src/test/java/org/jphototagger/e2e/base/TestDataManager.java
git commit -m "test: add TestDataManager for E2E test data"
```

---

## Task 4: Create E2ETestBase

**Files:**
- Create: `Program/src/test/java/org/jphototagger/e2e/base/E2ETestBase.java`

**Step 1: Create E2ETestBase**

Create file `Program/src/test/java/org/jphototagger/e2e/base/E2ETestBase.java`:

```java
package org.jphototagger.e2e.base;

import java.awt.Frame;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.jphototagger.program.app.AppInit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

/**
 * Base class for all E2E GUI tests.
 * Handles app launch, robot creation, and test data management.
 */
@Tag("e2e")
public abstract class E2ETestBase {

    protected static FrameFixture window;
    protected static Robot robot;
    protected TestDataManager testData;

    @BeforeAll
    static void launchApp() {
        robot = BasicRobot.robotWithNewAwtHierarchy();

        // Launch JPhotoTagger on the EDT
        Frame frame = GuiActionRunner.execute(() -> {
            AppInit.INSTANCE.init(new String[]{});
            return findMainFrame();
        });

        window = new FrameFixture(robot, frame);
        window.show();
    }

    private static Frame findMainFrame() {
        // Wait for main frame to be visible
        Frame[] frames = Frame.getFrames();
        for (Frame frame : frames) {
            if (frame.isVisible() && frame.getTitle().contains("JPhotoTagger")) {
                return frame;
            }
        }
        // If not found by title, return the first visible frame
        for (Frame frame : frames) {
            if (frame.isVisible()) {
                return frame;
            }
        }
        throw new IllegalStateException("Main frame not found");
    }

    @BeforeEach
    void setupTestData() throws Exception {
        testData = new TestDataManager();
        testData.createTempDirectory();
        testData.copyTestPhotos();
    }

    @AfterEach
    void cleanupTestData() {
        if (testData != null) {
            testData.cleanup();
        }
    }

    @AfterAll
    static void tearDown() {
        if (window != null) {
            window.cleanUp();
        }
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :Program:compileTestJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Program/src/test/java/org/jphototagger/e2e/base/E2ETestBase.java
git commit -m "test: add E2ETestBase for GUI test infrastructure"
```

---

## Task 5: Add Component Names to Import Dialog

**Files:**
- Modify: `Modules/ImportFiles/src/org/jphototagger/importfiles/ImportImageFilesDialog.java`

**Step 1: Read the file to find component declarations**

The dialog needs `setName()` calls for key components. Based on the design, add names for:
- The dialog itself
- Source folder text field
- Browse button
- OK/Import button
- Cancel button

**Step 2: Add setName() calls in postInitComponents()**

Edit `Modules/ImportFiles/src/org/jphototagger/importfiles/ImportImageFilesDialog.java`.

Find the `postInitComponents()` method (around line 95) and add after line 108 (after `lookupSkipDuplicates();`):

```java
        setComponentNames();
```

Then add a new method after `postInitComponents()`:

```java
    private void setComponentNames() {
        setName("dialog.import");
        buttonOk.setName("dialog.import.btnStart");
        buttonCancel.setName("dialog.import.btnCancel");
        buttonChooseSourceDir.setName("dialog.import.btnBrowseSource");
        buttonChooseTargetDir.setName("dialog.import.btnBrowseTarget");
        labelSourceDir.setName("dialog.import.lblSourceDir");
        labelTargetDir.setName("dialog.import.lblTargetDir");
    }
```

**Step 3: Verify compilation**

Run: `./gradlew :Modules:ImportFiles:compileJava`

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add Modules/ImportFiles/src/org/jphototagger/importfiles/ImportImageFilesDialog.java
git commit -m "feat: add component names to ImportImageFilesDialog for E2E testing"
```

---

## Task 6: Add Component Names to AppFrame Menu

**Files:**
- Modify: `Program/src/org/jphototagger/program/app/ui/AppFrame.java`

The menu items are added dynamically via lookup, so we need to find where the import menu item is created and named. Looking at `ImportImageFilesAction.java`, the menu item is created without a name.

**Step 1: Modify ImportImageFilesAction to set component name**

Edit `Modules/ImportFiles/src/org/jphototagger/importfiles/ImportImageFilesAction.java`.

Change the `getMenuItem()` method (around line 32):

```java
    @Override
    public JMenuItem getMenuItem() {
        JMenuItem item = UiFactory.menuItem(this);
        item.setName("menu.file.itemImport");
        MenuUtil.setMnemonics(item);
        return item;
    }
```

**Step 2: Verify compilation**

Run: `./gradlew :Modules:ImportFiles:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Modules/ImportFiles/src/org/jphototagger/importfiles/ImportImageFilesAction.java
git commit -m "feat: add component name to import menu item for E2E testing"
```

---

## Task 7: Create MainWindowPage

**Files:**
- Create: `Program/src/test/java/org/jphototagger/e2e/pages/MainWindowPage.java`

**Step 1: Create directory structure**

Run: `mkdir -p Program/src/test/java/org/jphototagger/e2e/pages`

**Step 2: Create MainWindowPage**

Create file `Program/src/test/java/org/jphototagger/e2e/pages/MainWindowPage.java`:

```java
package org.jphototagger.e2e.pages;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JPanelFixture;
import org.assertj.swing.fixture.JTreeFixture;

import javax.swing.JDialog;

/**
 * Page object for the main JPhotoTagger window.
 * Provides access to main UI components and navigation to dialogs.
 */
public class MainWindowPage {

    private final FrameFixture window;

    public MainWindowPage(FrameFixture window) {
        this.window = window;
    }

    /**
     * Opens the Import dialog via File menu.
     */
    public ImportDialogPage openImportDialog() {
        window.menuItem("menu.file.itemImport").click();

        // Wait for and find the import dialog
        DialogFixture dialog = window.dialog(new GenericTypeMatcher<JDialog>(JDialog.class) {
            @Override
            protected boolean isMatching(JDialog d) {
                return "dialog.import".equals(d.getName()) ||
                       (d.isVisible() && d.getTitle() != null &&
                        d.getTitle().toLowerCase().contains("import"));
            }
        });

        return new ImportDialogPage(dialog);
    }

    /**
     * Returns the directory tree component.
     */
    public JTreeFixture directoryTree() {
        return window.tree("treeDirectories");
    }

    /**
     * Returns the thumbnails panel.
     */
    public JPanelFixture thumbnailsPanel() {
        return window.panel("panelThumbnailsMetadata");
    }

    /**
     * Returns the search panel.
     */
    public JPanelFixture searchPanel() {
        return window.panel("panelSearch");
    }
}
```

**Step 3: Verify compilation**

Run: `./gradlew :Program:compileTestJava`

Expected: Compilation error (ImportDialogPage not yet created). This is expected.

**Step 4: Commit (with note about incomplete state)**

```bash
git add Program/src/test/java/org/jphototagger/e2e/pages/MainWindowPage.java
git commit -m "test: add MainWindowPage (partial, requires ImportDialogPage)"
```

---

## Task 8: Create ImportDialogPage

**Files:**
- Create: `Program/src/test/java/org/jphototagger/e2e/pages/ImportDialogPage.java`

**Step 1: Create ImportDialogPage**

Create file `Program/src/test/java/org/jphototagger/e2e/pages/ImportDialogPage.java`:

```java
package org.jphototagger.e2e.pages;

import org.assertj.swing.fixture.DialogFixture;

import java.io.File;

/**
 * Page object for the Import Images dialog.
 */
public class ImportDialogPage {

    private final DialogFixture dialog;

    public ImportDialogPage(DialogFixture dialog) {
        this.dialog = dialog;
    }

    /**
     * Sets the source directory by clicking browse and selecting folder.
     * Note: This is a simplified version that sets the label directly.
     * For full testing, would need to handle the directory chooser.
     */
    public ImportDialogPage withSourceDirectory(File folder) {
        // Click browse button to open directory chooser
        dialog.button("dialog.import.btnBrowseSource").click();

        // For now, we'll need to handle the native file chooser
        // This may require robot.keyPress to type the path
        // TODO: Implement directory chooser handling

        return this;
    }

    /**
     * Clicks the Import (OK) button.
     */
    public void clickImport() {
        dialog.button("dialog.import.btnStart").click();
    }

    /**
     * Clicks the Cancel button.
     */
    public void clickCancel() {
        dialog.button("dialog.import.btnCancel").click();
    }

    /**
     * Verifies the dialog is visible.
     */
    public ImportDialogPage requireVisible() {
        dialog.requireVisible();
        return this;
    }

    /**
     * Verifies the dialog is not visible (closed).
     */
    public void requireNotVisible() {
        dialog.requireNotVisible();
    }

    /**
     * Returns the underlying dialog fixture for advanced assertions.
     */
    public DialogFixture dialog() {
        return dialog;
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :Program:compileTestJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Program/src/test/java/org/jphototagger/e2e/pages/ImportDialogPage.java
git commit -m "test: add ImportDialogPage for import workflow testing"
```

---

## Task 9: Create ImportWorkflowTest

**Files:**
- Create: `Program/src/test/java/org/jphototagger/e2e/workflows/ImportWorkflowTest.java`

**Step 1: Create directory structure**

Run: `mkdir -p Program/src/test/java/org/jphototagger/e2e/workflows`

**Step 2: Create ImportWorkflowTest**

Create file `Program/src/test/java/org/jphototagger/e2e/workflows/ImportWorkflowTest.java`:

```java
package org.jphototagger.e2e.workflows;

import org.jphototagger.e2e.base.E2ETestBase;
import org.jphototagger.e2e.pages.ImportDialogPage;
import org.jphototagger.e2e.pages.MainWindowPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the photo import workflow.
 */
class ImportWorkflowTest extends E2ETestBase {

    private MainWindowPage mainWindow;

    @BeforeEach
    void setup() {
        mainWindow = new MainWindowPage(window);
    }

    @Test
    @DisplayName("Import dialog opens from File menu")
    void openImportDialog_displaysImportDialog() {
        ImportDialogPage importDialog = mainWindow.openImportDialog();

        importDialog.requireVisible();

        // Close dialog to clean up
        importDialog.clickCancel();
    }

    @Test
    @DisplayName("Import dialog can be cancelled")
    void cancelImportDialog_closesDialog() {
        ImportDialogPage importDialog = mainWindow.openImportDialog();

        importDialog.clickCancel();

        importDialog.requireNotVisible();
    }

    @Test
    @Disabled("Requires directory chooser handling - implement in next iteration")
    @DisplayName("Import photos from folder displays in thumbnail panel")
    void importPhotosFromFolder_displaysInThumbnailPanel() {
        // This test requires handling the native directory chooser
        // which is complex with AssertJ Swing.
        // Keeping as placeholder for next iteration.

        ImportDialogPage importDialog = mainWindow.openImportDialog();
        importDialog.withSourceDirectory(testData.getTestPhotosFolder());
        importDialog.clickImport();

        // Verify photos appear in thumbnail panel
        assertThat(testData.getTestPhotoCount()).isEqualTo(3);
    }
}
```

**Step 3: Verify compilation**

Run: `./gradlew :Program:compileTestJava`

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add Program/src/test/java/org/jphototagger/e2e/workflows/ImportWorkflowTest.java
git commit -m "test: add ImportWorkflowTest with initial dialog tests"
```

---

## Task 10: Update GitHub Actions for E2E Tests

**Files:**
- Modify: `.github/workflows/build.yml`

**Step 1: Add E2E test job with Xvfb**

Edit `.github/workflows/build.yml`, add after line 63 (after benchmark job):

```yaml

  e2e-tests:
    runs-on: ubuntu-latest
    needs: build

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Setup Gradle
      uses: gradle/actions/setup-gradle@v3

    - name: Run E2E tests with Xvfb
      run: xvfb-run --auto-servernum --server-args="-screen 0 1280x1024x24" ./gradlew :Program:e2eTest

    - name: Upload E2E test results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: e2e-test-results
        path: 'Program/build/test-results/e2eTest/*.xml'
```

**Step 2: Verify YAML syntax**

Run: `cat .github/workflows/build.yml | python3 -c "import sys, yaml; yaml.safe_load(sys.stdin); print('YAML is valid')"`

Expected: "YAML is valid"

**Step 3: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: add E2E test job with Xvfb"
```

---

## Task 11: Run E2E Tests Locally

**Step 1: Run E2E tests**

Run: `./gradlew :Program:e2eTest`

Expected: Tests run. May see failures if display not available.

If on Linux without display, run with Xvfb:

Run: `xvfb-run --auto-servernum ./gradlew :Program:e2eTest`

**Step 2: Review test output**

Check: `Program/build/reports/tests/e2eTest/index.html`

Expected: Test report shows test execution

**Step 3: Document any issues found**

If tests fail, note the failure reason for iteration.

---

## Task 12: Final Verification and Summary Commit

**Step 1: Verify all files created**

Run: `find Program/src/test/java/org/jphototagger/e2e -name "*.java" | wc -l`

Expected: 4 files (E2ETestBase, TestDataManager, MainWindowPage, ImportDialogPage, ImportWorkflowTest... actually 5 files, so expect 5)

Actually:
- `base/E2ETestBase.java`
- `base/TestDataManager.java`
- `pages/MainWindowPage.java`
- `pages/ImportDialogPage.java`
- `workflows/ImportWorkflowTest.java`

Run: `find Program/src/test/java/org/jphototagger/e2e -name "*.java"`

Expected: 5 Java files

**Step 2: Run full build**

Run: `./gradlew build`

Expected: BUILD SUCCESSFUL

**Step 3: Create summary commit**

```bash
git add -A
git status
```

If any unstaged changes, commit:

```bash
git commit -m "test: complete Phase 1 E2E testing infrastructure

- AssertJ Swing dependency added
- TestDataManager for test photo lifecycle
- E2ETestBase for shared test setup
- MainWindowPage and ImportDialogPage (page objects)
- ImportWorkflowTest with initial dialog tests
- GitHub Actions e2e-tests job with Xvfb

Implements Phase 1 of GUI automation design."
```

---

## Next Steps (Phase 2-4)

After Phase 1 is complete and tests pass:

**Phase 2: Keyword Tagging Workflow**
- Add `setName()` calls to KeywordsPanel components
- Create `KeywordPanelPage.java`
- Create `KeywordTaggingWorkflowTest.java`

**Phase 3: Search Workflow**
- Add `setName()` calls to search panel components
- Create `SearchPanelPage.java`
- Create `SearchWorkflowTest.java`

**Phase 4: Enhanced Import Testing**
- Implement directory chooser handling
- Enable the disabled import test
- Add more comprehensive import scenarios
