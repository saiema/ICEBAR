package ar.edu.unrc.exa.dc.search;

import ar.edu.unrc.exa.dc.icebar.Report;
import ar.edu.unrc.exa.dc.tools.ARepair;
import ar.edu.unrc.exa.dc.tools.ARepair.ARepairResult;
import ar.edu.unrc.exa.dc.tools.BeAFix;
import ar.edu.unrc.exa.dc.tools.BeAFixResult;
import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest;
import ar.edu.unrc.exa.dc.tools.InitialTests;
import ar.edu.unrc.exa.dc.util.TimeCounter;
import ar.edu.unrc.exa.dc.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static ar.edu.unrc.exa.dc.util.Utils.*;

public class IterativeCEBasedAlloyRepair {

    private static final Logger logger = Logger.getLogger(IterativeCEBasedAlloyRepair.class.getName());

    static {
        try {
            // This block configure the logger with handler and formatter
            FileHandler fh = new FileHandler("Repair.log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public static final int LAPS_DEFAULT = 4;

    private final ARepair aRepair;
    private final BeAFix beAFix;
    private final Set<BeAFixTest> trustedTests; //this should be a hash set
    private final Path modelToRepair;
    private final Path oracle;
    private final int laps;
    private int tests;

    public enum CegarSearch {
        DFS, BFS
    }

    private CegarSearch search = CegarSearch.DFS;
    public void setSearch(CegarSearch search) {
        this.search = search;
    }

    private boolean allowFactsRelaxation = false;
    public void allowFactsRelaxation(boolean allowFactsRelaxation) { this.allowFactsRelaxation = allowFactsRelaxation; }

    private boolean globalTrustedTests = true;
    public void globalTrustedTests(boolean globalTrustedTests) { this.globalTrustedTests = globalTrustedTests; }

    private boolean forceAssertionGeneration = false;
    public void forceAssertionGeneration(boolean forceAssertionGeneration) { this.forceAssertionGeneration = forceAssertionGeneration; }

    private long timeout = 0;
    public void timeout(long timeout) { this.timeout = timeout; }

    private boolean keepGoingAfterARepairNPE = false;
    public void keepGoingAfterARepairNPE(boolean keepGoingAfterARepairNPE) { this.keepGoingAfterARepairNPE =keepGoingAfterARepairNPE; }

    public IterativeCEBasedAlloyRepair(Path modelToRepair, Path oracle, ARepair aRepair, BeAFix beAFix, int laps) {
        if (!isValidPath(modelToRepair, Utils.PathCheck.ALS))
            throw new IllegalArgumentException("Invalid model to repair path (" + (modelToRepair==null?"NULL":modelToRepair.toString()) + ")");
        if (!isValidPath(oracle, Utils.PathCheck.ALS))
            throw new IllegalArgumentException("Invalid oracle path (" + (oracle==null?"NULL":oracle.toString()) + ")");
        if (aRepair == null)
            throw new IllegalArgumentException("null ARepair instance");
        if (beAFix == null)
            throw new IllegalArgumentException("null BeAFix instance");
        if (laps < 0)
            throw new IllegalArgumentException("Negative value for laps");
        this.aRepair = aRepair;
        this.aRepair.modelToRepair(modelToRepair);
        this.beAFix = beAFix;
        this.trustedTests = new HashSet<>();
        this.modelToRepair = modelToRepair;
        this.oracle = oracle;
        this.laps = laps;
        this.tests = 0;
    }

    public IterativeCEBasedAlloyRepair(Path modelToRepair, Path oracle, ARepair aRepair, BeAFix beAFix) {
        this(modelToRepair, oracle, aRepair, beAFix, LAPS_DEFAULT);
    }

    private InitialTests initialTests;
    public void setInitialTests(InitialTests initialTests) { this.initialTests = initialTests; }

    private boolean usePriorization = false;
    public void usePriorization(boolean usePriorization) {
        this.usePriorization = usePriorization;
    }


    public Optional<FixCandidate> repair() throws IOException {
        //watches for different time process recording
        TimeCounter arepairTimeCounter = new TimeCounter();
        TimeCounter beafixTimeCounter = new TimeCounter();
        TimeCounter totalTime = new TimeCounter();
        //CEGAR process
        CandidateSpace searchSpace = null;
        switch (search) {
            case DFS: {
                searchSpace = usePriorization?CandidateSpace.priorityStack():CandidateSpace.normalStack();
                break;
            }
            case BFS: {
                searchSpace = usePriorization?CandidateSpace.priorityQueue():CandidateSpace.normalQueue();
                break;
            }
        }
        FixCandidate originalCandidate = new FixCandidate(modelToRepair, 0, null);
        searchSpace.push(originalCandidate);
        int maxReachedLap = 0;
        totalTime.clockStart();
        while (!searchSpace.isEmpty()) {
            FixCandidate current = searchSpace.pop();
            maxReachedLap = Math.max(maxReachedLap, current.depth());
            logger.info("Repairing current candidate\n" + current);
            arepairTimeCounter.clockStart();
            ARepairResult aRepairResult = runARepairWithCurrentConfig(current);
            arepairTimeCounter.clockEnd();
            writeCandidateInfo(current, trustedTests, aRepairResult);
            logger.info("ARepair finished\n" + aRepairResult.toString());
            if (aRepairResult.equals(ARepairResult.ERROR)) {
                logger.severe("ARepair call ended in error:\n" + aRepairResult.message());
                if (aRepairResult.nullPointerExceptionFound() && keepGoingAfterARepairNPE) {
                    logger.warning("ARepair ended with a NullPointerException but we are going to ignore that and hope for the best");
                    continue;
                }
                Report report = Report.arepairFailed(current, current.untrustedTests().size() + current.trustedTests().size() + trustedTests.size(), arepairTimeCounter, beafixTimeCounter);
                writeReport(report);
                return Optional.empty();
            }
            if (aRepairResult.hasRepair() || aRepairResult.equals(ARepairResult.NO_TESTS)) {
                boolean fromOriginal = aRepairResult.equals(ARepairResult.NO_TESTS);
                FixCandidate repairCandidate = fromOriginal?current:new FixCandidate(aRepairResult.repair(), current.depth(), null);
                logger.info("Validating current candidate with BeAFix");
                beafixTimeCounter.clockStart();
                BeAFixResult beAFixCheckResult = runBeAFixWithCurrentConfig(repairCandidate, BeAFixMode.CHECK, false, false);
                beafixTimeCounter.clockEnd();
                logger.info("BeAFix check finished\n" + beAFixCheckResult.toString());
                int repairedPropertiesForCurrent = beAFixCheckResult.passingProperties();
                if (beAFixCheckResult.error()) {
                    logger.severe("BeAFix check ended in error, ending search");
                    Report report = Report.beafixCheckFailed(current, current.untrustedTests().size() + current.trustedTests().size() + trustedTests.size(), beafixTimeCounter, arepairTimeCounter);
                    writeReport(report);
                    return Optional.empty();
                } else if (beAFixCheckResult.checkResult()) {
                    logger.info("BeAFix validated the repair, fix found");
                    Report report = Report.repairFound(current, current.untrustedTests().size() + current.trustedTests().size() + trustedTests.size(), beafixTimeCounter, arepairTimeCounter);
                    writeReport(report);
                    return Optional.of(repairCandidate);
                } else if (current.depth() < laps) {
                    if (timeout > 0) {
                        totalTime.updateTotalTime();
                        if (totalTime.toMinutes() >= timeout) {
                            logger.info("CEGAR timeout (" + timeout + " minutes) reached");
                            Report report = Report.timeout(current, current.untrustedTests().size() + current.trustedTests().size() + trustedTests.size(), beafixTimeCounter, arepairTimeCounter);
                            writeReport(report);
                            return Optional.empty();
                        }
                    }
                    logger.info("BeAFix found the model to be invalid, generate tests and continue searching");
                    beafixTimeCounter.clockStart();
                    BeAFixResult beAFixResult = runBeAFixWithCurrentConfig(repairCandidate, BeAFixMode.TESTS, false, false);
                    beafixTimeCounter.clockEnd();
                    if (checkIfInvalidAndReportBeAFixResults(beAFixResult, current, beafixTimeCounter, arepairTimeCounter))
                        return Optional.empty();
                    beAFixResult.parseAllTests();
                    boolean noUntrustedTestsGenerated = beAFixResult.getUntrustedPositiveTests().isEmpty() && beAFixResult.getUntrustedNegativeTests().isEmpty();
                    boolean noTrustedTestsGenerated = beAFixResult.getTrustedPositiveTests().isEmpty() && beAFixResult.getTrustedNegativeTests().isEmpty() && beAFixResult.getCounterexampleTests().isEmpty();
                    if (allowFactsRelaxation && noUntrustedTestsGenerated && noTrustedTestsGenerated && trustedTests.isEmpty() && current.untrustedTests().isEmpty() && searchSpace.isEmpty()) {
                        logger.info("No tests available, generating with relaxed facts...");
                        beafixTimeCounter.clockStart();
                        beAFixResult = runBeAFixWithCurrentConfig(repairCandidate, BeAFixMode.TESTS, true, false);
                        beafixTimeCounter.clockEnd();
                        if (checkIfInvalidAndReportBeAFixResults(beAFixResult, current, beafixTimeCounter, arepairTimeCounter))
                            return Optional.empty();
                        beAFixResult.parseAllTests();
                        if (forceAssertionGeneration) {
                            logger.info("Generating with assertion forced test generation...");
                            beAFix.testsStartingIndex(Math.max(beAFix.testsStartingIndex(), beAFixResult.getMaxIndex()) + 1);
                            beafixTimeCounter.clockStart();
                            BeAFixResult beAFixResult_forcedAssertionTestGeneration = runBeAFixWithCurrentConfig(repairCandidate, BeAFixMode.TESTS, false, true);
                            beafixTimeCounter.clockEnd();
                            if (checkIfInvalidAndReportBeAFixResults(beAFixResult_forcedAssertionTestGeneration, current, beafixTimeCounter, arepairTimeCounter))
                                return Optional.empty();
                            beAFixResult.mergeWith(beAFixResult_forcedAssertionTestGeneration);
                        }
                    }
                    boolean trustedTestsAdded;
                    int newDepth = current.depth() + 1;
                    Set<BeAFixTest> trustedTests = new HashSet<>();
                    trustedTests.addAll(beAFixResult.getCounterexampleTests());
                    trustedTests.addAll(beAFixResult.getTrustedPositiveTests());
                    trustedTests.addAll(beAFixResult.getTrustedNegativeTests());
                    boolean addLocalTrustedTests;
                    if (globalTrustedTests || (current.untrustedTests().isEmpty() && current.trustedTests().isEmpty())) {
                        trustedTestsAdded = this.trustedTests.addAll(trustedTests);
                        addLocalTrustedTests = false;
                    } else { //local trusted tests except from original
                        trustedTestsAdded = !trustedTests.isEmpty();
                        addLocalTrustedTests = true;
                    }
                    for (BeAFixTest upTest : beAFixResult.getUntrustedPositiveTests()) {
                        Set<BeAFixTest> newTrustedTests = new HashSet<>(current.trustedTests());
                        boolean localTrustedTestsAdded = newTrustedTests.addAll(trustedTests);
                        Set<BeAFixTest> newTests = new HashSet<>(current.untrustedTests());
                        if (!newTests.add(upTest) && !trustedTestsAdded && !localTrustedTestsAdded)
                            continue;
                        FixCandidate newCandidateFromUntrustedTest;
                        if (!globalTrustedTests && addLocalTrustedTests)
                            newCandidateFromUntrustedTest = new FixCandidate(modelToRepair, newDepth, newTests, newTrustedTests);
                        else
                            newCandidateFromUntrustedTest = new FixCandidate(modelToRepair, newDepth, newTests);
                        newCandidateFromUntrustedTest.repairedProperties(repairedPropertiesForCurrent);
                        searchSpace.push(newCandidateFromUntrustedTest);
                    }
                    for (BeAFixTest unTest : beAFixResult.getUntrustedNegativeTests()) {
                        Set<BeAFixTest> newTrustedTests = new HashSet<>(current.trustedTests());
                        boolean localTrustedTestsAdded = newTrustedTests.addAll(trustedTests);
                        Set<BeAFixTest> newTests = new HashSet<>(current.untrustedTests());
                        if (!newTests.add(unTest) && !trustedTestsAdded && !localTrustedTestsAdded)
                            continue;
                        FixCandidate newCandidateFromUntrustedTest;
                        if (!globalTrustedTests && addLocalTrustedTests)
                            newCandidateFromUntrustedTest = new FixCandidate(modelToRepair, newDepth, newTests, newTrustedTests);
                        else
                            newCandidateFromUntrustedTest = new FixCandidate(modelToRepair, newDepth, newTests);
                        newCandidateFromUntrustedTest.repairedProperties(repairedPropertiesForCurrent);
                        searchSpace.push(newCandidateFromUntrustedTest);
                    }
                    if (noUntrustedTestsGenerated && trustedTestsAdded) {
                        FixCandidate onlyTrustedTestsCandidate;
                        if (!globalTrustedTests && addLocalTrustedTests)
                            onlyTrustedTestsCandidate = new FixCandidate(current.modelToRepair(), newDepth, current.untrustedTests(), trustedTests);
                        else
                            onlyTrustedTestsCandidate = new FixCandidate(current.modelToRepair(), newDepth, current.untrustedTests());
                        onlyTrustedTestsCandidate.repairedProperties(repairedPropertiesForCurrent);
                        searchSpace.push(onlyTrustedTestsCandidate);
                    }
                    tests += beAFixResult.generatedTests();
                    logger.info("Total tests generated: " + tests);
                    beAFix.testsStartingIndex(Math.max(beAFix.testsStartingIndex(), beAFixResult.getMaxIndex()) + 1);
                } else {
                    logger.info("max laps reached (" + laps + "), ending branch");
                }
            } else if (aRepairResult.hasMessage()) {
                logger.info("ARepair ended with the following message:\n" + aRepairResult.message());
            }
        }
        logger.info("CEGAR ended with no more candidates");
        Report report = Report.exhaustedSearchSpace(maxReachedLap, tests, beafixTimeCounter, arepairTimeCounter);
        writeReport(report);
        return Optional.empty();
    }


    private boolean checkIfInvalidAndReportBeAFixResults(BeAFixResult beAFixResult, FixCandidate current, TimeCounter beafixTimeCounter, TimeCounter arepairTimeCounter) throws IOException {
        if (!beAFixResult.error()) {
            String beafixMsg = "BeAFix finished\n";
            beafixMsg += beAFixResult + "\n";
            logger.info(beafixMsg);
            return false;
        } else {
            logger.severe("BeAFix test generation ended in error, ending search");
            Report report = Report.beafixGenFailed(current, tests, beafixTimeCounter, arepairTimeCounter);
            writeReport(report);
            return true;
        }
    }

    private ARepairResult runARepairWithCurrentConfig(FixCandidate candidate) {
        if (!aRepair.cleanFixDirectory())
            logger.warning("There was a problem cleaning ARepair .hidden folder, will keep going (cross your fingers)");
        Collection<BeAFixTest> tests = new LinkedList<>(trustedTests);
        tests.addAll(candidate.untrustedTests());
        tests.addAll(candidate.trustedTests());
        if (tests.isEmpty() && (initialTests == null || initialTests.getInitialTests().isEmpty()))
            return ARepairResult.NO_TESTS;
        Path testsPath = Paths.get(modelToRepair.toAbsolutePath().toString().replace(".als", "_tests.als"));
        File testsFile = testsPath.toFile();
        if (testsFile.exists()) {
            if (!testsFile.delete()) {
                logger.severe("Couldn't delete tests file (" + testsFile + ")");
                ARepairResult error = ARepairResult.ERROR;
                error.message("Couldn't delete tests file (" + testsFile + ")");
                return error;
            }
        }
        int testCount;
        try {
            if (initialTests != null)
                tests.addAll(initialTests.getInitialTests());
            testCount = generateTestsFile(tests, testsPath);
        } catch (IOException e) {
            logger.severe("An exception occurred while trying to generate tests file\n" + Utils.exceptionToString(e) + "\n");
            ARepairResult error = ARepairResult.ERROR;
            error.message(Utils.exceptionToString(e));
            return error;
        }
        logger.info("Running ARepair with " + testCount + " tests");
        aRepair.testsPath(testsPath);
        logger.info("Executing ARepair:\n" + aRepair.aRepairCommandToString());
        return aRepair.run();
    }

    private enum BeAFixMode {TESTS, CHECK}

    private BeAFixResult runBeAFixWithCurrentConfig(FixCandidate candidate, BeAFixMode mode, boolean relaxedFacts, boolean forceAssertionGeneration) {
        try {
            if (!beAFix.cleanOutputDir()) {
                return BeAFixResult.error("Couldn't delete BeAFix output directory");
            }
        } catch (IOException e) {
            logger.severe("An exception occurred when trying to clean BeAFix output directory\n" + exceptionToString(e));
            return BeAFixResult.error("An exception occurred when trying to clean BeAFix output directory\n" + exceptionToString(e));
        }
        Path modelToCheckWithOraclePath = Paths.get(candidate.modelToRepair().toAbsolutePath().toString().replace(".als", "_withOracle.als"));
        File modelToCheckWithOracleFile = modelToCheckWithOraclePath.toFile();
        if (modelToCheckWithOracleFile.exists()) {
            if (!modelToCheckWithOracleFile.delete()) {
                logger.severe("Couldn't delete model with oracle file (" + (modelToCheckWithOracleFile) + ")");
                return BeAFixResult.error("Couldn't delete model with oracle file (" + (modelToCheckWithOracleFile) + ")");
            }
        }
        try {
            mergeFiles(candidate.modelToRepair(), oracle, modelToCheckWithOraclePath);
        } catch (IOException e) {
            logger.severe("An exception occurred while trying to generate model with oracle file\n" + Utils.exceptionToString(e) + "\n");
            return BeAFixResult.error(Utils.exceptionToString(e));
        }
        BeAFixResult beAFixResult = null;
        beAFix.pathToModel(modelToCheckWithOraclePath);
        beAFix.factsRelaxationGeneration(relaxedFacts);
        beAFix.forceAssertionTestsGeneration(forceAssertionGeneration);
        switch (mode) {
            case TESTS: {
                beAFixResult = beAFix.runTestGeneration();
                break;
            }
            case CHECK: {
                beAFixResult = beAFix.runModelCheck();
                break;
            }
        }
        return beAFixResult;
    }

}
