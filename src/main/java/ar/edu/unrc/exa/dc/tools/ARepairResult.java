package ar.edu.unrc.exa.dc.tools;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public enum ARepairResult {

    REPAIRED,
    NOT_REPAIRED,
    ERROR,
    NO_TESTS,
    PARTIAL_REPAIR;

    private String message = null;
    private Path repair = null;
    private boolean npeFound = false;

    private List<BeAFixResult.BeAFixTest> usedTests = Collections.emptyList();

    public String message() {
        return message;
    }

    public void message(String msg) {
        this.message = msg;
    }

    public boolean hasMessage() {
        return message != null && !message.trim().isEmpty();
    }

    public Path repair() {
        return repair;
    }

    public void repair(Path repair) {
        this.repair = repair;
    }

    public boolean hasRepair() {
        return repair != null;
    }

    public void npeFound() {
        this.npeFound = true;
    }

    public boolean nullPointerExceptionFound() {
        return npeFound;
    }

    public void usedTests(List<BeAFixResult.BeAFixTest> usedTests) {
        this.usedTests = usedTests;
    }

    public List<BeAFixResult.BeAFixTest> usedTests() {
        return usedTests;
    }

    @Override
    public String toString() {
        String rep = "{\n\t" + name();
        if (!equals(NO_TESTS)) {
            if (hasMessage()) {
                rep += "\n\tMessage: " + message;
            }
            if (hasRepair()) {
                rep += "\n\tRepair found: " + repair.toAbsolutePath();
            }
        }
        rep += "\n}";
        return rep;
    }

}
