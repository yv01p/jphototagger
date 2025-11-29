package org.jphototagger.repository.sqlite;

/**
 * Record representing a default program configuration for a file suffix.
 */
public record DefaultProgramRecord(
    long idProgram,
    String filenameSuffix,
    String programAlias
) {}
