package ar.edu.unrc.exa.dc.tools;

import ar.edu.unrc.exa.dc.util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

import static ar.edu.unrc.exa.dc.util.Utils.isValidPath;

public final class InitialTests {

    private final Path initialTestsPath;
    private final int maxIndex;
    private final Collection<BeAFixResult.BeAFixTest> initialTests;
    private final int maxScope;

    public InitialTests(Path initialTestsPath) {
        if (!isValidPath(initialTestsPath, Utils.PathCheck.TESTS))
            throw new IllegalArgumentException("Initial tests file is not valid (" + (initialTestsPath == null?"NULL":initialTestsPath.toString()) + ")");
        try {
            this.initialTestsPath = initialTestsPath;
            initialTests = BeAFixResult.parseTestsFrom(initialTestsPath, BeAFixResult.BeAFixTest.TestType.INITIAL);
            maxIndex = BeAFixResult.tests().getMaxIndexFrom(initialTests);
            maxScope = initialTests.stream().map(BeAFixResult.BeAFixTest::getMaxScope).max((o1, o2) -> o1 >= o2?o1:o2).orElse(BeAFixResult.BeAFixTest.NO_SCOPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse initial tests", e);
        }
    }

    public int getMaxIndex() {
        return maxIndex;
    }

    public int getMaxScope() { return maxScope; }

    public Collection<BeAFixResult.BeAFixTest> getInitialTests() {
        return initialTests;
    }

    @Override
    public String toString() {
        return "{\n\t Initial tests from : " + initialTestsPath.toString()
                + "\n\t Max test index found : " + maxIndex
                + "\n\t Tests:\n"
                + initialTests.stream().map(BeAFixResult.BeAFixTest::toString).collect(Collectors.joining("\n"))
        + "\n}";
    }

}
