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

import static ar.edu.unrc.exa.dc.util.Utils.generateTestsFile;
import static ar.edu.unrc.exa.dc.util.Utils.isValidPath;

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

    private ARepair aRepair;
    private BeAFix beAFix;
    private Set<BeAFixTest> ceAndPositiveTrustedTests; //this should be a hash set
    private Path modelToRepair;
    private Path oracle;

    public IterativeCEBasedAlloyRepair(Path modelToRepair, Path oracle, ARepair aRepair, BeAFix beAFix) {
        if (!isValidPath(modelToRepair, Utils.PathCheck.ALS))
            throw new IllegalArgumentException("Invalid model to repair path (" + (modelToRepair==null?"NULL":modelToRepair.toString()) + ")");
        if (!isValidPath(oracle, Utils.PathCheck.ALS))
            throw new IllegalArgumentException("Invalid oracle path (" + (oracle==null?"NULL":oracle.toString()) + ")");
        if (aRepair == null)
            throw new IllegalArgumentException("null ARepair instance");
        if (beAFix == null)
            throw new IllegalArgumentException("null BeAFix instance");
        this.aRepair = aRepair;
        this.aRepair.modelToRepair(modelToRepair);
        this.beAFix = beAFix;
        this.ceAndPositiveTrustedTests = new HashSet<>();
        this.modelToRepair = modelToRepair;
        this.oracle = oracle;
    }

    public Optional<FixCandidate> repair() throws IOException {
        Stack<FixCandidate> searchSpace = new Stack<>();
        FixCandidate originalCandidate = new FixCandidate(modelToRepair, 0, null);
        searchSpace.add(originalCandidate);
        while (!searchSpace.isEmpty()) {
            FixCandidate current = searchSpace.pop();
            ARepairResult aRepairResult = runARepairWithCurrentConfig(current);
            if (aRepairResult.hasRepair()) {
                FixCandidate repairCandidate = new FixCandidate(aRepairResult.repair(), 0, null);
                BeAFixResult beAFixResult = runBeAFixWithCurrentConfig(repairCandidate);
                if (beAFixResult.error()) {
                    logger.severe("BeAFix ended in error");
                } else if (beAFixResult.getCounterexampleTests().isEmpty()) {
                    return Optional.of(repairCandidate);
                } else {
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
        Path testsPath = Paths.get(candidate.modelToRepair().toAbsolutePath().toString().replace(".als", "_tests.als"));
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
        //TODO: implements
        return null;
    }

}
