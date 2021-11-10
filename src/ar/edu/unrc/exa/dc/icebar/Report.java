package ar.edu.unrc.exa.dc.icebar;

import ar.edu.unrc.exa.dc.search.FixCandidate;
import ar.edu.unrc.exa.dc.util.TimeCounter;

public class Report {

    public static final class TestsAndCandidatesCounters {
        private final int totalUsedTests;
        private final int totalUsedTrustedTests;
        private final int totalUsedUntrustedTests;
        private final int evaluatedCandidates;
        private final int evaluatedCandidatesLeadingToNoFix;
        private final int evaluatedCandidatesLeadingToSpuriousFix;

        public TestsAndCandidatesCounters(int totalUsedTests, int totalUsedTrustedTests, int totalUsedUntrustedTests, int evaluatedCandidates, int evaluatedCandidatesLeadingToNoFix, int evaluatedCandidatesLeadingToSpuriousFix) {
            this.totalUsedTests = totalUsedTests;
            this.totalUsedTrustedTests = totalUsedTrustedTests;
            this.totalUsedUntrustedTests = totalUsedUntrustedTests;
            this.evaluatedCandidates = evaluatedCandidates;
            this.evaluatedCandidatesLeadingToNoFix = evaluatedCandidatesLeadingToNoFix;
            this.evaluatedCandidatesLeadingToSpuriousFix = evaluatedCandidatesLeadingToSpuriousFix;
        }

        @Override
        public String toString() {
            return totalUsedTests + SEPARATOR +
                    totalUsedTrustedTests + SEPARATOR +
                    totalUsedUntrustedTests + SEPARATOR +
                    evaluatedCandidates + SEPARATOR +
                    evaluatedCandidatesLeadingToNoFix + SEPARATOR +
                    evaluatedCandidatesLeadingToSpuriousFix;
        }

    }

    public enum Status {
        REPAIR_FOUND {

            @Override
            public String toString() {
                return "correct";
            }

        },
        AREPAIR_ONCE_SPURIOUS {

            @Override
            public String toString() {
                return "ARepair once Spurious";
            }

        },
        AREPAIR_ONCE_NO_FIX_FOUND {

            @Override
            public String toString() {
                return "ARepair once No Fix Found";
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
    private final int arepairCalls;
    private final TestsAndCandidatesCounters testsAndCandidatesCounters;

    private Report(Status status, FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls, TestsAndCandidatesCounters testsAndCandidatesCounters) {
        this.status = status;
        this.laps = candidate.depth();
        this.tests = tests;
        this.beafixTimer = beafixTimer;
        this.arepairTimer = arepairTimer;
        this.arepairCalls = arepairCalls;
        this.testsAndCandidatesCounters = testsAndCandidatesCounters;
    }

    private Report(Status status, int laps, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls, TestsAndCandidatesCounters testsAndCandidatesCounters) {
        this.status = status;
        this.laps = laps;
        this.tests = tests;
        this.beafixTimer = beafixTimer;
        this.arepairTimer = arepairTimer;
        this.arepairCalls = arepairCalls;
        this.testsAndCandidatesCounters = testsAndCandidatesCounters;
    }

    public static Report repairFound(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls, TestsAndCandidatesCounters testsAndCandidatesCounters) {
        return new Report(Status.REPAIR_FOUND, candidate, tests, beafixTimer, arepairTimer, arepairCalls, testsAndCandidatesCounters);
    }

    public static Report arepairFailed(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls, TestsAndCandidatesCounters testsAndCandidatesCounters) {
        return new Report(Status.AREPAIR_FAILED, candidate, tests, beafixTimer, arepairTimer, arepairCalls, testsAndCandidatesCounters);
    }

    public static Report beafixCheckFailed(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls, TestsAndCandidatesCounters testsAndCandidatesCounters) {
        return new Report(Status.BEAFIX_CHECK_FAILED, candidate, tests, beafixTimer, arepairTimer, arepairCalls, testsAndCandidatesCounters);
    }

    public static Report beafixGenFailed(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls, TestsAndCandidatesCounters testsAndCandidatesCounters) {
        return new Report(Status.BEAFIX_GEN_FAILED, candidate, tests, beafixTimer, arepairTimer, arepairCalls, testsAndCandidatesCounters);
    }

    public static Report icebarInternalError(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls, TestsAndCandidatesCounters testsAndCandidatesCounters) {
        return new Report(Status.ICEBAR_INTERNAL_ERROR, candidate, tests, beafixTimer, arepairTimer, arepairCalls, testsAndCandidatesCounters);
    }

    public static Report exhaustedSearchSpace(int laps, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls, TestsAndCandidatesCounters testsAndCandidatesCounters) {
        return new Report(Status.EXHAUSTED_CANDIDATES, laps, tests, beafixTimer, arepairTimer, arepairCalls, testsAndCandidatesCounters);
    }

    public static Report arepairOnceSpurious(int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls, TestsAndCandidatesCounters testsAndCandidatesCounters) {
        return new Report(Status.AREPAIR_ONCE_SPURIOUS, 0, tests, beafixTimer, arepairTimer, arepairCalls, testsAndCandidatesCounters);
    }

    public static Report arepairOnceNoFixFound(int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls, TestsAndCandidatesCounters testsAndCandidatesCounters) {
        return new Report(Status.AREPAIR_ONCE_NO_FIX_FOUND, 0, tests, beafixTimer, arepairTimer, arepairCalls, testsAndCandidatesCounters);
    }

    public static Report timeout(FixCandidate candidate, int tests, TimeCounter beafixTimer, TimeCounter arepairTimer, int arepairCalls, TestsAndCandidatesCounters testsAndCandidatesCounters) {
        return new Report(Status.TIMEOUT, candidate, tests, beafixTimer, arepairTimer, arepairCalls, testsAndCandidatesCounters);
    }

    public static final String SEPARATOR = ";";

    @Override
    public String toString() {
        return status.toString() + SEPARATOR +
                laps + SEPARATOR +
                tests + SEPARATOR +
                beafixTimer.toMilliSeconds() + SEPARATOR +
                arepairTimer.toMilliSeconds() + SEPARATOR +
                arepairCalls + (testsAndCandidatesCounters == null?"":(SEPARATOR + testsAndCandidatesCounters));
    }

}