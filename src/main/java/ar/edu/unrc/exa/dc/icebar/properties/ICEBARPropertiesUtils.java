package ar.edu.unrc.exa.dc.icebar.properties;

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
            case ICEBAR_OPENAI_ENABLE:
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
            case ICEBAR_OPENAI_CONFIG_FILE: return Optional.of("config.env");
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
            case ICEBAR_OPENAI_ENABLE:
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
                return value != null && (ICEBARProperties.IcebarLoggingLevel.OFF.toString().compareToIgnoreCase(value) == 0
                || ICEBARProperties.IcebarLoggingLevel.INFO.toString().compareToIgnoreCase(value) == 0
                || ICEBARProperties.IcebarLoggingLevel.FINE.toString().compareToIgnoreCase(value) == 0);
            }
            case ICEBAR_OPENAI_CONFIG_FILE: return value == null || value.isEmpty() || isPath(value, PathType.ENV, false);
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
        return Boolean.TRUE.toString().compareToIgnoreCase(stringRep) == 0 || Boolean.FALSE.toString().compareToIgnoreCase(stringRep) == 0;
    }

    private static boolean isValidSearchAlgorithm(String stringRep) {
        if (stringRep == null) {
            return false;
        }
        return ICEBARProperties.IcebarSearchAlgorithm.DFS.toString().compareToIgnoreCase(stringRep) == 0 || ICEBARProperties.IcebarSearchAlgorithm.BFS.toString().compareToIgnoreCase(stringRep) == 0;
    }

    private enum PathType {JAR, FOLDER, ENV}
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
        if (pathType.equals(PathType.ENV)) {
            return file.isFile() && file.getName().endsWith(".env");
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

    static void throwException(Property key, String value, String message) {
        throw new IllegalArgumentException(message + " (value: " + value + ") (property: " + key.getKey() + ")");
    }

}
