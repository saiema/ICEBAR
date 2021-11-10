package ar.edu.unrc.exa.dc.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest.TestType;
import ar.edu.unrc.exa.dc.util.OneTypePair;
import ar.edu.unrc.exa.dc.util.TestHashes;
import ar.edu.unrc.exa.dc.util.Utils;

import static ar.edu.unrc.exa.dc.util.Utils.getMaxScopeFromCommandSegments;
import static ar.edu.unrc.exa.dc.util.Utils.isValidPath;

public final class BeAFixResult {

    public static final String TEST_SEPARATOR = "===TEST===";

    public enum ResultType {TESTS, CHECK, ERROR}

    public static final class BeAFixTest {

        public enum TestType {TRUSTED, UNTRUSTED, INITIAL, BRANCH} //COUNTEREXAMPLE tests will be translated to TRUSTED and COUNTEREXAMPLE as source
        public enum TestSource {COUNTEREXAMPLE, PREDICATE}
        public enum Branch {POSITIVE, NEGATIVE, NONE, POSITIVE_AND_NEGATIVE, MULTIPLE, MULTIPLE_POSITIVE, MULTIPLE_NEGATIVE}
        private final TestType testType;
        private TestSource testSource;
        private Branch branch = Branch.NONE;
        private String command;
        private String predicate;
        private int index;
        private int subIndex;
        private static final String NOT_RELATED = null;
        private String relatedTest;
        private BeAFixTest relatedBeAFixTest;
        private BeAFixTest negativeBranch;
        private List<BeAFixTest> branches;
        public static int NO_SCOPE = -1;
        private int maxScope = NO_SCOPE;

        public BeAFixTest(String test, TestType testType) {
            if (test == null || test.trim().isEmpty())
                throw new IllegalArgumentException("null or empty test");
            if (testType == null)
                throw new IllegalArgumentException("null test type");
            this.testType = testType;
            try {
                parseTest(test);
            } catch (IllegalArgumentException | IllegalStateException e) {
                if (testType.equals(TestType.INITIAL)) {
                    parseTestInitialTestFallback(test);
                } else {
                    throw e;
                }
            }
        }

        private BeAFixTest(String command, String predicate, int index, String relatedTest, BeAFixTest relatedBeAFixTest, int maxScope, TestType testType, TestSource testSource, Branch branch, List<BeAFixTest> branches) {
            this.command = command;
            this.predicate = predicate;
            this.index = index;
            this.relatedTest = relatedTest;
            this.relatedBeAFixTest = relatedBeAFixTest;
            this.maxScope = maxScope;
            this.testType = testType;
            this.testSource = testSource;
            this.branch = branch;
            this.branches = branches;
        }

        private BeAFixTest(BeAFixTest positive, BeAFixTest negative) {
            this.testType = TestType.BRANCH;
            this.testSource = positive.testSource;
            this.branch = Branch.POSITIVE_AND_NEGATIVE;
            this.branches = positive.branches;
            this.command = positive.command;
            this.predicate = positive.predicate;
            this.index = positive.index;
            this.subIndex = positive.subIndex;
            this.relatedTest = positive.relatedTest;
            this.relatedBeAFixTest = positive.relatedBeAFixTest;
            this.negativeBranch = negative;
            this.maxScope = Math.max(positive.getMaxScope(), negative.getMaxScope());
        }

        private static final int MULTIBRANCH = 0;
        private static final int POSITIVE_MULTIBRANCH = 1;
        private static final int NEGATIVE_MULTIBRANCH = 2;
        private BeAFixTest(TestSource source, List<BeAFixTest> cases, int multibranchType) {
            if (cases == null || cases.size() < 2)
                throw new IllegalArgumentException("Either cases is null or there is none or one case (2 or more are required)");
            this.testType = TestType.BRANCH;
            this.testSource = source;
            if (multibranchType == POSITIVE_MULTIBRANCH)
                this.branch = Branch.MULTIPLE_POSITIVE;
            else if (multibranchType == NEGATIVE_MULTIBRANCH)
                this.branch = Branch.MULTIPLE_NEGATIVE;
            else
                this.branch = Branch.MULTIPLE;
            this.command = this.branch.name();
            this.predicate = this.branch.name();
            this.index = cases.get(0).getIndex();
            this.subIndex = -1;
            this.relatedTest = NOT_RELATED;
            this.relatedBeAFixTest = null;
            this.negativeBranch = null;
            this.maxScope = cases.stream().map(t -> t.maxScope).max(Integer::compareTo).orElse(NO_SCOPE);
            this.branches = new LinkedList<>();
            this.branches.addAll(cases);
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

        public int getIndex() { return index; }

        public int getSubIndex() { return subIndex; }

        public int getMaxScope() { return maxScope; }

        public boolean isRelated() { return relatedTest != null && !relatedTest.isEmpty(); }

        public boolean isCounterexampleTest() { return testSource.equals(TestSource.COUNTEREXAMPLE); }

        public boolean isPredicateTest() { return testSource.equals(TestSource.PREDICATE); }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean isPositiveAndNegativeBranch() { return testType.equals(TestType.BRANCH) && branch.equals(Branch.POSITIVE_AND_NEGATIVE); }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean isMultipleBranch() { return testType.equals(TestType.BRANCH) && (branch.equals(Branch.MULTIPLE) || branch.equals(Branch.MULTIPLE_POSITIVE) || branch.equals(Branch.MULTIPLE_NEGATIVE)); }

        public boolean isPositiveBranch() { return branch.equals(Branch.POSITIVE) || branch.equals(Branch.MULTIPLE_POSITIVE); }

        public boolean isNegativeBranch() { return branch.equals(Branch.NEGATIVE) || branch.equals(Branch.MULTIPLE_NEGATIVE); }

        public boolean isBranchedTest() {
            return !branch.equals(Branch.NONE) && !isPositiveBranch() && !isNegativeBranch();
        }

        public OneTypePair<BeAFixTest> getPositiveAndNegativeBranches() {
            if (!isPositiveAndNegativeBranch())
                throw new IllegalStateException("This is not a positive/negative branching test");
            if (negativeBranch == null)
                throw new IllegalStateException("This test has no associated negative test");
            TestType testType = this.branches == null?TestType.UNTRUSTED:TestType.BRANCH;
            BeAFixTest positive = new BeAFixTest(command, predicate, index, relatedTest, relatedBeAFixTest, maxScope, testType, testSource, this.branches==null?Branch.POSITIVE:Branch.MULTIPLE_POSITIVE, this.branches);
            BeAFixTest negative = negativeBranch;
            return new OneTypePair<>(positive, negative);
        }

        public List<BeAFixTest> getAlternateBranches() {
            if (!isMultipleBranch())
                throw new IllegalStateException("This is not a multiple branching test");
            if (branches == null || branches.isEmpty())
                throw new IllegalStateException("This test has no associated branches");
            return branches;
        }

        public String relatedTestID() { return relatedTest; }

        public BeAFixTest relatedBeAFixTest() {
            if (!isRelated())
                throw new IllegalStateException("This test is not related to another");
            return relatedBeAFixTest;
        }

        public void relatedBeAFixTest(BeAFixTest relatedBeAFixTest) {
            if (!isRelated())
                throw new IllegalStateException("This test is not related to anyone");
            if (!relatedBeAFixTest.isRelated())
                throw new IllegalArgumentException("Argument test is not related to anyone");
            if (relatedBeAFixTest.relatedTestID().compareTo(relatedTestID()) != 0)
                throw new IllegalArgumentException("This test is related to (" + relatedTestID() + ") but argument test is related to (" + relatedBeAFixTest.relatedTestID() + ")");
            this.relatedBeAFixTest = relatedBeAFixTest;
            relatedBeAFixTest.relatedBeAFixTest = this;
        }

        public static final String PREDICATE_START_DELIMITER = "--TEST START\n";
        public static final String PREDICATE_END_DELIMITER = "--TEST FINISH\n";
        private static final String COMMAND_NAME_SEPARATOR = "_";
        private static final String RELATED_TO_KEYWORD = "relTo-";
        private static final String RELATED_TO_KEYWORD_ALLOY_FRIENDLY = "relTo_";
        private static final String COMMAND_POS_KEYWORD = "POS";
        private static final String COMMAND_NEG_KEYWORD = "NEG";
        private static final String COMMAND_PREDICATE_KEYWORD = "PRED";
        private static final String COMMAND_COUNTEREXAMPLE_KEYWORD = "CE";
        //Test name should be <name>_<(PRED|CE)>_<index>_<subindex>[_<(POS|NEG>][_relTo-<ID>]
        //Initial tests can follow another naming convention
        private void parseTest(final String test) {
            String filteredRawTest = test.replaceAll(TEST_SEPARATOR, "");
            this.predicate = Utils.getBetweenStrings(filteredRawTest, PREDICATE_START_DELIMITER, PREDICATE_END_DELIMITER);
            if (predicate.isEmpty())
                throw new IllegalArgumentException("Predicate not found in:\n" + filteredRawTest);
            int runIdx = filteredRawTest.indexOf("run");
            if (runIdx < 0)
                throw new IllegalArgumentException("Command not found in:\n" + filteredRawTest);
            this.command = filteredRawTest.endsWith("\n")?filteredRawTest.substring(runIdx, filteredRawTest.indexOf("\n", runIdx)):filteredRawTest.substring(runIdx);
            String[] commandSegments = this.command.split(" ");
            if (commandSegments.length < 2)
                throw new IllegalArgumentException("Command was expected to have at least 2 words, but got " + commandSegments.length + " instead ( " + Arrays.toString(commandSegments) + ")");
            String commandFullName = commandSegments[1].trim();
            String[] commandNameSegments = commandFullName.split(COMMAND_NAME_SEPARATOR);
            if (commandNameSegments.length < 4)
                throw new IllegalStateException("Command name should at least have four (4) segments: <name>_<(PRED|CE)>_<index>_<subindex>[_<(POS|NEG>][_relTo-<ID>] (" + Arrays.toString(commandNameSegments) + ")");
            String testSourceRaw = commandNameSegments[1].trim();
            if (testSourceRaw.compareToIgnoreCase(COMMAND_COUNTEREXAMPLE_KEYWORD) == 0)
                testSource = TestSource.COUNTEREXAMPLE;
            else if (testSourceRaw.compareToIgnoreCase(COMMAND_PREDICATE_KEYWORD) == 0)
                testSource = TestSource.PREDICATE;
            else
                throw new IllegalStateException("Test source can't be determined, related value in test name is (" + commandNameSegments[1].trim() + ")") ;
            String indexRaw = commandNameSegments[2].trim();
            this.index = Integer.parseInt(indexRaw);
            String subIndexRaw = commandNameSegments[3].trim();
            this.subIndex = Integer.parseInt(subIndexRaw);
            if (commandNameSegments.length > 4) { //it should have at least one of [_<(POS|NEG>][_relTo-<ID>]
                String fifthValue = commandNameSegments[4].trim();
                String relToRaw = "";
                String posOrNegRaw = "";
                if (fifthValue.startsWith(RELATED_TO_KEYWORD)) { //
                    relToRaw = fifthValue;
                } else {
                    posOrNegRaw = fifthValue;
                    if (commandNameSegments.length > 5 && !commandNameSegments[5].trim().startsWith(RELATED_TO_KEYWORD)) {
                        throw new IllegalStateException("Test segments should end with relTo but it ends with " + commandNameSegments[5].trim());
                    } else if (commandNameSegments.length > 5) {
                        relToRaw = commandNameSegments[5].trim();
                    }
                }
                if (!relToRaw.isEmpty()) {
                    this.relatedTest = relToRaw.replace(RELATED_TO_KEYWORD, "");
                    this.predicate = this.predicate.replace(RELATED_TO_KEYWORD, RELATED_TO_KEYWORD_ALLOY_FRIENDLY);
                    this.command = this.command.replace(RELATED_TO_KEYWORD, RELATED_TO_KEYWORD_ALLOY_FRIENDLY);
                } else {
                    this.relatedTest = NOT_RELATED;
                }
                if (!posOrNegRaw.isEmpty()) {
                    if (posOrNegRaw.compareToIgnoreCase(COMMAND_POS_KEYWORD) == 0) {
                        this.branch = Branch.POSITIVE;
                    } else if (posOrNegRaw.compareToIgnoreCase(COMMAND_NEG_KEYWORD) == 0) {
                        this.branch = Branch.NEGATIVE;
                    } else {
                        throw new IllegalStateException("Test segment that should state either POS or NEG have " + posOrNegRaw + " instead");
                    }
                }
            }
            this.maxScope = getMaxScopeFromCommandSegments(commandSegments);
        }

        private void parseTestInitialTestFallback(final String test) {
            String filteredRawTest = test.replaceAll(TEST_SEPARATOR, "");
            this.predicate = Utils.getBetweenStrings(filteredRawTest, PREDICATE_START_DELIMITER, PREDICATE_END_DELIMITER);
            if (predicate.isEmpty())
                throw new IllegalArgumentException("Predicate not found in:\n" + filteredRawTest);
            int runIdx = filteredRawTest.indexOf("run");
            if (runIdx < 0)
                throw new IllegalArgumentException("Command not found in:\n" + filteredRawTest);
            this.command = filteredRawTest.endsWith("\n")?filteredRawTest.substring(runIdx, filteredRawTest.indexOf("\n", runIdx)):filteredRawTest.substring(runIdx);
            String[] commandSegments = this.command.split(" ");
            if (commandSegments.length < 2)
                throw new IllegalArgumentException("Command was expected to have at least 2 words, but got " + commandSegments.length + " instead ( " + Arrays.toString(commandSegments) + ")");
            String commandFullName = commandSegments[1].trim();
            String indexRaw = commandFullName.replaceAll("\\D+","");
            this.index = Integer.parseInt(indexRaw);
            this.subIndex = 0;
            this.relatedTest = NOT_RELATED;
            this.branch = Branch.NONE;
            this.maxScope = getMaxScopeFromCommandSegments(commandSegments);
        }

        @Override
        public int hashCode() {
            if (isBranchedTest())
                throw new IllegalStateException("Do not call this on branching tests");
            int hash = currentTestHashCode();
            if (hash == 0)
                throw new IllegalStateException("hash is zero at this point");
            return hash;
        }

        public int currentTestHashCode() {
            MessageDigest messageDigest;
            try {
                messageDigest = MessageDigest.getInstance("MD5");
                String expect = command().substring(command().indexOf("expect"));
                messageDigest.update(expect.getBytes());
                messageDigest.update(getPredicateBody().getBytes());
                byte[] digest = messageDigest.digest();
                return Arrays.hashCode(digest);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("This should not be happening!", e);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (other == null)
                return false;
            if (this == other)
                return true;
            if (!(other instanceof BeAFixTest))
                return false;
            BeAFixTest otherAsTest = (BeAFixTest) other;
            return hashCode() == otherAsTest.hashCode();
        }

        private String getPredicateBody() {
            int start = predicate.indexOf('{');
            if (start < 0)
                throw new IllegalStateException("There should be at least one { in the predicate\n" + predicate);
            return predicate.substring(start);
        }

        @Override
        public String toString() {
            if (isBranchedTest()) {
                if (isPositiveAndNegativeBranch())
                    return positiveAndNegativeToString();
                else if (isMultipleBranch())
                    return multipleBranchesToString();
            } else {
                return singleTestToString();
            }
            throw new IllegalStateException("Invalid state in toString");
        }

        private String singleTestToString() {
            String rep = "{\n\t" + testType.name();
            rep += "\n\tPredicate:\n" + predicate;
            rep += "\n\tCommand: " + command;
            rep += "\n}";
            return rep;
        }

        private String positiveAndNegativeToString() {
            String rep = "{\n\t Positive and Negative branch";
            rep += "\n\t Positive branch :\n";
            if (isMultipleBranch()) {
                rep += multipleBranchesToString();
            } else {
                rep += singleTestToString();
            }
            rep += "\n\t Negative branch :\n" + negativeBranch.toString();
            rep += "\n}";
            return rep;
        }

        private String multipleBranchesToString() {
            StringBuilder rep = new StringBuilder("{\n\t Multiple branches");
            for (BeAFixTest branch : branches) {
                rep.append("\n").append(branch.toString()).append("\n");
            }
            rep.append("\n}");
            return rep.toString();
        }

    }

    private Path cetFile;
    private Path ttFile;
    private Path utFile;
    private String message;
    private boolean check;
    private boolean testsParsed = false;

    private List<BeAFixTest> ceTests;
    private List<BeAFixTest> ttTests;
    private List<BeAFixTest> utTests;
    private int maxIndex = -1;
    private ResultType resultType;
    private int generatedTests = 0;

    //only for checks
    private int passingProperties = -1;
    private int totalProperties = -1;

    public int passingProperties() {
        return passingProperties;
    }

    private BeAFixResult() {}

    public static BeAFixResult tests() {
        BeAFixResult beAFixResult = new BeAFixResult();
        beAFixResult.resultType = ResultType.TESTS;
        return beAFixResult;
    }

    public static BeAFixResult error(String message) {
        BeAFixResult beAFixResult = new BeAFixResult();
        beAFixResult.resultType = ResultType.ERROR;
        beAFixResult.message(message);
        return beAFixResult;
    }

    public static BeAFixResult check(Path checkFile) {
        if (checkFile == null)
            throw new IllegalArgumentException("checkFile is null");
        if (!checkFile.toFile().exists())
            return error("No check file found at: " + checkFile);
        return parseCheckFile(checkFile);
    }

    public boolean isCheck() { return this.resultType.equals(ResultType.CHECK); }

    public boolean checkResult() {
        if (!isCheck()) {
            throw new IllegalStateException("This is not a CHECK result");
        }
        return check;
    }

    public boolean error() {
        return this.resultType.equals(ResultType.ERROR);
    }

    public void message(String message) {
        this.message = message;
    }

    public String message() {
        return this.message;
    }

    public int getMaxIndex() {
        return maxIndex;
    }

    public int generatedTests() {
        return generatedTests;
    }

    int getMaxIndexFrom(Collection<BeAFixTest> tests) {
        int max = 0;
        for (BeAFixTest test : tests) {
            if (test.getIndex() > max)
                max = test.getIndex();
        }
        return max;
    }

    public void counterexampleTestsFile(Path cetFile) {
        this.cetFile = cetFile;
    }

    public void trustedTestsFile(Path ttFile) {
        this.ttFile = ttFile;
    }

    public void untrustedTestsFile(Path utFile) {
        this.utFile = utFile;
    }

    private List<BeAFixTest> counterexampleTests = null;
    public List<BeAFixTest> getCounterexampleTests() {
        if (!testsParsed)
            throw new IllegalStateException("Tests are not yet parsed (must call #parseAllTests first)");
        if (counterexampleTests == null) {
            counterexampleTests = new LinkedList<>(ceTests);
            for (BeAFixTest tTest : ttTests) {
                if (tTest.testSource.equals(BeAFixTest.TestSource.COUNTEREXAMPLE))
                    counterexampleTests.add(tTest);
            }
        }
        return counterexampleTests;
    }

    private List<BeAFixTest> counterExampleUntrustedTests = null;
    public List<BeAFixTest> getCounterExampleUntrustedTests() {
        if (!testsParsed)
            throw new IllegalStateException("Tests are not yet parsed (must call #parseAllTests first)");
        if (counterExampleUntrustedTests == null) {
            counterExampleUntrustedTests = new LinkedList<>();
            for (BeAFixTest utTest : utTests) {
                if (utTest.testSource.equals(BeAFixTest.TestSource.COUNTEREXAMPLE))
                    counterExampleUntrustedTests.add(utTest);
            }
        }
        return counterExampleUntrustedTests;
    }

    private List<BeAFixTest> predicateTests = null;
    public List<BeAFixTest> getPredicateTests() {
        if (!testsParsed)
            throw new IllegalStateException("Tests are not yet parsed (must call #parseAllTests first)");
        if (predicateTests == null) {
            predicateTests = new LinkedList<>();
            for (BeAFixTest utTest : utTests) {
                if (utTest.testSource.equals(BeAFixTest.TestSource.PREDICATE))
                    predicateTests.add(utTest);
            }
        }
        return predicateTests;
    }

    public void parseAllTests() throws IOException {
        if (testsParsed)
            throw new IllegalStateException("Tests already parsed");
        if (!isCheck() && !error()) {
            List<BeAFixTest> ceTests = parseCounterexampleTests();
            List<BeAFixTest> ttTests = parseTrustedTests();
            List<BeAFixTest> utTests = parseUntrustedTests();
            generatedTests = ceTests.size() + ttTests.size() + utTests.size();
            mergeRelated(ceTests, ttTests);
            mergeRelated(utTests);
            mergeMultipleBranches(utTests);
            mergePositiveAndNegativeBranches(utTests);
            testsParsed = true;
        }
    }

    private List<BeAFixTest> parseCounterexampleTests() throws IOException {
        if (ceTests == null)
            ceTests = (isCheck() || error()) ? new LinkedList<>() : parseTestsFrom(cetFile, TestType.TRUSTED);
        maxIndex = Math.max(maxIndex, getMaxIndexFrom(ceTests));
        return ceTests;
    }

    private List<BeAFixTest> parseTrustedTests() throws IOException {
        if (ttTests == null)
            ttTests = (isCheck() || error()) ? new LinkedList<>() : parseTestsFrom(ttFile, TestType.TRUSTED);
        maxIndex = Math.max(maxIndex, getMaxIndexFrom(ttTests));
        return ttTests;
    }

    private List<BeAFixTest> parseUntrustedTests() throws IOException {
        if (utTests == null)
            utTests = (isCheck() || error()) ? new LinkedList<>() : parseTestsFrom(utFile, TestType.UNTRUSTED);
        maxIndex = Math.max(maxIndex, getMaxIndexFrom(utTests));
        return utTests;
    }


    @SafeVarargs
    private final void mergeRelated(final List<BeAFixTest>... tests) {
        if (tests == null || tests.length == 0)
            return;
        boolean merged = true;
        while (merged) {
            merged = false;
            for (int t = 0; t < tests[0].size(); t++) {
                BeAFixTest currentTest = tests[0].get(t);
                if (!currentTest.isRelated())
                    continue;
                if (currentTest.relatedBeAFixTest() != null)
                    continue;
                for (List<BeAFixTest> otherTests : tests) {
                    Optional<BeAFixTest> relatedTest = searchAndRemoveRelatedTest(currentTest, otherTests);
                    if (relatedTest.isPresent()) {
                        currentTest.relatedBeAFixTest(relatedTest.get());
                        merged = true;
                        break;
                    }
                }
                if (currentTest.isRelated() && !merged)
                    throw new IllegalStateException("Related test is missing it's related BeAFixTest test");
            }
        }
    }

    private void mergeMultipleBranches(List<BeAFixTest> untrustedTests) {
        if (untrustedTests == null || untrustedTests.isEmpty())
            return;
        boolean merged = true;
        while (merged) {
            merged = false;
            for (int i = 0; i < untrustedTests.size(); i++) {
                BeAFixTest currentTest = untrustedTests.get(i);
                List<BeAFixTest> alternateTests = getAlternateCases(currentTest, untrustedTests);
                if (alternateTests.size() > 1) {
                    int branchType = BeAFixTest.MULTIBRANCH;
                    if (currentTest.isPositiveBranch())
                        branchType = BeAFixTest.POSITIVE_MULTIBRANCH;
                    else if (currentTest.isNegativeBranch())
                        branchType = BeAFixTest.NEGATIVE_MULTIBRANCH;
                    BeAFixTest multipleBranchTest = new BeAFixTest(currentTest.testSource, alternateTests, branchType);
                    merged = true;
                    removeCases(untrustedTests, alternateTests);
                    untrustedTests.add(multipleBranchTest);
                    break;
                }
            }
        }
    }

    private List<BeAFixTest> getAlternateCases(BeAFixTest from, List<BeAFixTest> untrustedTests) {
        boolean isPositive = from.isPositiveBranch();
        boolean isNegative = from.isNegativeBranch();
        if (!isPositive && !isNegative) {
            return untrustedTests.stream().filter(t -> t.getIndex() == from.getIndex()).collect(Collectors.toList());
        } else if (isPositive) {
            return untrustedTests.stream().filter(t -> t.getIndex() == from.getIndex() && t.isPositiveBranch()).collect(Collectors.toList());
        } else {
            return untrustedTests.stream().filter(t -> t.getIndex() == from.getIndex() && t.isNegativeBranch()).collect(Collectors.toList());
        }
    }

    private void removeCases(List<BeAFixTest> from, List<BeAFixTest> toRemove) {
        Set<Integer> hashesToRemove = toRemove.stream().map(System::identityHashCode).collect(Collectors.toSet());
        for (int i = 0; i < from.size(); i++) {
            BeAFixTest current = from.get(i);
            if (hashesToRemove.contains(System.identityHashCode(current))) {
                from.remove(i);
                i--;
            }
        }
    }

    private void mergePositiveAndNegativeBranches(List<BeAFixTest> untrustedTests) {
        if (untrustedTests == null || untrustedTests.isEmpty())
            return;
        boolean merged = true;
        while (merged) {
            merged = false;
            for (int i = 0; i < untrustedTests.size(); i++) {
                BeAFixTest currentTest = untrustedTests.get(i);
                if (!currentTest.isPositiveBranch())
                    continue;
                Optional<BeAFixTest> opNegativeBranch = searchAndRemoveBranches(currentTest, untrustedTests);
                if (!opNegativeBranch.isPresent())
                    throw new IllegalStateException("No negative branch for positive branch:\n" + currentTest);
                BeAFixTest branch = new BeAFixTest(currentTest, opNegativeBranch.get());
                untrustedTests.add(branch);
                merged = true;
            }
        }
    }

    private Optional<BeAFixTest> searchAndRemoveBranches(BeAFixTest positiveTest, List<BeAFixTest> from) {
        if (!positiveTest.isPositiveBranch())
            throw new IllegalArgumentException("Positive test is not a positive test:\n" + positiveTest);
        int positiveTestIdx = -1;
        int negativeTestIdx = -1;
        boolean positiveFirst = false;
        BeAFixTest negativeTest = null;
        for (int i = 0; i < from.size(); i++) {
            BeAFixTest current = from.get(i);
            if (current.isPositiveBranch() && current == positiveTest) {
                positiveTestIdx = i;
                positiveFirst = negativeTestIdx < 0;
            }
            if (current.isNegativeBranch() && current.getIndex() == positiveTest.getIndex()) {
                negativeTestIdx = i;
                negativeTest = current;
            }
        }
        if (positiveFirst) {     //[...positiveTest...negativeTest...]
            from.remove(positiveTestIdx);
            from.remove(negativeTestIdx - 1);
        } else {                //[...negativeTest...positiveTest...]
            from.remove(negativeTestIdx);
            from.remove(positiveTestIdx - 1);
        }
        return Optional.ofNullable(negativeTest);
    }

    private Optional<BeAFixTest> searchAndRemoveRelatedTest(BeAFixTest test, List<BeAFixTest> from) {
        if (!test.isRelated())
            throw new IllegalArgumentException("test is not related to anyone");
        for (int t = 0; t < from.size(); t++) {
            BeAFixTest other = from.get(t);
            if (!other.isRelated())
                continue;
            if (test.relatedTestID().compareTo(other.relatedTestID()) != 0)
                continue;
            if (test.command().compareTo(other.command()) == 0)
                continue;
            from.remove(t);
            return Optional.of(other);
        }
        return Optional.empty();
    }

    static List<BeAFixTest> parseTestsFrom(Path file, TestType testType) throws IOException {
        if (!isValidPath(file, Utils.PathCheck.TESTS))
            throw new IllegalArgumentException("Invalid tests file: " + file.toString());
        List<BeAFixTest> tests = new LinkedList<>();
        String[] rawTests = Files.lines(file).collect(Collectors.joining("\n")).split(TEST_SEPARATOR);
        for (String rawTest : rawTests) {
            if (rawTest.trim().isEmpty())
                continue;
            BeAFixTest test = new BeAFixTest(rawTest, testType);
            tests.add(test);
        }
        return tests;
    }

    private static final String INVALID = "INVALID";
    private static final String VALID = "VALID";
    private static final String EXCEPTION = "EXCEPTION";
    private static BeAFixResult parseCheckFile(Path checkFile) {
        BeAFixResult beAFixResult = new BeAFixResult();
        try {
            List<String> checkLines = Files.lines(checkFile).collect(Collectors.toList());
            if (checkLines.isEmpty()) {
                return error("No lines found in check file: " + checkLines);
            }
            String firstLine = checkLines.get(0);
            if (firstLine.startsWith(VALID)) {
                beAFixResult.resultType = ResultType.CHECK;
                beAFixResult.check = true;
                beAFixResult.message("Valid model (" + checkFile.toString().replace(".verification", ".als") + ")");
            } else if (firstLine.startsWith(INVALID)) {
                beAFixResult.resultType = ResultType.CHECK;
                beAFixResult.check = false;
                if (firstLine.contains("(")) {
                    String repairedAndTotalProperties = firstLine.substring(firstLine.indexOf("("));
                    repairedAndTotalProperties = repairedAndTotalProperties.replace("(", "").replace(")", "");
                    String[] values = repairedAndTotalProperties.split("/");
                    int repaired = Integer.parseInt(values[0].trim());
                    int total = Integer.parseInt(values[1].trim());
                    beAFixResult.passingProperties = repaired;
                    beAFixResult.totalProperties = total;
                }
                beAFixResult.message(
                                "Invalid model (" + checkFile.toString().replace(".verification", ".als") + ")" +
                                " passing properties: " + beAFixResult.passingProperties + "/" + beAFixResult.totalProperties
                );
            } else if (firstLine.startsWith(EXCEPTION)) {
                beAFixResult.resultType = ResultType.ERROR;
                StringBuilder exceptionMsg = new StringBuilder();
                for (int i = 1; i < checkLines.size(); i++) {
                    exceptionMsg.append(checkLines.get(i)).append("\n");
                }
                beAFixResult.message("Error validating model (" + checkFile.toString().replace(".verification", ".als") +  ")\n" + exceptionMsg);
            } else {
                return error("Invalid validation file (" + checkFile + ")" + "\n" + String.join("\n", checkLines));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return error("Error while parsing check file (" + checkFile + ")" + "\n" + Utils.exceptionToString(e));
        }
        return beAFixResult;
    }

    @Override
    public String toString() {
        String rep = "{\n\t";
        switch (resultType) {
            case ERROR: {
                rep += "An error occurred!\n\tMessage: " + message + "\n}";
                break;
            }
            case TESTS: {
                String ceTests = testsToString(this.ceTests);
                String ttTests = testsToString(this.ttTests);
                String utTests = testsToString(this.utTests);
                rep += "Message: " + message;
                rep += "\n\tMax index for test batch: " + maxIndex;
                rep += "\n\tCounterexample tests:\n";
                rep += ceTests;
                rep += "\n\tTrusted tests:\n";
                rep += ttTests;
                rep += "\n\tUntrusted tests:\n";
                rep += utTests;
                rep += "}";
                break;
            }
            case CHECK: {
                rep += "CHECK " + (check?"SUCCEEDED":"FAILED") + "\n\tMessage: " + message + "\n}";
                break;
            }
        }
        return rep;
    }

    private String testsToString(List<BeAFixTest> from) {
        StringBuilder rep = new StringBuilder();
        if (from == null || from.isEmpty())
            return "NO TESTS\n";
        for (BeAFixTest ceTest : from) {
            rep.append(ceTest.toString()).append("\n");
        }
        return rep.toString();
    }

}
