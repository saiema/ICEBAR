package ar.edu.unrc.exa.dc.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

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
            throw new IllegalArgumentException("null path");
        if (!pathToCheck.toFile().exists())
            throw new IllegalArgumentException("path doesn't exist (" + pathToCheck.toString() + ")");
        switch (check) {
            case DIR: return pathToCheck.toFile().isDirectory();
            case ALS: return pathToCheck.toFile().isFile() && pathToCheck.endsWith(".als");
            case JAR: return pathToCheck.toFile().isFile() && pathToCheck.endsWith(".jar");
            case FILE: return pathToCheck.toFile().isFile();
            case EXISTS: return true;
        }
        return false;
    }

}
