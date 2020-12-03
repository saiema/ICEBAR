package ar.edu.unrc.exa.dc.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Stack;
import java.util.stream.Collectors;
import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest.TestType;
import ar.edu.unrc.exa.dc.util.Utils;

public final class BeAFixResult {

    private static final String TEST_SEPARATOR = "===TEST===\n";

    public static final class BeAFixTest {

        public enum TestType {COUNTEREXAMPLE, UNTRUSTED_POSITIVE, UNTRUSTED_NEGATIVE, TRUSTED_POSITIVE}
        private final TestType testType;
        private String command;
        private String predicate;

        public BeAFixTest(String test, TestType testType) {
            if (test == null || test.trim().isEmpty())
                throw new IllegalArgumentException("null or empty test");
            if (testType == null)
                throw new IllegalArgumentException("null test type");
            this.testType = testType;
            parseTest(test);
        }

        public String command() {
            return command;
        }

        public String predicate() {
            return predicate;
        }

        public TestType testType() {
            return testType;
        }

        private static final String PREDICATE_START_DELIMITER = "--TEST START\n";
        private static final String PREDICATE_END_DELIMITER = "--TEST FINISH\n";
        private void parseTest(final String test) {
            this.predicate = Utils.getBetweenStrings(test, PREDICATE_START_DELIMITER, PREDICATE_END_DELIMITER);
            if (predicate.isEmpty())
                throw new IllegalArgumentException("Predicate not found in:\n" + test);
            int runIdx = test.indexOf("run");
            if (runIdx < 0)
                throw new IllegalArgumentException("Command not found in:\n" + test);
            this.command = test.substring(runIdx, test.indexOf("\n", runIdx));
        }

        @Override
        public int hashCode() {
            MessageDigest messageDigest;
            try {
                messageDigest = MessageDigest.getInstance("MD5");
                messageDigest.update(command().getBytes());
                byte[] digest = messageDigest.digest();
                return Arrays.hashCode(digest);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("This should not be happening!", e);
            }
        }

        @Override
        public String toString() {
            String rep = "{\n\t" + testType.name();
            rep += "\n\tPredicate:\n" + predicate;
            rep += "\n\tCommand: " + command;
            rep += "\n}";
            return rep;
        }

    }

    private Path cetFile;
    private Path uptFile;
    private Path untFile;
    private Path tptFile;
    private String message;
    private boolean error;

    private Collection<BeAFixTest> ceTests;
    private Collection<BeAFixTest> uptTests;
    private Collection<BeAFixTest> untTests;
    private Collection<BeAFixTest> tptTests;

    public void error(boolean error) {
        this.error = error;
    }

    public boolean error() {
        return this.error;
    }

    public void message(String message) {
        this.message = message;
    }

    public String message() {
        return this.message;
    }

    public void counterexampleTests(Path cetFile) {
        this.cetFile = cetFile;
    }

    public Path counterexampleTests() {
        return cetFile;
    }

    public void untrustedPositiveTests(Path uptFile) {
        this.uptFile = uptFile;
    }

    public Path untrustedPositiveTests() {
        return uptFile;
    }

    public void untrustedNegativeTests(Path untFile) {
        this.untFile = untFile;
    }

    public Path untrustedNegativeTests() {
        return untFile;
    }

    public void trustedPositiveTests(Path tptFile) {
        this.tptFile = tptFile;
    }

    public Path trustedPositiveTests() {
        return tptFile;
    }

    public Collection<BeAFixTest> getCounterexampleTests() throws IOException {
        if (ceTests == null)
            ceTests = parseTestsFrom(cetFile, TestType.COUNTEREXAMPLE);
        return ceTests;
    }

    public Collection<BeAFixTest> getUntrustedPositiveTests() throws IOException {
        if (uptTests == null)
            uptTests = parseTestsFrom(uptFile, TestType.UNTRUSTED_POSITIVE);
        return uptTests;
    }

    public Collection<BeAFixTest> getUntrustedNegativeTests() throws IOException {
        if (untTests == null)
            untTests = parseTestsFrom(untFile, TestType.UNTRUSTED_NEGATIVE);
        return untTests;
    }

    public Collection<BeAFixTest> getTrustedPositiveTests() throws IOException {
        if (tptTests == null)
            tptTests = parseTestsFrom(tptFile, TestType.TRUSTED_POSITIVE);
        return tptTests;
    }

    private boolean validateTestsFile(Path tests) {
        if (tests == null)
            return false;
        if (!tests.toFile().exists())
            return false;
        if (!tests.toFile().isFile())
            return false;
        return tests.toString().endsWith(".tests");
    }

    private Collection<BeAFixTest> parseTestsFrom(Path file, TestType testType) throws IOException {
        if (!validateTestsFile(file))
            throw new IllegalArgumentException("Invalid tests file: " + file.toString());
        String[] rawTests = Files.lines(file).collect(Collectors.joining("\n")).split(TEST_SEPARATOR);
        Collection<BeAFixTest> tests = new LinkedList<>();
        for (String rawTest : rawTests) {
            if (rawTest.trim().isEmpty())
                continue;
            BeAFixTest test = new BeAFixTest(rawTest, testType);
            tests.add(test);
        }
        return tests;
    }

    @Override
    public String toString() {
        String rep = "{\n\t";
        if (error)
            rep += "An error occurred!\n\tMessage: " + message + "\n}";
        else {
            rep += "Message: " + message;
            rep += "\n\tCounterexample tests:\n";
            rep += testsToString(TestType.COUNTEREXAMPLE);
            rep += "\n\tPositive trusted tests:\n";
            rep += testsToString(TestType.TRUSTED_POSITIVE);
            rep += "\n\tPositive untrusted tests:\n";
            rep += testsToString(TestType.UNTRUSTED_POSITIVE);
            rep += "\n\tNegative untrusted tests:\n";
            rep += testsToString(TestType.UNTRUSTED_NEGATIVE);
            rep += "}";
        }
        return rep;
    }

    private String testsToString(TestType testType) {
        StringBuilder rep = new StringBuilder();
        try {
            Collection<BeAFixTest> tests = null;
            switch (testType) {
                case COUNTEREXAMPLE: {
                    tests = getCounterexampleTests();
                    break;
                }
                case UNTRUSTED_POSITIVE: {
                    tests = getUntrustedPositiveTests();
                    break;
                }
                case UNTRUSTED_NEGATIVE: {
                    tests = getUntrustedNegativeTests();
                    break;
                }
                case TRUSTED_POSITIVE: {
                    tests = getTrustedPositiveTests();
                    break;
                }
            }
            for (BeAFixTest ceTest : tests) {
                rep.append(ceTest.toString()).append("\n");
            }
        } catch (IOException e) {
            rep.append("\tException while getting ").append(testType.name()).append(" tests:\n").append(Utils.exceptionToString(e));
        }
        return rep.toString();
    }

}
