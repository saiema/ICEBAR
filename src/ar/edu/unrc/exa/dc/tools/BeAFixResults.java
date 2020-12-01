package ar.edu.unrc.exa.dc.tools;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Stack;

public final class BeAFixResults {

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
            parseTest(test, 0);
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

        private void parseTest(final String test, final int from) {
            String text = from == 0?test.trim():test;
            if (from >= text.length() && (this.command == null || this.predicate == null))
                throw new IllegalArgumentException("Reached end of test and failed to found both command and predicate\n" + test);
            if (text.startsWith("pred", from)) {
                if (this.predicate != null)
                    throw new IllegalArgumentException("Two predicates found in test: " + this.predicate + " and " + text.substring(from));
                Stack<Character> curlyBracesStack = new Stack<>();
                StringBuilder pred = new StringBuilder();
                int currIdx = from;
                boolean stackUpdated = false;
                while (currIdx < test.length()) {
                    char currChar = test.charAt(currIdx);
                    if (currChar == '{') {
                        curlyBracesStack.push('{');
                        stackUpdated = true;
                    } else if (currChar == '}' && curlyBracesStack.peek() == '{')
                        curlyBracesStack.pop();
                    else if (currChar == '}' && curlyBracesStack.peek() == '}')
                        curlyBracesStack.push('{');
                    else
                        pred.append(currChar);
                    if (stackUpdated && curlyBracesStack.isEmpty())
                        break;
                    currIdx++;
                }
                this.predicate = pred.toString();
                if (this.command == null)
                    parseTest(test, currIdx + 1);
            } else if (text.startsWith("run", from)) {
                int eol = text.indexOf('\n', from);
                if (eol >= 0 && this.command == null) {
                    this.command = text.substring(from, eol);
                } else if (eol >= 0) {
                    throw new IllegalArgumentException("Two commands found in test: " + this.command + " and " + text.substring(from, eol));
                } else if (this.command == null) {
                    throw new IllegalArgumentException("No end of line character found starting with " + test.substring(from));
                }
                if (this.predicate == null)
                    parseTest(test, eol + 1);
            } else {
                throw new IllegalArgumentException("After trimming we should be seeing either pred or run but we get " + test.substring(from));
            }
        }

    }

    private Path cetFile;
    private Path uptFile;
    private Path untFile;
    private Path tptFile;
    private String message;

    private Collection<BeAFixTest> ceTests;
    private Collection<BeAFixTest> uptTests;
    private Collection<BeAFixTest> untTests;
    private Collection<BeAFixTest> tptTests;

    public void counterexampleTests(Path cetFile) {
        this.cetFile = cetFile;
    }

    public Path counterexampleTests() {
        return cetFile;
    }

    public boolean hasCounterexamples() {
        if (this.cetFile == null)
            return false;
        return ceTests == null || !ceTests.isEmpty();
    }

    public void untrustedPositiveTests(Path uptFile) {
        this.uptFile = uptFile;
    }

    public Path untrustedPositiveTests() {
        return uptFile;
    }

    public boolean hasUntrustedPositiveTests() {
        if (this.uptFile == null)
            return false;
        return uptTests == null || !uptTests.isEmpty();
    }

    public void untrustedNegativeTests(Path untFile) {
        this.untFile = untFile;
    }

    public Path untrustedNegativeTests() {
        return untFile;
    }

    public boolean hasUntrustedNegativeTests() {
        if (this.untFile == null)
            return false;
        return untTests == null || !untTests.isEmpty();
    }

    public void trustedPositiveTests(Path tptFile) {
        this.tptFile = tptFile;
    }

    public Path trustedPositiveTests() {
        return tptFile;
    }

    public boolean hasTrustedPositiveTests() {
        if (this.tptFile == null)
            return false;
        return tptTests == null || !tptTests.isEmpty();
    }

    public Collection<BeAFixTest> getCounterexampleTests() {
        if (ceTests == null)
            ceTests = parseTestsFrom(cetFile);
        return ceTests;
    }

    public Collection<BeAFixTest> getUntrustedPositiveTests() {
        if (uptTests == null)
            uptTests = parseTestsFrom(uptFile);
        return uptTests;
    }

    public Collection<BeAFixTest> getUntrustedNegativeTests() {
        if (untTests == null)
            untTests = parseTestsFrom(untFile);
        return untTests;
    }

    public Collection<BeAFixTest> getTrustedPositiveTests() {
        if (tptTests == null)
            tptTests = parseTestsFrom(tptFile);
        return tptTests;
    }

    private boolean validateTestsFile(Path tests) {
        if (!tests.toFile().exists())
            return false;
        if (!tests.toFile().isFile())
            return false;
        return tests.toString().endsWith(".tests");
    }

    private Collection<BeAFixTest> parseTestsFrom(Path file) {
        //TODO: implement
        return null;
    }

}
