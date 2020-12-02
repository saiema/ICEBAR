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
}
