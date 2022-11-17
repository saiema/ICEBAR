package ar.edu.unrc.exa.dc.icebar.properties;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

class ICEBARPropertiesUtils {

    static Optional<String> defaultValue(Property property) {
        switch (property) {
            case ICEBAR_USE_DEFAULT_ON_INVALID_VALUE :
            case BEAFIX_INSTANCE_TESTS :
            case BEAFIX_NO_INSTANCE_TEST_FOR_NEGATIVE_TEST_WHEN_NO_FACTS :
            case AREPAIR_TREAT_PARTIAL_REPAIRS_AS_FIXES :
            case ICEBAR_ENABLE_RELAXEDFACTS_GENERATION :
            case ICEBAR_ENABLE_FORCE_ASSERTION_TESTS :
            case ICEBAR_UPDATE_AREPAIR_SCOPE_FROM_ORACLE :
            case ICEBAR_KEEP_GOING_ON_AREPAIR_NPE :
                return Optional.of(Boolean.toString(true));
            case BEAFIX_JAR :
            case AREPAIR_ROOT :
                return Optional.empty();
            case BEAFIX_TESTS : return Optional.of(Integer.toString(1));
            case BEAFIX_MODEL_OVERRIDES_FOLDER : return Optional.of("modelOverrides");
            case ICEBAR_LAPS : return Optional.of(Integer.toString(20));
            case ICEBAR_PRIORIZATION :
            case ICEBAR_GLOBAL_TRUSTED_TESTS :
            case ICEBAR_SAVE_ALL_TEST_SUITES:
                return Optional.of(Boolean.toString(false));
            case ICEBAR_SEARCH : return Optional.of(ICEBARProperties.IcebarSearchAlgorithm.BFS.toString());
            case ICEBAR_TIMEOUT : return Optional.of(Integer.toString(60));
            case ICEBAR_LOGGING_FILE_VERBOSITY: return Optional.of(ICEBARProperties.IcebarLoggingLevel.FINE.toString());
            case ICEBAR_LOGGING_CONSOLE_VERBOSITY: return Optional.of(ICEBARProperties.IcebarLoggingLevel.INFO.toString());
        }
        return Optional.empty();
    }

    static boolean isValidValue(Property property, String value) {
        switch (property) {
            case ICEBAR_USE_DEFAULT_ON_INVALID_VALUE :
            case BEAFIX_INSTANCE_TESTS :
            case BEAFIX_NO_INSTANCE_TEST_FOR_NEGATIVE_TEST_WHEN_NO_FACTS :
            case AREPAIR_TREAT_PARTIAL_REPAIRS_AS_FIXES :
            case ICEBAR_PRIORIZATION :
            case ICEBAR_ENABLE_RELAXEDFACTS_GENERATION :
            case ICEBAR_ENABLE_FORCE_ASSERTION_TESTS :
            case ICEBAR_GLOBAL_TRUSTED_TESTS :
            case ICEBAR_UPDATE_AREPAIR_SCOPE_FROM_ORACLE :
            case ICEBAR_KEEP_GOING_ON_AREPAIR_NPE :
            case ICEBAR_SAVE_ALL_TEST_SUITES:
                return isBoolean(value);
            case BEAFIX_JAR : return isPath(value, PathType.JAR, false);
            case BEAFIX_TESTS : return isNumber(value, false);
            case BEAFIX_MODEL_OVERRIDES_FOLDER : return value == null || value.isEmpty() || isPath(value, PathType.FOLDER, false);
            case AREPAIR_ROOT : return isPath(value, PathType.FOLDER,true);
            case ICEBAR_LAPS :
            case ICEBAR_TIMEOUT :
                return isNumber(value,true);
            case ICEBAR_SEARCH : return isValidSearchAlgorithm(value);
            case ICEBAR_LOGGING_FILE_VERBOSITY:
            case ICEBAR_LOGGING_CONSOLE_VERBOSITY: {
                return "NONE".compareToIgnoreCase(value) == 0
                || "SIMPLE".compareToIgnoreCase(value) == 0
                || "FULL".compareToIgnoreCase(value) == 0;
            }
        }
        throw new IllegalArgumentException("Invalid or unsupported property (" + property + ")");
    }

    private static boolean isNumber(String stringRep, boolean acceptZero) {
        if (stringRep == null) {
            return false;
        }
        try {
            int num = Integer.parseInt(stringRep);
            return acceptZero?num >= 0:num > 0;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private static boolean isBoolean(String stringRep) {
        if (stringRep == null) {
            return false;
        }
        return "true".compareToIgnoreCase(stringRep) == 0 || "false".compareToIgnoreCase(stringRep) == 0;
    }

    private static boolean isValidSearchAlgorithm(String stringRep) {
        if (stringRep == null) {
            return false;
        }
        return "DFS".compareToIgnoreCase(stringRep) == 0 || "BFS".compareToIgnoreCase(stringRep) == 0;
    }

    private enum PathType {JAR, FOLDER}
    private static boolean isPath(String stringRep, PathType pathType, boolean absolute) {
        if (stringRep == null) {
            return false;
        }
        File file;
        try {
            file = Paths.get(stringRep).toFile();
        } catch (InvalidPathException ive) {
            return false;
        }
        if (absolute && !file.isAbsolute()) {
            return false;
        }
        if (pathType.equals(PathType.JAR)) {
            return file.isFile() && file.getName().endsWith(".jar");
        }
        if (pathType.equals(PathType.FOLDER)) {
            return file.isDirectory();
        }
        return false;
    }

    static boolean toBoolean(String value) {
        return Boolean.parseBoolean(value);
    }

    static int toNumber(String value) {
        return Integer.parseInt(value);
    }

    static Path toPath(String value) {
        return Paths.get(value);
    }

    @Contract("_, _ -> fail")
    static void throwException(Property key, String value) {
        throw new IllegalArgumentException("Invalid value " + value + " for property " + key.getKey());
    }

}
