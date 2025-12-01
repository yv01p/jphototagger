# GUI Automation E2E Testing Design

**Date:** 2025-12-01
**Status:** Approved
**Scope:** Full GUI automation for Swing using AssertJ Swing, covering Import, Keyword Tagging, and Search workflows

---

## Overview

Implement end-to-end GUI tests for JPhotoTagger's three core workflows using AssertJ Swing with Page Object pattern. Tests run locally and in CI via Xvfb.

## Key Decisions

| Decision | Choice |
|----------|--------|
| Test location | `Program/src/test/java/org/jphototagger/e2e/` |
| Test data | Embedded placeholder JPGs in `resources/e2e/photos/` |
| Architecture | Page Objects wrapping AssertJ Swing interactions |
| Component naming | Hierarchical with type prefix (e.g., `dialog.import.btnStart`) |
| CI strategy | Xvfb (X Virtual Framebuffer) in GitHub Actions |
| Implementation order | Import → Keyword tagging → Search |

---

## Project Structure

```
Program/
├── src/
│   ├── main/java/...              # Existing app code
│   └── test/
│       ├── java/org/jphototagger/e2e/
│       │   ├── base/
│       │   │   ├── E2ETestBase.java       # Shared setup/teardown, robot init
│       │   │   └── TestDataManager.java   # Copy test photos to temp dirs
│       │   ├── pages/
│       │   │   ├── MainWindowPage.java    # Main frame interactions
│       │   │   ├── ImportDialogPage.java  # Import workflow
│       │   │   ├── KeywordPanelPage.java  # Keyword tagging
│       │   │   └── SearchPanelPage.java   # Search workflow
│       │   └── workflows/
│       │       ├── ImportWorkflowTest.java
│       │       ├── KeywordTaggingWorkflowTest.java
│       │       └── SearchWorkflowTest.java
│       └── resources/e2e/
│           ├── photos/
│           │   ├── test-photo-01.jpg      # Minimal placeholder JPG
│           │   ├── test-photo-02.jpg
│           │   └── test-photo-03.jpg
│           └── README.md                  # Documents test photo purposes
```

---

## Test Infrastructure

### E2ETestBase.java

All workflow tests extend this base class:

```java
@ExtendWith(GUITestExtension.class)
public abstract class E2ETestBase {
    protected static FrameFixture window;
    protected static Robot robot;
    protected TestDataManager testData;

    @BeforeAll
    static void launchApp() {
        robot = BasicRobot.robotWithNewAwtHierarchy();
        // Launch JPhotoTagger main frame
        Frame frame = GuiActionRunner.execute(() -> {
            AppMain.main(new String[]{});
            return findMainFrame();
        });
        window = new FrameFixture(robot, frame);
    }

    @BeforeEach
    void setupTestData() {
        testData = new TestDataManager();
        testData.createTempDirectory();
        testData.copyTestPhotos();
    }

    @AfterEach
    void cleanupTestData() {
        testData.cleanup();
    }

    @AfterAll
    static void tearDown() {
        window.cleanUp();
    }
}
```

### TestDataManager.java

Handles test photo lifecycle:
- Creates temp directory per test
- Copies placeholder JPGs from `resources/e2e/photos/`
- Cleans up after each test
- Provides paths for test assertions

---

## Page Objects

### MainWindowPage.java

Entry point, provides access to other pages:

```java
public class MainWindowPage {
    private final FrameFixture window;

    public MainWindowPage(FrameFixture window) {
        this.window = window;
    }

    public ImportDialogPage openImportDialog() {
        window.menuItem("menu.file.itemImport").click();
        DialogFixture dialog = window.dialog("dialog.import");
        return new ImportDialogPage(dialog);
    }

    public KeywordPanelPage keywordPanel() {
        return new KeywordPanelPage(window.panel("panel.keywords"));
    }

    public SearchPanelPage searchPanel() {
        return new SearchPanelPage(window.panel("panel.search"));
    }

    public DirectoryTreeComponent directoryTree() {
        return new DirectoryTreeComponent(window.tree("tree.directories"));
    }

    public ThumbnailPanelComponent thumbnailPanel() {
        return new ThumbnailPanelComponent(window.panel("panel.thumbnails"));
    }
}
```

### ImportDialogPage.java

```java
public class ImportDialogPage {
    private final DialogFixture dialog;

    public ImportDialogPage selectSourceFolder(File folder) {
        dialog.textBox("dialog.import.txtSourceFolder").setText(folder.getPath());
        return this;
    }

    public void clickImport() {
        dialog.button("dialog.import.btnStart").click();
        dialog.requireNotVisible();  // Dialog closes on success
    }
}
```

**Pattern:** Each page object returns `this` for fluent chaining, or a new page object when navigating.

---

## Workflow Tests

### ImportWorkflowTest.java

```java
class ImportWorkflowTest extends E2ETestBase {
    private MainWindowPage mainWindow;

    @BeforeEach
    void setup() {
        mainWindow = new MainWindowPage(window);
    }

    @Test
    void importPhotosFromFolder_displaysInThumbnailPanel() {
        // Arrange
        File sourceFolder = testData.getTestPhotosFolder();

        // Act
        mainWindow.openImportDialog()
                  .selectSourceFolder(sourceFolder)
                  .clickImport();

        // Assert
        mainWindow.directoryTree().requireSelection(sourceFolder.getName());
        mainWindow.thumbnailPanel().requirePhotoCount(3);
    }

    @Test
    void importPhotos_addsToDirectoryTree() {
        File sourceFolder = testData.getTestPhotosFolder();

        mainWindow.openImportDialog()
                  .selectSourceFolder(sourceFolder)
                  .clickImport();

        mainWindow.directoryTree()
                  .expandPath("Folders")
                  .requireNodeExists(sourceFolder.getName());
    }
}
```

**Test naming:** `methodUnderTest_expectedBehavior`

---

## Component Naming Requirements

Components that need `setName()` calls added to production code:

### Main Window
```java
menuFileImport.setName("menu.file.itemImport");
directoryTreePanel.setName("panel.directories");
keywordsPanel.setName("panel.keywords");
searchPanel.setName("panel.search");
thumbnailPanel.setName("panel.thumbnails");
directoryTree.setName("tree.directories");
keywordsTree.setName("tree.keywords");
```

### Import Dialog
```java
dialog.setName("dialog.import");
sourceTextField.setName("dialog.import.txtSourceFolder");
browseButton.setName("dialog.import.btnBrowse");
importButton.setName("dialog.import.btnStart");
cancelButton.setName("dialog.import.btnCancel");
```

### Search Panel
```java
searchField.setName("panel.search.txtQuery");
searchButton.setName("panel.search.btnSearch");
resultsTable.setName("panel.search.tableResults");
```

### Keyword Panel
```java
keywordsTree.setName("panel.keywords.treeKeywords");
addButton.setName("panel.keywords.btnAdd");
removeButton.setName("panel.keywords.btnRemove");
```

---

## Gradle & CI Configuration

### gradle/libs.versions.toml

```toml
[versions]
assertj-swing = "3.17.1"

[libraries]
assertj-swing = { module = "org.assertj:assertj-swing-junit", version.ref = "assertj-swing" }
```

### Program/build.gradle.kts

```kotlin
dependencies {
    testImplementation(libs.assertj.swing)
    testImplementation(libs.bundles.junit5)
    testImplementation(libs.assertj)
}

// Separate task for E2E tests (slower, needs display)
tasks.register<Test>("e2eTest") {
    description = "Runs E2E GUI tests"
    group = "verification"

    useJUnitPlatform {
        includeTags("e2e")
    }

    // Fail fast - stop on first failure for faster feedback
    failFast = true
}
```

### .github/workflows/build.yml

```yaml
- name: Run E2E tests
  run: xvfb-run --auto-servernum ./gradlew :Program:e2eTest
```

---

## Implementation Phases

### Phase 1: Infrastructure
1. Add AssertJ Swing dependency to version catalog
2. Create `Program/src/test/java/org/jphototagger/e2e/base/` package
3. Implement `E2ETestBase.java` and `TestDataManager.java`
4. Add placeholder test photos to `resources/e2e/photos/`
5. Configure `e2eTest` Gradle task
6. Add Xvfb step to GitHub Actions

### Phase 2: Import Workflow
1. Add `setName()` calls to import-related components
2. Create `MainWindowPage.java` with import dialog navigation
3. Create `ImportDialogPage.java`
4. Write `ImportWorkflowTest.java` with 2-3 tests
5. Verify tests pass locally and in CI

### Phase 3: Keyword Tagging Workflow
1. Add `setName()` calls to keyword panel components
2. Create `KeywordPanelPage.java`
3. Write `KeywordTaggingWorkflowTest.java`

### Phase 4: Search Workflow
1. Add `setName()` calls to search panel components
2. Create `SearchPanelPage.java`
3. Write `SearchWorkflowTest.java`

Each phase is self-contained and results in passing tests before moving on.
