package ar.edu.unrc.exa.dc.util;

import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

public final class Utils {

    public static String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return pw.toString();
    }

    public enum PathCheck {
        DIR, ALS, JAR, FILE, EXISTS
    }

    public static boolean isValidPath(Path pathToCheck, PathCheck check) {
        if (pathToCheck == null)
            return false;
        if (!pathToCheck.toFile().exists())
            return false;
        switch (check) {
            case DIR: return pathToCheck.toFile().isDirectory();
            case ALS: return pathToCheck.toFile().isFile() && pathToCheck.endsWith(".als");
            case JAR: return pathToCheck.toFile().isFile() && pathToCheck.endsWith(".jar");
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
            throw new Error("Couldn't create result file (" + (result.toString()) + ")");
        for (String aLine : Files.readAllLines(a)) {
            Files.write(result, aLine.getBytes(), StandardOpenOption.APPEND);
        }
        Files.write(result, "\n".getBytes(), StandardOpenOption.APPEND);
        for (String bLine : Files.readAllLines(b)) {
            Files.write(result, bLine.getBytes(), StandardOpenOption.APPEND);
        }
    }

    public static void generateTestsFile(Collection<BeAFixTest> tests, Path output) throws IOException {
        if (tests == null || tests.isEmpty())
            throw new IllegalArgumentException("null or empty tests");
        if (output == null || output.toFile().exists())
            throw new IllegalArgumentException("output path is either null or points to an existing file (" + (output==null?"NULL":output.toString()) + ")");
        File testsFile = output.toFile();
        if (!testsFile.createNewFile()) {
            throw new Error("Couldn't create tests file (" + (testsFile.toString()) + ")");
        }
        Files.write(output, "\n".getBytes(), StandardOpenOption.APPEND);
        for (BeAFixTest test : tests) {
            Files.write(output, ("--" + test.testType().toString() + "\n" + test.predicate() + "\n" + test.command() + "\n").getBytes(), StandardOpenOption.APPEND);
        }
    }

}
