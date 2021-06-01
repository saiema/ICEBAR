package ar.edu.unrc.exa.dc.icebar;

import ar.edu.unrc.exa.dc.search.FixCandidate;
import ar.edu.unrc.exa.dc.util.TimeCounter;

public class Report {

    public enum Status {
        REPAIR_FOUND {

            @Override
            public String toString() {
                return "correct";
            }

        },
        AREPAIR_FAILED {

            @Override
            public String toString() {
                return "ARepair_Fail";
            }

        },
        BEAFIX_CHECK_FAILED {

            @Override
            public String toString() {
                return "BeAFix_Check_Fail";
            }

        },
        BEAFIX_GEN_FAILED {

            @Override
            public String toString() {
                return "BeAFix_Gen_Fail";
            }

        },
        ICEBAR_INTERNAL_ERROR {

            @Override
            public String toString() { return "ICEBAR_INTERNAL_ERROR"; }

        },
        EXHAUSTED_CANDIDATES {

            @Override
            public String toString() {
                return "exhausted";
            }

        },
        TIMEOUT {

            @Override
            public String toString() {
                return "timeout";
            }

        }
        ;
        @Override
        public abstract String toString();
    }

    private final Status status;
    private final int tests;
    private final int laps;
    private final TimeCounter beafixTimer;
    private final TimeCounter arepairTimer;
    private int arepairCalls;

    private Report(Status status, FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls) {
        this.status = status;
        this.laps = candidate.depth();
        this.tests = tests;
        this.beafixTimer = beafixTimer;
        this.arepairTimer = arepairTimer;
        this.arepairCalls = arepairCalls;
    }

    private Report(Status status, int laps, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls) {
        this.status = status;
        this.laps = laps;
        this.tests = tests;
        this.beafixTimer = beafixTimer;
        this.arepairTimer = arepairTimer;
        this.arepairCalls = arepairCalls;
    }

    public static Report repairFound(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls) {
        return new Report(Status.REPAIR_FOUND, candidate, tests, beafixTimer, arepairTimer, arepairCalls);
    }

    public static Report arepairFailed(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls) {
        return new Report(Status.AREPAIR_FAILED, candidate, tests, beafixTimer, arepairTimer, arepairCalls);
    }

    public static Report beafixCheckFailed(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls) {
        return new Report(Status.BEAFIX_CHECK_FAILED, candidate, tests, beafixTimer, arepairTimer, arepairCalls);
    }

    public static Report beafixGenFailed(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls) {
        return new Report(Status.BEAFIX_GEN_FAILED, candidate, tests, beafixTimer, arepairTimer, arepairCalls);
    }

    public static Report icebarInternError(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls) {
        return new Report(Status.ICEBAR_INTERNAL_ERROR, candidate, tests, beafixTimer, arepairTimer, arepairCalls);
    }

    public static Report exhaustedSearchSpace(int laps, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls) {
        return new Report(Status.EXHAUSTED_CANDIDATES, laps, tests, beafixTimer, arepairTimer, arepairCalls);
    }

    public static Report timeout(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls) {
        return new Report(Status.TIMEOUT, candidate, tests, beafixTimer, arepairTimer, arepairCalls);
    }

    public static final String SEPARATOR = ";";

    @Override
    public String toString() {
        return status.toString() + SEPARATOR + laps + SEPARATOR + tests + SEPARATOR + beafixTimer.toMilliSeconds() + SEPARATOR + arepairTimer.toMilliSeconds() + SEPARATOR + arepairCalls;
    }

}