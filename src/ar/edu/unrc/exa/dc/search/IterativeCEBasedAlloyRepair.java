package ar.edu.unrc.exa.dc.search;

import ar.edu.unrc.exa.dc.tools.ARepair;
import ar.edu.unrc.exa.dc.tools.ARepair.ARepairResult;
import ar.edu.unrc.exa.dc.tools.BeAFixResult;
import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

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

    private Set<BeAFixTest> ceAndPositiveTrustedTests; //this should be a hash set
    private Path modelToRepair;
    private Path oracles;

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
        //TODO: implement
        return null;
    }

    public BeAFixResult runBeAFixWithCurrentConfig(FixCandidate candidate) {
        //TODO: implements
        return null;
    }

}
