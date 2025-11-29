package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class SqliteProgramsDatabaseTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private SqliteProgramsDatabase database;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        new SqliteTables(factory).createTables();
        database = new SqliteProgramsDatabase(factory);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    // Program record helper
    private ProgramRecord createProgram(String filename, String alias, boolean action) {
        return new ProgramRecord(
            -1L,  // id not set yet
            action,
            filename,
            alias,
            null,  // parametersBeforeFilename
            null,  // parametersAfterFilename
            false, // inputBeforeExecute
            false, // inputBeforeExecutePerFile
            false, // singleFileProcessing
            false, // changeFile
            -1,    // sequenceNumber (auto-assigned)
            false, // usePattern
            null   // pattern
        );
    }

    @Test
    void insertProgram_insertsAndSetsId() {
        ProgramRecord program = createProgram("/usr/bin/gimp", "GIMP", false);

        boolean inserted = database.insertProgram(program);

        assertThat(inserted).isTrue();
        assertThat(program.id()).isGreaterThan(0);
        assertThat(program.sequenceNumber()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void insertProgram_setsSequenceNumberWhenNegative() {
        ProgramRecord program1 = createProgram("/usr/bin/gimp", "GIMP", false);
        ProgramRecord program2 = createProgram("/usr/bin/inkscape", "Inkscape", false);

        database.insertProgram(program1);
        database.insertProgram(program2);

        assertThat(program1.sequenceNumber()).isEqualTo(0);
        assertThat(program2.sequenceNumber()).isEqualTo(1);
    }

    @Test
    void insertProgram_withAllFields() {
        ProgramRecord program = new ProgramRecord(
            -1L,
            true,  // action
            "/usr/bin/convert",
            "ImageMagick Convert",
            "-resize 800x600",
            "-quality 90",
            true,  // inputBeforeExecute
            true,  // inputBeforeExecutePerFile
            true,  // singleFileProcessing
            true,  // changeFile
            5,     // explicit sequence number
            true,  // usePattern
            "*.jpg"
        );

        boolean inserted = database.insertProgram(program);

        assertThat(inserted).isTrue();
        assertThat(program.id()).isGreaterThan(0);
        assertThat(program.sequenceNumber()).isEqualTo(5);
    }

    @Test
    void updateProgram_updatesFields() {
        ProgramRecord program = createProgram("/usr/bin/gimp", "GIMP", false);
        database.insertProgram(program);
        long originalId = program.id();

        ProgramRecord updated = new ProgramRecord(
            originalId,
            false,
            "/usr/bin/gimp-2.10",
            "GIMP 2.10",
            "-n",
            "--verbose",
            true,
            false,
            true,
            false,
            10,
            true,
            "*.png"
        );

        boolean result = database.updateProgram(updated);

        assertThat(result).isTrue();
        ProgramRecord found = database.findProgram(originalId);
        assertThat(found).isNotNull();
        assertThat(found.filename()).isEqualTo("/usr/bin/gimp-2.10");
        assertThat(found.alias()).isEqualTo("GIMP 2.10");
        assertThat(found.parametersBeforeFilename()).isEqualTo("-n");
        assertThat(found.parametersAfterFilename()).isEqualTo("--verbose");
        assertThat(found.inputBeforeExecute()).isTrue();
        assertThat(found.inputBeforeExecutePerFile()).isFalse();
        assertThat(found.singleFileProcessing()).isTrue();
        assertThat(found.changeFile()).isFalse();
        assertThat(found.sequenceNumber()).isEqualTo(10);
        assertThat(found.usePattern()).isTrue();
        assertThat(found.pattern()).isEqualTo("*.png");
    }

    @Test
    void deleteProgram_removesProgram() {
        ProgramRecord program = createProgram("/usr/bin/gimp", "GIMP", false);
        database.insertProgram(program);
        long id = program.id();

        boolean deleted = database.deleteProgram(program);

        assertThat(deleted).isTrue();
        assertThat(database.findProgram(id)).isNull();
    }

    @Test
    void getAllPrograms_returnsSortedBySequenceAndAlias() {
        ProgramRecord prog1 = new ProgramRecord(-1L, false, "/bin/b", "B Program", null, null, false, false, false, false, 1, false, null);
        ProgramRecord prog2 = new ProgramRecord(-1L, false, "/bin/a", "A Program", null, null, false, false, false, false, 1, false, null);
        ProgramRecord prog3 = new ProgramRecord(-1L, false, "/bin/c", "C Program", null, null, false, false, false, false, 0, false, null);
        ProgramRecord action = new ProgramRecord(-1L, true, "/bin/act", "Action 1", null, null, false, false, false, false, 0, false, null);

        database.insertProgram(prog1);
        database.insertProgram(prog2);
        database.insertProgram(prog3);
        database.insertProgram(action);

        List<ProgramRecord> programs = database.getAllPrograms(false); // Get programs only

        assertThat(programs).hasSize(3);
        assertThat(programs.get(0).alias()).isEqualTo("C Program"); // seq 0
        assertThat(programs.get(1).alias()).isEqualTo("A Program"); // seq 1, alphabetically first
        assertThat(programs.get(2).alias()).isEqualTo("B Program"); // seq 1, alphabetically second
    }

    @Test
    void getAllPrograms_returnsActionsOnly() {
        ProgramRecord program = createProgram("/bin/program", "Program", false);
        ProgramRecord action1 = createProgram("/bin/action1", "Action 1", true);
        ProgramRecord action2 = createProgram("/bin/action2", "Action 2", true);

        database.insertProgram(program);
        database.insertProgram(action1);
        database.insertProgram(action2);

        List<ProgramRecord> actions = database.getAllPrograms(true);

        assertThat(actions).hasSize(2);
        assertThat(actions).allMatch(ProgramRecord::action);
    }

    @Test
    void findProgram_returnsById() {
        ProgramRecord program = createProgram("/usr/bin/gimp", "GIMP", false);
        database.insertProgram(program);
        long id = program.id();

        ProgramRecord found = database.findProgram(id);

        assertThat(found).isNotNull();
        assertThat(found.id()).isEqualTo(id);
        assertThat(found.filename()).isEqualTo("/usr/bin/gimp");
        assertThat(found.alias()).isEqualTo("GIMP");
    }

    @Test
    void findProgram_returnsNullWhenNotFound() {
        ProgramRecord found = database.findProgram(99999L);

        assertThat(found).isNull();
    }

    @Test
    void existsProgram_returnsTrueWhenExists() {
        ProgramRecord program = createProgram("/usr/bin/gimp", "GIMP", false);
        database.insertProgram(program);

        boolean exists = database.existsProgram(program);

        assertThat(exists).isTrue();
    }

    @Test
    void existsProgram_returnsFalseWhenNotExists() {
        ProgramRecord program = createProgram("/usr/bin/nonexistent", "NonExistent", false);

        boolean exists = database.existsProgram(program);

        assertThat(exists).isFalse();
    }

    @Test
    void existsProgram_checksAliasAndFilename() {
        ProgramRecord program1 = createProgram("/usr/bin/gimp", "GIMP", false);
        database.insertProgram(program1);

        ProgramRecord sameAlias = createProgram("/different/path", "GIMP", false);
        ProgramRecord sameFilename = createProgram("/usr/bin/gimp", "Different Alias", false);

        assertThat(database.existsProgram(sameAlias)).isFalse();
        assertThat(database.existsProgram(sameFilename)).isFalse();
    }

    @Test
    void getProgramCount_returnsCorrectCount() {
        database.insertProgram(createProgram("/bin/p1", "P1", false));
        database.insertProgram(createProgram("/bin/p2", "P2", false));
        database.insertProgram(createProgram("/bin/a1", "A1", true));

        int programCount = database.getProgramCount(false);
        int actionCount = database.getProgramCount(true);

        assertThat(programCount).isEqualTo(2);
        assertThat(actionCount).isEqualTo(1);
    }

    @Test
    void hasProgram_returnsTrueWhenProgramExists() {
        database.insertProgram(createProgram("/bin/program", "Program", false));

        assertThat(database.hasProgram()).isTrue();
    }

    @Test
    void hasProgram_returnsFalseWhenNoProgramExists() {
        assertThat(database.hasProgram()).isFalse();
    }

    @Test
    void hasAction_returnsTrueWhenActionExists() {
        database.insertProgram(createProgram("/bin/action", "Action", true));

        assertThat(database.hasAction()).isTrue();
    }

    @Test
    void hasAction_returnsFalseWhenNoActionExists() {
        assertThat(database.hasAction()).isFalse();
    }

    @Test
    void getDefaultImageOpenProgram_returnsSequenceZeroProgram() {
        ProgramRecord defaultProg = new ProgramRecord(-1L, false, "/bin/default", "Default", null, null, false, false, false, false, 0, false, null);
        ProgramRecord otherProg = new ProgramRecord(-1L, false, "/bin/other", "Other", null, null, false, false, false, false, 1, false, null);

        database.insertProgram(defaultProg);
        database.insertProgram(otherProg);

        ProgramRecord found = database.getDefaultImageOpenProgram();

        assertThat(found).isNotNull();
        assertThat(found.alias()).isEqualTo("Default");
        assertThat(found.sequenceNumber()).isEqualTo(0);
        assertThat(found.action()).isFalse();
    }

    @Test
    void getDefaultImageOpenProgram_returnsNullWhenNone() {
        ProgramRecord found = database.getDefaultImageOpenProgram();

        assertThat(found).isNull();
    }

    @Test
    void setDefaultProgram_insertsNewDefault() {
        ProgramRecord program = createProgram("/bin/program", "Program", false);
        database.insertProgram(program);
        long programId = program.id();

        boolean result = database.setDefaultProgram("jpg", programId);

        assertThat(result).isTrue();
        assertThat(database.existsDefaultProgram("jpg")).isTrue();
    }

    @Test
    void setDefaultProgram_updatesExistingDefault() {
        ProgramRecord program1 = createProgram("/bin/p1", "P1", false);
        ProgramRecord program2 = createProgram("/bin/p2", "P2", false);
        database.insertProgram(program1);
        database.insertProgram(program2);

        database.setDefaultProgram("jpg", program1.id());
        boolean result = database.setDefaultProgram("jpg", program2.id());

        assertThat(result).isTrue();
        ProgramRecord found = database.findDefaultProgram("jpg");
        assertThat(found).isNotNull();
        assertThat(found.id()).isEqualTo(program2.id());
    }

    @Test
    void findDefaultProgram_returnsProgramBySuffix() {
        ProgramRecord program = createProgram("/bin/program", "Program", false);
        database.insertProgram(program);
        database.setDefaultProgram("jpg", program.id());

        ProgramRecord found = database.findDefaultProgram("jpg");

        assertThat(found).isNotNull();
        assertThat(found.id()).isEqualTo(program.id());
        assertThat(found.alias()).isEqualTo("Program");
    }

    @Test
    void findDefaultProgram_returnsNullWhenNotFound() {
        ProgramRecord found = database.findDefaultProgram("nonexistent");

        assertThat(found).isNull();
    }

    @Test
    void deleteDefaultProgram_removesDefault() {
        ProgramRecord program = createProgram("/bin/program", "Program", false);
        database.insertProgram(program);
        database.setDefaultProgram("jpg", program.id());

        boolean deleted = database.deleteDefaultProgram("jpg");

        assertThat(deleted).isTrue();
        assertThat(database.existsDefaultProgram("jpg")).isFalse();
    }

    @Test
    void existsDefaultProgram_returnsTrueWhenExists() {
        ProgramRecord program = createProgram("/bin/program", "Program", false);
        database.insertProgram(program);
        database.setDefaultProgram("jpg", program.id());

        assertThat(database.existsDefaultProgram("jpg")).isTrue();
    }

    @Test
    void existsDefaultProgram_returnsFalseWhenNotExists() {
        assertThat(database.existsDefaultProgram("nonexistent")).isFalse();
    }

    @Test
    void findAllDefaultPrograms_returnsAllDefaults() {
        ProgramRecord program1 = createProgram("/bin/p1", "P1", false);
        ProgramRecord program2 = createProgram("/bin/p2", "P2", false);
        database.insertProgram(program1);
        database.insertProgram(program2);
        database.setDefaultProgram("jpg", program1.id());
        database.setDefaultProgram("png", program2.id());

        List<DefaultProgramRecord> defaults = database.findAllDefaultPrograms();

        assertThat(defaults).hasSize(2);
        assertThat(defaults).extracting(DefaultProgramRecord::filenameSuffix)
            .containsExactlyInAnyOrder("jpg", "png");
    }

    @Test
    void deleteProgram_alsoDeletesFromDefaultPrograms() {
        ProgramRecord program = createProgram("/bin/program", "Program", false);
        database.insertProgram(program);
        database.setDefaultProgram("jpg", program.id());

        database.deleteProgram(program);

        assertThat(database.existsDefaultProgram("jpg")).isFalse();
    }
}
