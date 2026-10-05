package com.airline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** db/schema.sql is the readable standalone copy required by the assignment; it must match the migration. */
class SchemaFilesTest {

    @Test
    void standaloneSchema_matchesInitialMigration() throws Exception {
        String schema = Files.readString(Path.of("db/schema.sql")).replace("\r\n", "\n");
        String migration =
                Files.readString(Path.of("db/migrations/V1__init_schema.sql")).replace("\r\n", "\n");
        assertEquals(migration, schema, "db/schema.sql is out of sync with db/migrations/V1__init_schema.sql");
    }
}
