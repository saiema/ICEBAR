package ar.edu.unrc.exa.dc.cegar;

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
        LAPS_REACHED {

            @Override
            public String toString() {
                return "Laps-OUT";
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
        EXHAUSTED_CANDIDATES {

            @Override
            public String toString() {
                return "exhausted";
            }

        }
        ;
        @Override
        public abstract String toString();
    }

    private final Status status;
    private final int tests;
    private final FixCandidate candidate;
    private final int laps;
    private final TimeCounter beafixTimer;
    private final TimeCounter arepairTimer;

    private Report(Status status, FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer) {
        this.status = status;
        this.candidate = candidate;
        this.laps = candidate.depth();
        this.tests = tests;
        this.beafixTimer = beafixTimer;
        this.arepairTimer = arepairTimer;
    }

    private Report(Status status, int laps, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer) {
        this.status = status;
        this.candidate = null;
        this.laps = laps;
        this.tests = tests;
        this.beafixTimer = beafixTimer;
        this.arepairTimer = arepairTimer;
    }

    public static Report repairFound(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer) {
        return new Report(Status.REPAIR_FOUND, candidate, tests, beafixTimer, arepairTimer);
    }

    public static Report lapsReached(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer) {
        return new Report(Status.LAPS_REACHED, candidate, tests, beafixTimer, arepairTimer);
    }

    public static Report arepairFailed(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer) {
        return new Report(Status.AREPAIR_FAILED, candidate, tests, beafixTimer, arepairTimer);
    }

    public static Report beafixCheckFailed(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer) {
        return new Report(Status.BEAFIX_CHECK_FAILED, candidate, tests, beafixTimer, arepairTimer);
    }

    public static Report beafixGenFailed(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer) {
        return new Report(Status.BEAFIX_GEN_FAILED, candidate, tests, beafixTimer, arepairTimer);
    }

    public static Report exhaustedSearchSpace(int laps, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer) {
        return new Report(Status.EXHAUSTED_CANDIDATES, laps, tests, beafixTimer, arepairTimer);
    }

    public static final String SEPARATOR = ";";

    @Override
    public String toString() {
        return status.toString() + SEPARATOR + laps + SEPARATOR + tests + SEPARATOR + beafixTimer.toMilliSeconds() + SEPARATOR + arepairTimer.toMilliSeconds();
    }

}
