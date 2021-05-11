package ar.edu.unrc.exa.dc.search;

import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest;
import java.nio.file.Path;
import static ar.edu.unrc.exa.dc.util.Utils.*;
import java.util.Collection;
import java.util.LinkedList;

public class FixCandidate {

    private final Path modelToRepair;
    private final int depth;
    private final Collection<BeAFixTest> untrustedTests; //only untrusted tests
    private final Collection<BeAFixTest> trustedTests; //only trusted tests
    private int repairedProperties = 0;

    public FixCandidate(Path modelToRepair, int depth, Collection<BeAFixTest> untrustedTests) {
        this(modelToRepair, depth, untrustedTests, null);
    }

    public FixCandidate(Path modelToRepair, int depth, Collection<BeAFixTest> untrustedTests, Collection<BeAFixTest> trustedTests) {
        if (!isValidPath(modelToRepair, PathCheck.ALS))
            throw new IllegalArgumentException("Invalid model to repair (" + (modelToRepair==null?"NULL":modelToRepair.toString()) + ")");
        if (depth < 0)
            throw new IllegalArgumentException("negative depth (" + depth + ")");
        if (untrustedTests != null) {
            for (BeAFixTest uTest : untrustedTests) {
                if (uTest.testType().equals(BeAFixTest.TestType.COUNTEREXAMPLE) || uTest.testType().equals(BeAFixTest.TestType.TRUSTED_POSITIVE)) {
                    throw new IllegalArgumentException("trusted or counterexample found in untrusted tests (" + uTest.testType() + ")");
                }
            }
        }
        this.modelToRepair = modelToRepair;
        this.depth = depth;
        this.untrustedTests = untrustedTests==null?new LinkedList<>():untrustedTests;
        this.trustedTests = trustedTests==null?new LinkedList<>():trustedTests;
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

    @Override
    public String toString() {
        return "Model: " + modelToRepair.toString() + "\n" + "Depth: " + depth + "\n" + "Repaired properties: " + repairedProperties;
    }

}
