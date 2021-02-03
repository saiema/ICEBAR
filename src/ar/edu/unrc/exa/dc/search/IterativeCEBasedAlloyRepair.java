package ar.edu.unrc.exa.dc.search;

import ar.edu.unrc.exa.dc.cegar.Report;
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
    public void setInitialTests(InitialTests initialTests) {
        this.initialTests = initialTests;
    }

    private boolean usePriorization = false;
    public void usePriorization(boolean usePriorization) {
        this.usePriorization = usePriorization;
    }

    public Optional<FixCandidate> repair() throws IOException {
        //watches for different time process recording
        TimeCounter arepairTimeCounter = new TimeCounter();
        TimeCounter beafixTimeCounter = new TimeCounter();
        //CEGAR process
        //Stack<FixCandidate> searchSpace = new Stack<>();
        CandidateSpace searchSpace = usePriorization?CandidateSpace.priorityStack():CandidateSpace.normalStack();
        FixCandidate originalCandidate = new FixCandidate(modelToRepair, 0, null);
        searchSpace.push(originalCandidate);
        int maxReachedLap = 0;
        while (!searchSpace.isEmpty()) {
            FixCandidate current = searchSpace.pop();
            maxReachedLap = Math.max(maxReachedLap, current.depth());
            logger.info("Repairing current candidate\n" + current.toString());
            arepairTimeCounter.clockStart();
            ARepairResult aRepairResult = runARepairWithCurrentConfig(current);
            arepairTimeCounter.clockEnd();
            logger.info("ARepair finished\n" + aRepairResult.toString());
            if (aRepairResult.equals(ARepairResult.ERROR)) {
                logger.severe("ARepair call ended in error:\n" + aRepairResult.message());
                Report report = Report.arepairFailed(current, tests, arepairTimeCounter, beafixTimeCounter);
                writeReport(report);
                return Optional.empty();
            }
            if (aRepairResult.hasRepair() || aRepairResult.equals(ARepairResult.NO_TESTS)) {
                FixCandidate repairCandidate = aRepairResult.equals(ARepairResult.NO_TESTS)?current:new FixCandidate(aRepairResult.repair(), current.depth(), null);
                logger.info("Validating current candidate with BeAFix");
                beafixTimeCounter.clockStart();
                BeAFixResult beAFixCheckResult = runBeAFixWithCurrentConfig(repairCandidate, BeAFixMode.CHECK);
                beafixTimeCounter.clockEnd();
                logger.info("BeAFix check finished\n" + beAFixCheckResult.toString());
                int repairedPropertiesForCurrent = beAFixCheckResult.passingProperties();
                if (beAFixCheckResult.error()) {
                    logger.severe("BeAFix check ended in error, ending search");
                    Report report = Report.beafixCheckFailed(current, tests, beafixTimeCounter, arepairTimeCounter);
                    writeReport(report);
                    return Optional.empty();
                } else if (beAFixCheckResult.checkResult()) {
                    logger.info("BeAFix validated the repair, fix found");
                    Report report = Report.repairFound(current, tests, beafixTimeCounter, arepairTimeCounter);
                    writeReport(report);
                    return Optional.of(repairCandidate);
                } else if (current.depth() < laps) {
                    logger.info("BeAFix found the model to be invalid, generate tests and continue searching");
                    beafixTimeCounter.clockStart();
                    BeAFixResult beAFixResult = runBeAFixWithCurrentConfig(repairCandidate, BeAFixMode.TESTS);
                    beafixTimeCounter.clockEnd();
                    if (!beAFixResult.error()) {
                        String beafixMsg = "BeAFix finished\n";
                        beafixMsg += beAFixResult.toString() + "\n";
                        logger.info(beafixMsg);
                    } else {
                        logger.severe("BeAFix test generation ended in error, ending search");
                        Report report = Report.beafixGenFailed(current, tests, beafixTimeCounter, arepairTimeCounter);
                        writeReport(report);
                        return Optional.empty();
                    }
                    boolean noUntrustedTests = beAFixResult.getUntrustedPositiveTests().isEmpty() && beAFixResult.getUntrustedNegativeTests().isEmpty();
                    boolean trustedTestsAdded;
                    int newDepth = current.depth() + 1;
                    trustedTestsAdded = trustedTests.addAll(beAFixResult.getCounterexampleTests());
                    trustedTestsAdded |= trustedTests.addAll(beAFixResult.getTrustedPositiveTests());
                    trustedTestsAdded |= trustedTests.addAll(beAFixResult.getTrustedNegativeTests());
                    for (BeAFixTest upTest : beAFixResult.getUntrustedPositiveTests()) {
                        Collection<BeAFixTest> newTests = new LinkedList<>(current.untrustedTests());
                        newTests.add(upTest);
                        FixCandidate newCandidateFromUntrustedTest = new FixCandidate(modelToRepair, newDepth, newTests);
                        newCandidateFromUntrustedTest.repairedProperties(repairedPropertiesForCurrent);
                        searchSpace.push(newCandidateFromUntrustedTest);
                    }
                    for (BeAFixTest unTest : beAFixResult.getUntrustedNegativeTests()) {
                        Collection<BeAFixTest> newTests = new LinkedList<>(current.untrustedTests());
                        newTests.add(unTest);
                        FixCandidate newCandidateFromUntrustedTest = new FixCandidate(modelToRepair, newDepth, newTests);
                        newCandidateFromUntrustedTest.repairedProperties(repairedPropertiesForCurrent);
                        searchSpace.push(newCandidateFromUntrustedTest);
                    }
                    if (noUntrustedTests && trustedTestsAdded) {
                        FixCandidate onlyTrustedTestsCandidate = new FixCandidate(current.modelToRepair(), newDepth, current.untrustedTests());
                        onlyTrustedTestsCandidate.repairedProperties(repairedPropertiesForCurrent);
                        searchSpace.push(onlyTrustedTestsCandidate);
                    }
                    tests = trustedTests.size() + beAFixResult.getUntrustedNegativeTests().size() + beAFixResult.getUntrustedPositiveTests().size();
                    logger.info("Total tests generated: " + tests);
                    beAFix.testsStartingIndex(Math.max(beAFix.testsStartingIndex(), beAFixResult.getMaxIndex()) + 1);
                } else {
                    logger.info("max laps reached (" + laps + "), ending branch");
                }
            } else if (aRepairResult.hasMessage()) {
                logger.info("ARepair ended with the following message:\n" + aRepairResult.message());
//                Report report = Report.arepairFailed(current, tests, beafixTimeCounter, arepairTimeCounter);
//                writeReport(report);
            }
        }
        logger.info("CEGAR ended with no more candidates");
        Report report = Report.exhaustedSearchSpace(maxReachedLap, tests, beafixTimeCounter, arepairTimeCounter);
        writeReport(report);
        return Optional.empty();
    }

    private ARepairResult runARepairWithCurrentConfig(FixCandidate candidate) {
        if (!aRepair.cleanFixDirectory())
            logger.warning("There was a problem cleaning ARepair .hidden folder, will keep going (cross your fingers)");
        Collection<BeAFixTest> tests = new LinkedList<>(trustedTests);
        tests.addAll(candidate.untrustedTests());
        if (tests.isEmpty())
            return ARepairResult.NO_TESTS;
        Path testsPath = Paths.get(modelToRepair.toAbsolutePath().toString().replace(".als", "_tests.als"));
        File testsFile = testsPath.toFile();
        if (testsFile.exists()) {
            if (!testsFile.delete()) {
                logger.severe("Couldn't delete tests file (" + testsFile.toString() + ")");
                ARepairResult error = ARepairResult.ERROR;
                error.message("Couldn't delete tests file (" + testsFile.toString() + ")");
                return error;
            }
        }
        try {
            if (initialTests != null)
                tests.addAll(initialTests.getInitialTests());
            generateTestsFile(tests, testsPath);
        } catch (IOException e) {
            logger.severe("An exception occurred while trying to generate tests file\n" + Utils.exceptionToString(e) + "\n");
            ARepairResult error = ARepairResult.ERROR;
            error.message(Utils.exceptionToString(e));
            return error;
        }
        logger.info("Running ARepair with " + tests.size() + " tests");
        aRepair.testsPath(testsPath);
        return aRepair.run();
    }

    private enum BeAFixMode {TESTS, CHECK}

    private BeAFixResult runBeAFixWithCurrentConfig(FixCandidate candidate, BeAFixMode mode) {
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
                logger.severe("Couldn't delete model with oracle file (" + (modelToCheckWithOracleFile.toString()) + ")");
                return BeAFixResult.error("Couldn't delete model with oracle file (" + (modelToCheckWithOracleFile.toString()) + ")");
            }
        }
        try {
            mergeFiles(candidate.modelToRepair(), oracle, modelToCheckWithOraclePath);
        } catch (IOException e) {
            logger.severe("An exception occurred while trying to generate model with oracle file\n" + Utils.exceptionToString(e) + "\n");
            return BeAFixResult.error(Utils.exceptionToString(e));
        }
        BeAFixResult beAFixResult = null;
        switch (mode) {
            case TESTS: {
                beAFixResult = beAFix.pathToModel(modelToCheckWithOraclePath).runTestGeneration();
                break;
            }
            case CHECK: {
                beAFixResult = beAFix.pathToModel(modelToCheckWithOraclePath).runModelCheck();
                break;
            }
        }
        return beAFixResult;
    }

}
