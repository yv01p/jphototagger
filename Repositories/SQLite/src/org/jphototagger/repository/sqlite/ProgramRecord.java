package org.jphototagger.repository.sqlite;

/**
 * Mutable data record representing a program configuration.
 * Used as a DTO for program database operations, avoiding GUI dependencies.
 * Unlike typical records, this has mutable fields to allow database layer to set ID and sequence number.
 */
public class ProgramRecord {
    private long id;
    private final boolean action;
    private final String filename;
    private final String alias;
    private final String parametersBeforeFilename;
    private final String parametersAfterFilename;
    private final boolean inputBeforeExecute;
    private final boolean inputBeforeExecutePerFile;
    private final boolean singleFileProcessing;
    private final boolean changeFile;
    private int sequenceNumber;
    private final boolean usePattern;
    private final String pattern;

    public ProgramRecord(
        long id,
        boolean action,
        String filename,
        String alias,
        String parametersBeforeFilename,
        String parametersAfterFilename,
        boolean inputBeforeExecute,
        boolean inputBeforeExecutePerFile,
        boolean singleFileProcessing,
        boolean changeFile,
        int sequenceNumber,
        boolean usePattern,
        String pattern
    ) {
        this.id = id;
        this.action = action;
        this.filename = filename;
        this.alias = alias;
        this.parametersBeforeFilename = parametersBeforeFilename;
        this.parametersAfterFilename = parametersAfterFilename;
        this.inputBeforeExecute = inputBeforeExecute;
        this.inputBeforeExecutePerFile = inputBeforeExecutePerFile;
        this.singleFileProcessing = singleFileProcessing;
        this.changeFile = changeFile;
        this.sequenceNumber = sequenceNumber;
        this.usePattern = usePattern;
        this.pattern = pattern;
    }

    public long id() { return id; }
    public boolean action() { return action; }
    public String filename() { return filename; }
    public String alias() { return alias; }
    public String parametersBeforeFilename() { return parametersBeforeFilename; }
    public String parametersAfterFilename() { return parametersAfterFilename; }
    public boolean inputBeforeExecute() { return inputBeforeExecute; }
    public boolean inputBeforeExecutePerFile() { return inputBeforeExecutePerFile; }
    public boolean singleFileProcessing() { return singleFileProcessing; }
    public boolean changeFile() { return changeFile; }
    public int sequenceNumber() { return sequenceNumber; }
    public boolean usePattern() { return usePattern; }
    public String pattern() { return pattern; }

    // Mutable setters for database layer
    public void setId(long id) { this.id = id; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
}
