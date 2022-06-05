package ar.edu.unrc.exa.dc.tools;

import java.nio.file.Path;

public enum ARepairResult {

    REPAIRED,
    NOT_REPAIRED,
    ERROR,
    NO_TESTS,
    PARTIAL_REPAIR;

    private String message = null;
    private Path repair = null;
    private boolean npeFound = false;

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
