package ar.edu.unrc.exa.dc.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

import static ar.edu.unrc.exa.dc.util.Utils.validateTestsFile;

public final class InitialTests {

    private final Path initialTestsPath;
    private final int maxIndex;
    private final Collection<BeAFixResult.BeAFixTest> initialTests;

    public InitialTests(Path initialTestsPath) {
        if (!validateTestsFile(initialTestsPath))
            throw new IllegalArgumentException("Initial tests file is not valid (" + (initialTestsPath == null?"NULL":initialTestsPath.toString()) + ")");
        try {
            this.initialTestsPath = initialTestsPath;
            initialTests = BeAFixResult.parseTestsFrom(initialTestsPath, BeAFixResult.BeAFixTest.TestType.INITIAL);
            maxIndex = BeAFixResult.tests().getMaxIndexFrom(initialTests);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse initial tests", e);
        }
    }

    public int getMaxIndex() {
        return maxIndex;
    }

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
