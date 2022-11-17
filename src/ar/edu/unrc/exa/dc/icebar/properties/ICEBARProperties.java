package ar.edu.unrc.exa.dc.icebar.properties;

import ar.edu.unrc.exa.dc.util.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static ar.edu.unrc.exa.dc.icebar.properties.ICEBARPropertiesUtils.*;
import static ar.edu.unrc.exa.dc.icebar.properties.Property.*;

public class ICEBARProperties {

    private boolean useDefaultOnInvalidValue = false;

    private static ICEBARProperties instance;
    public static ICEBARProperties getInstance() {
        if (instance == null) {
            instance = new ICEBARProperties();
        }
        return instance;
    }

    private ICEBARProperties() {
        Optional<String> vmValue = getPropertyFromVMProperties(ICEBAR_USE_DEFAULT_ON_INVALID_VALUE);
        try {
            Optional<String> propertiesFileValue = getPropertyFromPropertiesFile(ICEBAR_USE_DEFAULT_ON_INVALID_VALUE, true);
            propertiesFileValue.ifPresent(s -> useDefaultOnInvalidValue = toBoolean(s));
            vmValue.ifPresent(s -> useDefaultOnInvalidValue = toBoolean(s));
        } catch (Exception e) {
            vmValue.ifPresent(s -> useDefaultOnInvalidValue = toBoolean(s));
            if (!useDefaultOnInvalidValue) {
                throwException(
                        ICEBAR_USE_DEFAULT_ON_INVALID_VALUE,
                        "There was an exception getting the value from the properties file (" + ICEBARFileBasedProperties.ICEBAR_PROPERTIES + ") and the VM Arguments set this property to false.\n" +
                                Utils.exceptionToString(e)
                );
            }
        }
    }

    private Optional<String> getRawProperty(Property property) {
        Optional<String> propertiesFileValue = getPropertyFromPropertiesFile(property);
        Optional<String> vmPropertiesValue = getPropertyFromVMProperties(property);
        if (vmPropertiesValue.isPresent()) {
            return vmPropertiesValue;
        } else return propertiesFileValue;
    }

    private String getProperty(Property property) {
        Optional<String> propertyValue = getRawProperty(property);
        if (propertyValue.isPresent()) {
            return propertyValue.get();
        } else {
            throwException(
                    property,
                    "Couldn't get a value for property (" + property + ") and a value is needed (no default value is available)."
            );
        }
        throw new IllegalStateException("Shouldn't have reached this point!");
    }

    private Optional<String> getPropertyFromPropertiesFile(Property property) {
        return getPropertyFromPropertiesFile(property, false);
    }

    private Optional<String> getPropertyFromPropertiesFile(Property property, boolean throwException) {
        try {
            String propertiesFileValue;
            if (ICEBARFileBasedProperties.getInstance().argumentExist(property)) {
                propertiesFileValue = ICEBARFileBasedProperties.getInstance().getValue(property);
                if (isValidValue(property, propertiesFileValue)) {
                    return Optional.of(propertiesFileValue);
                } else if (useDefaultOnInvalidValue) {
                    return defaultValue(property);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            if (throwException) {
                throw e;
            }
            return Optional.empty();
        }
    }

    private Optional<String> getPropertyFromVMProperties(Property property) {
        if (doesVMPropertyExists(property)) {
            String vmValue = getVMPropertyValue(property);
            if (isValidValue(property, vmValue)) {
                return Optional.of(vmValue);
            } else if (useDefaultOnInvalidValue) {
                return defaultValue(property);
            }
            return Optional.empty();
        } else {
            return defaultValue(property);
        }
    }

    public boolean useDefaultOnInvalidValue() {
        return useDefaultOnInvalidValue;
    }

    public boolean enableBeAFixInstanceTestsGeneration() {
        return toBoolean(getProperty(BEAFIX_INSTANCE_TESTS));
    }

    public boolean noInstanceTestForNegativeBranchWhenNoFacts() {
        return toBoolean(getProperty(BEAFIX_NO_INSTANCE_TEST_FOR_NEGATIVE_TEST_WHEN_NO_FACTS));
    }

    public boolean arepairTreatPartialRepairsAsFixes() {
        return toBoolean(getProperty(AREPAIR_TREAT_PARTIAL_REPAIRS_AS_FIXES));
    }

    public boolean enableCandidatePrioritization() {
        return toBoolean(getProperty(ICEBAR_PRIORIZATION));
    }

    public boolean enableRelaxedFactsTestGeneration() {
        return toBoolean(getProperty(ICEBAR_ENABLE_RELAXEDFACTS_GENERATION));
    }

    public boolean forceAssertionTestGeneration() {
        return toBoolean(getProperty(ICEBAR_ENABLE_FORCE_ASSERTION_TESTS));
    }

    public boolean globalTrustedTests() {
        return toBoolean(getProperty(ICEBAR_GLOBAL_TRUSTED_TESTS));
    }

    public boolean updateARepairScopeFromOracle() {
        return toBoolean(getProperty(ICEBAR_UPDATE_AREPAIR_SCOPE_FROM_ORACLE));
    }

    public boolean keepGoingOnARepairNPE() {
        return toBoolean(getProperty(ICEBAR_KEEP_GOING_ON_AREPAIR_NPE));
    }

    public boolean saveAllTestSuites() {
        return toBoolean(getProperty(ICEBAR_SAVE_ALL_TEST_SUITES));
    }

    public Path beafixJar() {
        return toPath(getProperty(BEAFIX_JAR));
    }

    public int testsToGenerateUpperBound() {
        return toNumber(getProperty(BEAFIX_TESTS));
    }

    @Nullable
    public Path beafixModelOverridesFolder() {
        String pathValue = getProperty(BEAFIX_MODEL_OVERRIDES_FOLDER);
        if (pathValue.trim().isEmpty()) {
            return null;
        }
        return toPath(pathValue);
    }

    public Path arepairRootFolder() {
        return toPath(getProperty(AREPAIR_ROOT));
    }

    public int icebarLaps() {
        return toNumber(getProperty(ICEBAR_LAPS));
    }

    public int icebarTimeout() {
        return toNumber(getProperty(ICEBAR_TIMEOUT));
    }

    public enum IcebarSearchAlgorithm {BFS, DFS}
    public IcebarSearchAlgorithm icebarSearchAlgorithm() {
        return IcebarSearchAlgorithm.valueOf(getProperty(ICEBAR_SEARCH).toUpperCase());
    }

    public enum IcebarLoggingLevel {
        OFF {
            @Override
            public Level getLoggerLevel() {
                return Level.OFF;
            }
        },
        INFO {
            @Override
            public Level getLoggerLevel() {
                return Level.INFO;
            }
        },
        FINE {
            @Override
            public Level getLoggerLevel() {
                return Level.FINE;
            }
        }
        ;
        public abstract Level getLoggerLevel();
        }
    public IcebarLoggingLevel icebarConsoleLoggingLevel() {
        return IcebarLoggingLevel.valueOf(getProperty(ICEBAR_LOGGING_CONSOLE_VERBOSITY).toUpperCase());
    }

    public IcebarLoggingLevel icebarFileLoggingLevel() {
        return IcebarLoggingLevel.valueOf(getProperty(ICEBAR_LOGGING_FILE_VERBOSITY).toUpperCase());
    }

    public List<String> getAllRawPropertyValues() {
        return Arrays.stream(values()).map(this::getRawProperty).map(v -> v.orElse("UNSET")).collect(Collectors.toList());
    }

    public static Map<String, String> getOptionsAndDescriptions() {
        Map<String, String> optionsAndDescriptions = new TreeMap<>();
        for (Property property : Property.values()) {
            optionsAndDescriptions.put(property.getKey(), property.getDescription());
        }
        return optionsAndDescriptions;
    }

    public static Path generateTemplatePropertiesFile(String path) throws IOException {
        if (Utils.isValidPath(path, Utils.PathCheck.PROPERTIES, false)) {
            Path newPropertiesFile = Paths.get(path);
            StringBuilder sb = new StringBuilder();
            sb.append("#AUTOMATICALLY GENERATED PROPERTIES\n");
            sb.append("#IMPORTANT: Properties that do not have a default value (need to be manually set) will have 'UNSET' as value\n");
            sb.append("\n\n");
            sb.append("#ICEBAR can be configured both by a .properties file having lines with <key>=<value> or by using `-D<key>=<value>` command line arguments.\n");
            sb.append("#The command line arguments will always override the configurations defined in the .properties file, available options are:\n\n");
            for (Property property : Property.values()) {
                Arrays.stream(property.getDescription().split("\n")).forEach(line -> sb.append("#").append(line).append("\n"));
                Optional<String> defaultValue = defaultValue(property);
                if (defaultValue.isPresent()) {
                    sb.append(property.getKey()).append("=").append(defaultValue.get());
                } else {
                    sb.append(property.getKey()).append("=").append("UNSET");
                }
                sb.append("\n\n");
            }
            Files.write(newPropertiesFile,sb.toString().getBytes(), StandardOpenOption.CREATE_NEW);
            return newPropertiesFile;
        }
        return null;
    }

    private static boolean doesVMPropertyExists(Property key) {
        return System.getProperty(key.getKey()) != null;
    }

    private static String getVMPropertyValue(Property key) {
        return System.getProperty(key.getKey());
    }

}
