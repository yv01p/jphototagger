package org.jphototagger.repository.hsqldb;

import java.sql.Connection;
import java.sql.SQLException;
import org.jphototagger.testsupport.TestDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for database repository tests.
 * Provides an isolated in-memory HSQLDB instance for each test.
 */
public abstract class DatabaseTestBase {

    protected TestDatabase testDb;

    @BeforeEach
    void setUpDatabase() throws SQLException {
        testDb = TestDatabase.createInMemory();
        createSchema(testDb);
    }

    @AfterEach
    void tearDownDatabase() {
        if (testDb != null) {
            testDb.close();
        }
    }

    /**
     * Override to create the database schema needed for tests.
     */
    protected abstract void createSchema(TestDatabase db) throws SQLException;

    /**
     * Gets a connection to the test database.
     */
    protected Connection getConnection() throws SQLException {
        return testDb.getConnection();
    }
}
