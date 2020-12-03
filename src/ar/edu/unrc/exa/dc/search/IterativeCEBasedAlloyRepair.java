package ar.edu.unrc.exa.dc.search;

import ar.edu.unrc.exa.dc.tools.ARepair;
import ar.edu.unrc.exa.dc.tools.ARepair.ARepairResult;
import ar.edu.unrc.exa.dc.tools.BeAFix;
import ar.edu.unrc.exa.dc.tools.BeAFixResult;
import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest;
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
    private final Set<BeAFixTest> ceAndPositiveTrustedTests; //this should be a hash set
    private final Path modelToRepair;
    private final Path oracle;
    private final int laps;

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
        this.ceAndPositiveTrustedTests = new HashSet<>();
        this.modelToRepair = modelToRepair;
        this.oracle = oracle;
        this.laps = laps;
    }

    public IterativeCEBasedAlloyRepair(Path modelToRepair, Path oracle, ARepair aRepair, BeAFix beAFix) {
        this(modelToRepair, oracle, aRepair, beAFix, LAPS_DEFAULT);
    }

    public Optional<FixCandidate> repair() throws IOException {
        Stack<FixCandidate> searchSpace = new Stack<>();
        FixCandidate originalCandidate = new FixCandidate(modelToRepair, 0, null);
        searchSpace.add(originalCandidate);
        while (!searchSpace.isEmpty()) {
            FixCandidate current = searchSpace.pop();
            ARepairResult aRepairResult = runARepairWithCurrentConfig(current);
            if (aRepairResult.equals(ARepairResult.ERROR)) {
                logger.severe("ARepair call ended in error:\n" + aRepairResult.message());
                return Optional.empty();
            }
            if (aRepairResult.hasRepair()) {
                FixCandidate repairCandidate = new FixCandidate(aRepairResult.repair(), 0, null);
                BeAFixResult beAFixResult = runBeAFixWithCurrentConfig(repairCandidate);
                if (beAFixResult.error()) {
                    logger.severe("BeAFix ended in error");
                    return Optional.empty();
                } else if (beAFixResult.getCounterexampleTests().isEmpty()) {
                    return Optional.of(repairCandidate);
                } else if (current.depth() < laps) {
                    ceAndPositiveTrustedTests.addAll(beAFixResult.getCounterexampleTests());
                    ceAndPositiveTrustedTests.addAll(beAFixResult.getTrustedPositiveTests());
                    for (BeAFixTest upTest : beAFixResult.getUntrustedPositiveTests()) {
                        Collection<BeAFixTest> newTests = new LinkedList<>(current.untrustedTests());
                        newTests.add(upTest);
                        FixCandidate newCandidateFromUntrustedTest = new FixCandidate(modelToRepair, current.depth() + 1, newTests);
                        searchSpace.push(newCandidateFromUntrustedTest);
                    }
                    for (BeAFixTest unTest : beAFixResult.getUntrustedNegativeTests()) {
                        Collection<BeAFixTest> newTests = new LinkedList<>(current.untrustedTests());
                        newTests.add(unTest);
                        FixCandidate newCandidateFromUntrustedTest = new FixCandidate(modelToRepair, current.depth() + 1, newTests);
                        searchSpace.push(newCandidateFromUntrustedTest);
                    }
                }
            } else if (aRepairResult.hasMessage()) {
                logger.info("ARepair ended with the following message:\n" + aRepairResult.message());
            }
        }
        return Optional.empty();
    }

    public ARepairResult runARepairWithCurrentConfig(FixCandidate candidate) {
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
        Collection<BeAFixTest> tests = new LinkedList<>(ceAndPositiveTrustedTests);
        tests.addAll(candidate.untrustedTests());
        try {
            generateTestsFile(tests, testsPath);
        } catch (IOException e) {
            logger.severe("An exception occurred while trying to generate tests file\n" + Utils.exceptionToString(e) + "\n");
            ARepairResult error = ARepairResult.ERROR;
            error.message(Utils.exceptionToString(e));
            return error;
        }
        aRepair.testsPath(testsPath);
        return aRepair.run();
    }

    public BeAFixResult runBeAFixWithCurrentConfig(FixCandidate candidate) {
        Path modelToCheckWithOraclePath = Paths.get(candidate.modelToRepair().toAbsolutePath().toString().replace(".als", "_withOracle.als"));
        File modelToCheckWithOracleFile = modelToCheckWithOraclePath.toFile();
        if (modelToCheckWithOracleFile.exists()) {
            if (!modelToCheckWithOracleFile.delete()) {
                logger.severe("Couldn't delete model with oracle file (" + (modelToCheckWithOracleFile.toString()) + ")");
                BeAFixResult error = new BeAFixResult();
                error.error(true);
                error.message("Couldn't delete model with oracle file (" + (modelToCheckWithOracleFile.toString()) + ")");
                return error;
            }
        }
        try {
            mergeFiles(candidate.modelToRepair(), oracle, modelToCheckWithOraclePath);
        } catch (IOException e) {
            logger.severe("An exception occurred while trying to generate model with oracle file\n" + Utils.exceptionToString(e) + "\n");
            BeAFixResult error = new BeAFixResult();
            error.error(true);
            error.message(Utils.exceptionToString(e));
            return error;
        }
        return beAFix.pathToModel(modelToCheckWithOraclePath).run();
    }

}
