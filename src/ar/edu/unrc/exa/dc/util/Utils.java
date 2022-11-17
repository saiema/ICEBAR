package ar.edu.unrc.exa.dc.util;

import ar.edu.unrc.exa.dc.icebar.ICEBARExperiment;
import ar.edu.unrc.exa.dc.icebar.Report;
import ar.edu.unrc.exa.dc.icebar.properties.ICEBARProperties;
import ar.edu.unrc.exa.dc.search.FixCandidate;
import ar.edu.unrc.exa.dc.tools.ARepairResult;
import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

import static ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest.NO_SCOPE;

public final class Utils {

    private static final String failedTestSuitesPrefix = "failed_test_suites";

    public static String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public enum PathCheck {
        DIR, ALS, JAR, FILE, EXISTS, PROPERTIES, TESTS
    }

    public static boolean isValidPath(String pathToCheck, PathCheck check) {
        return isValidPath(pathToCheck, check, true);
    }

    public static boolean isValidPath(Path pathToCheck, PathCheck check) {
        return isValidPath(pathToCheck, check, true);
    }

    public static boolean isValidPath(String pathToCheck, PathCheck check, boolean exist) {
        if (pathToCheck == null)
            return false;
        return isValidPath(Paths.get(pathToCheck), check, exist);
    }

    public static boolean isValidPath(Path pathToCheck, PathCheck check, boolean exist) {
        if (pathToCheck == null)
            return false;
        if (pathToCheck.toFile().exists() != exist)
            return false;
        switch (check) {
            case DIR: return pathToCheck.toFile().isDirectory() || !pathToCheck.toFile().isFile();
            case ALS: return pathToCheck.toFile().isFile() && pathToCheck.toString().endsWith(".als");
            case JAR: return pathToCheck.toFile().isFile() && pathToCheck.toString().endsWith(".jar");
            case PROPERTIES: return (pathToCheck.toFile().isFile() || !exist) && pathToCheck.toString().endsWith(".properties");
            case TESTS: return pathToCheck.toFile().isFile() && pathToCheck.toString().endsWith(".tests");
            case FILE: return pathToCheck.toFile().isFile();
            case EXISTS: return true;
        }
        return false;
    }

    public static void mergeFiles(Path a, Path b, Path result) throws IOException {
        if (!isValidPath(a, PathCheck.FILE))
            throw new IllegalArgumentException("first path is not valid (" + (a==null?"NULL":a.toString()) + ")");
        if (!isValidPath(a, PathCheck.FILE))
            throw new IllegalArgumentException("second path is not valid (" + (b==null?"NULL":b.toString()) + ")");
        if (result == null || result.toFile().exists())
            throw new IllegalArgumentException("result path is either null or points to an existing file (" + (result==null?"NULL":result.toString()) + ")");
        File rFile = result.toFile();
        if (!rFile.createNewFile())
            throw new Error("Couldn't create result file (" + (result) + ")");
        for (String aLine : Files.readAllLines(a)) {
            Files.write(result, (aLine + "\n").getBytes(), StandardOpenOption.APPEND);
        }
        Files.write(result, "\n".getBytes(), StandardOpenOption.APPEND);
        for (String bLine : Files.readAllLines(b)) {
            Files.write(result, (bLine + "\n").getBytes(), StandardOpenOption.APPEND);
        }
    }

    public static int generateTestsFile(Collection<BeAFixTest> tests, Path output) throws IOException {
        return writeTestsToFile(tests, output, true);
    }

    private static int testSuiteCount = 0;
    public static void saveFailingTestSuite(Collection<BeAFixTest> tests, String modelName) throws IOException {
        if (ICEBARProperties.getInstance().saveAllTestSuites()) {
            Path testsPath = Paths.get(
                    ICEBARExperiment.getInstance().failedTestSuitesFolderPath().toString(),
                    modelName + "_failing_test_suite_" + testSuiteCount++ + ".als"
            );
            writeTestsToFile(tests,testsPath,true);
        }
    }

    public static int writeTestsToFile(Collection<BeAFixTest> tests, Path output, boolean newFile) throws IOException {
        int testCount = 0;
        if (tests == null || tests.isEmpty())
            throw new IllegalArgumentException("null or empty tests");
        if (output == null)
            throw new IllegalArgumentException("output path is null");
        if (newFile && output.toFile().exists())
            throw new IllegalArgumentException("output path point to an existing file while newFile mode is used (" + output + ")");
        if (!newFile && !output.toFile().exists())
            throw new IllegalArgumentException("output path points to a non existing file while append mode (!newFile) is used (" + output + ")");
        File testsFile = output.toFile();
        if (newFile && !testsFile.createNewFile()) {
            throw new Error("Couldn't create tests file (" + (testsFile) + ")");
        }
        Files.write(output, "\n".getBytes(), StandardOpenOption.APPEND);
        for (BeAFixTest test : tests) {
            Files.write(output, ("--" + test.testType().toString() + "\n" + test.predicate() + "\n" + test.command() + "\n").getBytes(), StandardOpenOption.APPEND);
            testCount++;
            if (test.isRelated()) {
                Files.write(output,
                        ("--" + test.relatedBeAFixTest().testType().toString() + "\n" +
                                test.relatedBeAFixTest().predicate() + "\n" +
                                test.relatedBeAFixTest().command() + "\n")
                                .getBytes(),
                        StandardOpenOption.APPEND);
                testCount++;
            }
        }
        return testCount;
    }

    public static void writeTestsToLog(Collection<BeAFixTest> tests, Logger logger) {
        StringBuilder sb = new StringBuilder("\n");
        for (BeAFixTest test : tests) {
            sb.append("--").append(test.testType().toString()).append("\n").append(test.predicate()).append("\n").append(test.command()).append("\n");
            if (test.isRelated()) {
                sb.append("--").append(test.relatedBeAFixTest().testType().toString()).append("\n").append(test.relatedBeAFixTest().predicate()).append("\n").append(test.relatedBeAFixTest().command()).append("\n");
            }
        }
        logger.fine(sb.toString());
    }

    /**
     * Get text between two strings. Passed limiting strings are not
     * included into result.
     *
     * @param text     Text to search in.
     * @param textFrom Text to start cutting from (exclusive).
     * @param textTo   Text to stop cutting at (exclusive).
     */
    public static String getBetweenStrings(
            String text,
            String textFrom,
            String textTo) {

        String result;

        // Cut the beginning of the text to not occasionally meet a
        // 'textTo' value in it:
        result =
                text.substring(
                        text.indexOf(textFrom) + textFrom.length()
                );

        // Cut the excessive ending of the text:
        result =
                result.substring(
                        0,
                        result.indexOf(textTo));

        return result;
    }

    /**
     * Deletes Folder with all of its content
     *
     * @param folder path to folder which should be deleted
     */
    public static void deleteFolderAndItsContent(final Path folder) throws IOException {
        Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.isSymbolicLink(file))
                    Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                if (!Files.isSymbolicLink(dir))
                    Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void writeReport(Report report) throws IOException {
        String reportFileRaw = "icebar.info";
        Path reportFilePath = Paths.get(reportFileRaw);
        File reportFile = reportFilePath.toFile();
        if (reportFile.exists() && !reportFile.delete())
            throw new Error("Report file (" + reportFilePath + ") exists but couldn't be deleted");
        if (!reportFile.createNewFile()) {
            throw new Error("Couldn't create report file (" + reportFilePath + ")");
        }
        Files.write(reportFilePath, report.toString().getBytes(), StandardOpenOption.APPEND);
    }

    private static final String CANDIDATE_REPORT_HEADER =
                    "MODEL" + Report.SEPARATOR +
                    "DEPTH" + Report.SEPARATOR +
                    "CE_GLOBAL" + Report.SEPARATOR +
                    "CE_TRUSTED_LOCAL" + Report.SEPARATOR +
                    "CE_UNTRUSTED_LOCAL" + Report.SEPARATOR +
                    "PREDICATE_LOCAL" + Report.SEPARATOR +
                    "AREPAIR STATUS" + Report.SEPARATOR +
                    "TOTAL TRUSTED TESTS" + Report.SEPARATOR +
                    "TOTAL UNTRUSTED TESTS" + "\n"
            ;
    
    private static final String CANDIDATE_FILE = "icebar_arepair.info";
    
    public static void startCandidateInfoFile() throws IOException {
        Path candidateInfoFilePath = Paths.get(CANDIDATE_FILE);
        File candidateInfoFile = candidateInfoFilePath.toFile();
        if (candidateInfoFile.exists() && !candidateInfoFile.delete())
            throw new Error("Candidate info file (" + candidateInfoFilePath + ") exists but couldn't be deleted");
        if (!candidateInfoFile.createNewFile()) {
            throw new Error("Couldn't create candidate info file (" + candidateInfoFilePath + ")");
        }
        Files.write(candidateInfoFilePath, CANDIDATE_REPORT_HEADER.getBytes(), StandardOpenOption.APPEND);
    }

    public static void writeCandidateInfo(FixCandidate candidate, Collection<BeAFixTest> globalCounterexampleTests, ARepairResult aRepairResult) throws IOException {
        Path candidateInfoFilePath = Paths.get(CANDIDATE_FILE);
        File candidateInfoFile = candidateInfoFilePath.toFile();
        if (!candidateInfoFile.exists())
            throw new Error("Candidate info file (" + candidateInfoFilePath + ") doesn't exists");
        int globalCounterexampleTestsCount = countTests(globalCounterexampleTests, BeAFixTest.TestSource.COUNTEREXAMPLE);
        int localTrustedCounterexampleTestsCount = countTests(candidate.trustedTests(), BeAFixTest.TestSource.COUNTEREXAMPLE);
        int localUntrustedCounterexampleTestsCount = countTests(candidate.untrustedTests(), BeAFixTest.TestSource.COUNTEREXAMPLE);
        int localUntrustedPredicateTestsCount = countTests(candidate.untrustedTests(), BeAFixTest.TestSource.PREDICATE);
        int totalTrustedTests = globalCounterexampleTestsCount + localTrustedCounterexampleTestsCount;
        int totalUntrustedTests = localUntrustedPredicateTestsCount + localUntrustedCounterexampleTestsCount;
        String candidateInfo =
                        candidate.modelName() + Report.SEPARATOR +
                        candidate.depth() + Report.SEPARATOR +
                        globalCounterexampleTestsCount + Report.SEPARATOR +
                        localTrustedCounterexampleTestsCount + Report.SEPARATOR +
                        localUntrustedCounterexampleTestsCount + Report.SEPARATOR +
                        localUntrustedPredicateTestsCount + Report.SEPARATOR +
                        aRepairResult.name()  +
                        totalTrustedTests + Report.SEPARATOR +
                        totalUntrustedTests + "\n"
                ;
        Files.write(candidateInfoFilePath, candidateInfo.getBytes(), StandardOpenOption.APPEND);
    }

    private static int countTests(Collection<BeAFixTest> tests, BeAFixTest.TestSource testSource) {
        int count = 0;
        boolean countTest;
        for (BeAFixTest test : tests) {
            countTest = false;
            if (testSource.equals(BeAFixTest.TestSource.COUNTEREXAMPLE) && test.isCounterexampleTest())
                countTest = true;
            else if (testSource.equals(BeAFixTest.TestSource.PREDICATE) && test.isPredicateTest())
                countTest = true;
            if (countTest) {
                count++;
                if (test.isRelated()) {
                    count++;
                }
            }
        }
        return count;
    }

    public static int getMaxScopeFromAlsFile(Path alsFile) throws IOException {
        if (!isValidPath(alsFile, PathCheck.ALS))
            throw new IllegalArgumentException("Expecting an .als file but got " + alsFile.toString() + " instead");
        int maxScope = NO_SCOPE;
        for (String aLine : Files.readAllLines(alsFile)) {
            if (aLine.trim().startsWith("run "))
                maxScope = Math.max(maxScope, getMaxScopeFromCommandSegments(aLine.split(" ")));
        }
        return maxScope;
    }

    public static int getMaxScopeFromCommandSegments(String[] segments) {
        int currentScope = NO_SCOPE;
        int idx = 0;
        while(idx < segments.length && segments[idx].compareTo("for") != 0) idx++;
        if (idx < segments.length) {
            while (idx < segments.length && segments[idx].compareTo("expect") != 0) {
                String numberData = segments[idx].replaceAll("\\D+", "");
                if (!numberData.trim().isEmpty()) {
                    try {
                        int scope = Integer.parseInt(numberData);
                        currentScope = Math.max(currentScope, scope);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Error while parsing scope value from " + String.join(" ", segments) + "@" + idx + "\n" + Utils.exceptionToString(e));
                    }
                }
                idx++;
            }
        }
        return currentScope;
    }

    public static boolean findNullPointerExceptionInLog(Path log) throws IOException {
        if (!isValidPath(log, PathCheck.FILE))
            throw new IllegalArgumentException("Invalid log file " + (log==null?"NULL":log.toString()));
        for (String aLine : Files.readAllLines(log)) {
            if (aLine.trim().startsWith("Exception"))
                return aLine.contains("java.lang.NullPointerException");
        }
        return false;
    }

    public static Optional<String> findStringInFile(Path f, String target) throws IOException {
        if (!isValidPath(f, PathCheck.FILE))
            throw new IllegalArgumentException("Invalid file " + (f==null?"NULL":f.toString()));
        for (String aLine : Files.readAllLines(f)) {
            if (aLine.contains(target)) {
                return Optional.of(aLine.trim());
            }
        }
        return Optional.empty();
    }

    private static final String SYMBOLS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int NAME_LENGTH = 8;
    public static String generateRandomName() {
        StringBuilder sb = new StringBuilder();
        Random symbolRng = new Random();
        Random letterOrNumberCoin = new Random(); //TRUE is letter, FALSE is number
        Random upperOrLowerCaseCoin = new Random(); //TRUE is uppercase, FALSE is lowercase
        Random numberRng = new Random();
        for (int i = 0; i < NAME_LENGTH; i++) {
            if (letterOrNumberCoin.nextBoolean()) {
                //letter
                char symbol = SYMBOLS.charAt(symbolRng.nextInt(SYMBOLS.length()));
                if (upperOrLowerCaseCoin.nextBoolean()) {
                    //uppercase
                    sb.append(symbol);
                } else {
                    //lowercase
                    sb.append(Character.toLowerCase(symbol));
                }
            } else {
                //number
                sb.append(numberRng.nextInt(10));
            }
        }
        return sb.toString();
    }

    public static Path createFailedTestSuitesFolder(String modelName) throws IOException {
        String fullName = failedTestSuitesPrefix + "_" + modelName + "_" + generateRandomName() + "_" + simpleInstant();
        Path failedTestSuitesFolderPath = Paths.get(fullName);
        Files.createDirectories(failedTestSuitesFolderPath);
        return failedTestSuitesFolderPath;
    }

    private static String simpleInstant() {
        String rawInstant = Instant.now().toString();
        String noColons = rawInstant.replaceAll(":", "-");
        int finalDot = noColons.lastIndexOf(".");
        if (finalDot > 0) {
            return noColons.substring(0, finalDot);
        }
        return noColons;
    }

}
