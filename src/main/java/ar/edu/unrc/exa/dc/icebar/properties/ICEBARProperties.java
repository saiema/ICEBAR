package ar.edu.unrc.exa.dc.icebar.properties;

import ar.edu.unrc.exa.dc.logging.LocalLogging;
import ar.edu.unrc.exa.dc.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ar.edu.unrc.exa.dc.icebar.properties.ICEBARPropertiesUtils.*;
import static ar.edu.unrc.exa.dc.icebar.properties.Property.*;

public class ICEBARProperties {

    private static final Logger logger = LocalLogging.getLogger(ICEBARProperties.class, IcebarLoggingLevel.INFO, IcebarLoggingLevel.OFF);

    private boolean useDefaultOnInvalidValue = false;

    private static ICEBARProperties instance;
    public static ICEBARProperties getInstance() {
        if (instance == null) {
            instance = new ICEBARProperties();
        }
        return instance;
    }

    private ICEBARProperties() {
        PropertyValue vmValue = getPropertyFromVMProperties(ICEBAR_USE_DEFAULT_ON_INVALID_VALUE);
        try {
            PropertyValue propertiesFileValue = getPropertyFromPropertiesFile(ICEBAR_USE_DEFAULT_ON_INVALID_VALUE, true);
            if (propertiesFileValue.hasValidValue()) {
                useDefaultOnInvalidValue = toBoolean(propertiesFileValue.value());
            }
            if (vmValue.hasValidValue()) {
                useDefaultOnInvalidValue = toBoolean(vmValue.value());
            }
        } catch (Exception e) {
            if (vmValue.hasValidValue()) {
                useDefaultOnInvalidValue = toBoolean(vmValue.value());
            }
            if (!useDefaultOnInvalidValue) {
                throwException(
                        ICEBAR_USE_DEFAULT_ON_INVALID_VALUE,
                        null,
                        "There was an exception getting the value from the properties file (" + ICEBARFileBasedProperties.ICEBAR_PROPERTIES + ") and the VM Arguments set this property to false.\n" +
                                Utils.exceptionToString(e)
                );
            }
        }
    }

    private Optional<String> getValidProperty(Property property) {
        PropertyValue propertiesFileValue = getPropertyFromPropertiesFile(property);
        PropertyValue vmPropertiesValue = getPropertyFromVMProperties(property);
        if (vmPropertiesValue.hasValidValue()) {
            return Optional.of(vmPropertiesValue.value());
        } else if (!vmPropertiesValue.error() && propertiesFileValue.hasValidValue()) {
            return Optional.of(propertiesFileValue.value());
        } else if (vmPropertiesValue.error()) {
            throwException(property, vmPropertiesValue.value(),"Got an invalid value from VM arguments");
        } else if (propertiesFileValue.error()) {
            throwException(property,propertiesFileValue.value(), "Got an invalid value from the .properties file and there is no related VM argument that overrides it");
        }
        return Optional.empty();
    }

    private String getProperty(Property property) {
        Optional<String> propertyValue = getValidProperty(property);
        if (propertyValue.isPresent()) {
            return propertyValue.get();
        } else {
            throwException(
                    property,
                    null,
                    "Couldn't get a valid value for property (" + property + ") and a value is needed (no default value is available). Check the information regarding the property, maybe the value is invalid."
            );
        }
        throw new IllegalStateException("Shouldn't have reached this point!");
    }

    private PropertyValue getPropertyFromPropertiesFile(Property property) {
        return getPropertyFromPropertiesFile(property, false);
    }

    private PropertyValue getPropertyFromPropertiesFile(Property property, boolean throwException) {
        try {
            String propertiesFileValue;
            if (ICEBARFileBasedProperties.getInstance().argumentExist(property)) {
                propertiesFileValue = ICEBARFileBasedProperties.getInstance().getValue(property);
                return valueToProperty(property, propertiesFileValue);
            }
            return PropertyValue.noValue();
        } catch (Exception e) {
            if (throwException) {
                throwException(property, null, "An exception occurred while trying to get value from properties file");
            }
            return PropertyValue.noValue();
        }
    }

    @NotNull
    private PropertyValue valueToProperty(Property property, String value) {
        if (isValidValue(property, value)) {
            return PropertyValue.validValue(value);
        } else if (useDefaultOnInvalidValue) {
            logger.warning("Value for property (" + property + ") is not valid, will be using the default value instead.\n" +
                    "Please check the description for the property to see if the value (" + value + ") is valid\n" +
                    property.getDescription());
            return defaultValue(property).map(PropertyValue::validValue).orElseGet(PropertyValue::noValue);
        } else {
            return PropertyValue.invalidValue(value);
        }
    }

    private PropertyValue getPropertyFromVMProperties(Property property) {
        if (doesVMPropertyExists(property)) {
            String vmValue = getVMPropertyValue(property);
            return valueToProperty(property, vmValue);
        }  /*
            VM properties only overwrite properties from file if one of the following situations happen:
            1. The properties file does not define a value for the property
            2. The VM properties has a -D<property>=value
             */
        return PropertyValue.noValue();
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

    public boolean icebarOpenAI() {
        return toBoolean(getProperty(ICEBAR_OPENAI_ENABLE));
    }

    public Path icebarOpenAIEnvFile() {
        return toPath(getProperty(ICEBAR_OPENAI_CONFIG_FILE));
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

    public List<String> getAllRawProperties() {
        return Arrays.stream(values()).map(p ->
            p.getKey() + "=" + getRawProperty(p).orElse("UNSET")
        ).collect(Collectors.toList());
    }

    private Optional<String> getRawProperty(Property property) {
        String propertyFileValue = ICEBARFileBasedProperties.getInstance().getValue(property);
        String vmPropertyValue = getVMPropertyValue(property);
        if (isValidValue(property, vmPropertyValue)) {
            return vmPropertyValue == null?Optional.of(""):Optional.of(vmPropertyValue);
        } else if (isValidValue(property, propertyFileValue)) {
            return propertyFileValue==null?Optional.of(""):Optional.of(propertyFileValue);
        }
        return defaultValue(property);
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
