package ar.edu.unrc.exa.dc.search;

import ar.edu.unrc.exa.dc.logging.LocalLogging;
import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest;
import ar.edu.unrc.exa.dc.util.TestHashes;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static ar.edu.unrc.exa.dc.util.Utils.*;

public final class FixCandidate {

    private static final Logger logger = LocalLogging.getLogger(FixCandidate.class);

    private static boolean checkRepeated = false;
    public static void checkRepeated(boolean checkRepeated) { FixCandidate.checkRepeated = checkRepeated; }

    private final Path modelToRepair;
    private final int depth;
    private final Collection<BeAFixTest> untrustedTests; //only untrusted tests
    private final Collection<BeAFixTest> trustedTests; //only trusted tests
    private int repairedProperties = 0;
    private final String id;
    private final TestHashes testHashes = new TestHashes();
    private final FixCandidate from;
    private final boolean hasLocalTests;

    public static FixCandidate initialCandidate(Path modelToRepair) {
        return new FixCandidate(modelToRepair, 0, null, null, null);
    }

    public static FixCandidate aRepairCheckCandidate(Path modelToRepair, int depth) {
        return new FixCandidate(modelToRepair, depth, null, null, null);
    }

    public static FixCandidate descendant(Path modelToRepair, Collection<BeAFixTest> untrustedTests, FixCandidate parent) {
        return descendant(modelToRepair, untrustedTests, null, parent);
    }

    public static FixCandidate descendant(Path modelToRepair, Collection<BeAFixTest> untrustedTests, Collection<BeAFixTest> trustedTests, FixCandidate parent) {
        if (parent == null)
            throw new IllegalArgumentException("Can't have a descendant with a null parent");
        return new FixCandidate(modelToRepair, parent.depth() + 1, untrustedTests, trustedTests, parent);
    }

    private FixCandidate(Path modelToRepair, int depth, Collection<BeAFixTest> untrustedTests, Collection<BeAFixTest> trustedTests, FixCandidate from) {
        id = generateRandomName();
        this.from = from;
        if (!isValidPath(modelToRepair, PathCheck.ALS))
            throw new IllegalArgumentException("Invalid model to repair (" + (modelToRepair==null?"NULL":modelToRepair.toString()) + ")");
        if (depth < 0)
            throw new IllegalArgumentException("negative depth (" + depth + ")");
        if (untrustedTests != null) {
            for (BeAFixTest uTest : untrustedTests) {
                if (uTest.testType().equals(BeAFixTest.TestType.TRUSTED)) {
                    throw new IllegalArgumentException("trusted or counterexample found in untrusted tests (" + uTest.testType() + ")");
                }
            }
        }
        this.modelToRepair = modelToRepair;
        this.depth = depth;
        this.untrustedTests = untrustedTests==null?new LinkedList<>():filterAlreadySeen(untrustedTests, from);
        this.trustedTests = trustedTests==null?new LinkedList<>():filterAlreadySeen(trustedTests, from);
        hasLocalTests = (!this.untrustedTests.isEmpty() || !this.trustedTests.isEmpty());
    }

    private Collection<BeAFixTest> filterAlreadySeen(Collection<BeAFixTest> tests, FixCandidate from) {
        if (from == null)
            return tests;
        Collection<BeAFixTest> filtered = new LinkedList<>();
        for (BeAFixTest test : tests) {
            if (!checkRepeated || !alreadySeen(test, this)) {
                filtered.add(test);
                testHashes.addHash(test);
            } else {
                logger.warning("Filtered test [" + test.currentTestHashCode() + "]");
            }
        }
        return filtered;
    }

    private boolean alreadySeen(BeAFixTest test, FixCandidate from) {
        if (from == null)
            return false;
        else if (from.testHashes.contains(test))
            return true;
        else if (from.from != null)
            return alreadySeen(test, from.from);
        return false;
    }

    public Path modelToRepair() {
        return this.modelToRepair;
    }

    public int depth() {
        return this.depth;
    }

    public Collection<BeAFixTest> untrustedTests() {
        return this.untrustedTests;
    }

    public Collection<BeAFixTest> trustedTests() {
        return this.trustedTests;
    }

    public void repairedProperties(int repairedProperties) {
        this.repairedProperties = repairedProperties;
    }

    public int repairedProperties() {
        return repairedProperties;
    }

    public String modelName() {
        return modelToRepair.getFileName().toString().replace(".als","");
    }

    public boolean hasLocalTests() {
        return hasLocalTests;
    }

    public String id() {
        return id;
    }

    public FixCandidate parent() { return from; }

    @Override
    public String toString() {
        return "Model: " + modelToRepair.toString() + "\n" + "Depth: " + depth + "\n" + "Repaired properties: " + repairedProperties;
    }

}
