package ar.edu.unrc.exa.dc.util;

import ar.edu.unrc.exa.dc.icebar.Report;
import ar.edu.unrc.exa.dc.search.FixCandidate;
import ar.edu.unrc.exa.dc.tools.ARepair;
import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Optional;

import static ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest.NO_SCOPE;

public final class Utils {

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
        if (pathToCheck == null)
            return false;
        return isValidPath(Paths.get(pathToCheck), check);
    }

    public static boolean isValidPath(Path pathToCheck, PathCheck check) {
        if (pathToCheck == null)
            return false;
        if (!pathToCheck.toFile().exists())
            return false;
        switch (check) {
            case DIR: return pathToCheck.toFile().isDirectory();
            case ALS: return pathToCheck.toFile().isFile() && pathToCheck.toString().endsWith(".als");
            case JAR: return pathToCheck.toFile().isFile() && pathToCheck.toString().endsWith(".jar");
            case PROPERTIES: return pathToCheck.toFile().isFile() && pathToCheck.toString().endsWith(".properties");
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
        int testCount = 0;
        if (tests == null || tests.isEmpty())
            throw new IllegalArgumentException("null or empty tests");
        if (output == null || output.toFile().exists())
            throw new IllegalArgumentException("output path is either null or points to an existing file (" + (output==null?"NULL":output.toString()) + ")");
        File testsFile = output.toFile();
        if (!testsFile.createNewFile()) {
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
                    "TRUSTED_GLOBAL" + Report.SEPARATOR +
                    "TRUSTED_LOCAL" + Report.SEPARATOR +
                    "UNTRUSTED_LOCAL" + Report.SEPARATOR +
                    "AREPAIR STATUS" + "\n"
            ;
    public static void startCandidateInfoFile() throws IOException {
        String candidateInfoFileRaw = "icebar_arepair.info";
        Path candidateInfoFilePath = Paths.get(candidateInfoFileRaw);
        File candidateInfoFile = candidateInfoFilePath.toFile();
        if (candidateInfoFile.exists() && !candidateInfoFile.delete())
            throw new Error("Candidate info file (" + candidateInfoFilePath + ") exists but couldn't be deleted");
        if (!candidateInfoFile.createNewFile()) {
            throw new Error("Couldn't create candidate info file (" + candidateInfoFilePath + ")");
        }
        Files.write(candidateInfoFilePath, CANDIDATE_REPORT_HEADER.getBytes(), StandardOpenOption.APPEND);
    }

    public static void writeCandidateInfo(FixCandidate candidate, Collection<BeAFixTest> trustedTests, ARepair.ARepairResult aRepairResult) throws IOException {
        String candidateInfoFileRaw = "cegar_arepair.info";
        Path candidateInfoFilePath = Paths.get(candidateInfoFileRaw);
        File candidateInfoFile = candidateInfoFilePath.toFile();
        if (!candidateInfoFile.exists())
            throw new Error("Candidate info file (" + candidateInfoFilePath + ") doesn't exists");
        String candidateInfo =
                        candidate.modelName() + Report.SEPARATOR +
                        candidate.depth() + Report.SEPARATOR +
                        countTests(trustedTests, BeAFixTest.TestType.COUNTEREXAMPLE) + Report.SEPARATOR +
                        countTests(trustedTests, BeAFixTest.TestType.TRUSTED) + Report.SEPARATOR +
                        countTests(candidate.trustedTests(), BeAFixTest.TestType.TRUSTED) + Report.SEPARATOR +
                        countTests(candidate.untrustedTests(), BeAFixTest.TestType.UNTRUSTED) + Report.SEPARATOR +
                        aRepairResult.name()  + "\n"
                ;
        Files.write(candidateInfoFilePath, candidateInfo.getBytes(), StandardOpenOption.APPEND);
    }

    private static int countTests(Collection<BeAFixTest> tests, BeAFixTest.TestType testType) {
        int count = 0;
        for (BeAFixTest test : tests) {
            if (test.testType().equals(testType))
                count++;
            else if (test.isRelated() && test.relatedBeAFixTest().testType().equals(testType))
                count++;
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

}
