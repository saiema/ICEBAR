package ar.edu.unrc.exa.dc.icebar;

import java.io.*;
import java.util.Properties;

public class ICEBARProperties {

    private static final String BEAFIX_PREFIX = "cegar.tools.beafix";
    private static final String AREPAIR_PREFIX = "cegar.tools.arepair";
    
    public enum ConfigKey {
        BEAFIX_JAR {
            @Override
            public String getKey() {
                return BEAFIX_PREFIX + ".jar";
            }
        },
        BEAFIX_TESTS {
            @Override
            public String getKey() {
                return BEAFIX_PREFIX + ".tests";
            }
        },
        BEAFIX_AREPAIR_COMPAT_RELAXED_MODE {
            @Override
            public String getKey() { return BEAFIX_PREFIX + ".compat.relaxed"; }
        },
        BEAFIX_MODEL_OVERRIDES_FOLDER {
            @Override
            public String getKey() {
                return BEAFIX_PREFIX + ".modeloverridesfolder";
            }
        },
        BEAFIX_INSTANCE_TESTS {
            @Override
            public String getKey() {
                return BEAFIX_PREFIX + ".instancetests";
            }
        },
        BEAFIX_BUGGY_FUNCS_FILE {
            @Override
            public String getKey() {
                return BEAFIX_PREFIX + ".buggyfuncsfile";
            }
        },
        AREPAIR_ROOT {
            @Override
            public String getKey() {
                return AREPAIR_PREFIX + ".root";
            }
        },
        CEGAR_LAPS {
            @Override
            public String getKey() { return "cegar.laps"; }
        },
        CEGAR_PRIORIZATION {
            @Override
            public String getKey() { return "cegar.priorization"; }
        },
        CEGAR_SEARCH {
            @Override
            public String getKey() { return "cegar.search"; }
        },
        CEGAR_ENABLE_RELAXEDFACTS_GENERATION {
            @Override
            public String getKey() { return "cegar.allowrelaxedfacts"; }
        },
        CEGAR_ENABLE_FORCE_ASSERTION_TESTS {
            @Override
            public String getKey() { return "cegar.forceassertiontests"; }
        },
        CEGAR_GLOBAL_TRUSTED_TESTS {
            @Override
            public String getKey() { return "cegar.globaltrustedtests"; }
        },
        CEGAR_TIMEOUT {
            @Override
            public String getKey() { return "cegar.timeout"; }
        },
        CEGAR_UPDATE_AREPAIR_SCOPE_FROM_ORACLE {
            @Override
            public String getKey() { return "cegar.updatescopefromoracle"; }
        },
        CEGAR_KEEP_GOING_ON_AREPAIR_NPE {
            @Override
            public String getKey() { return "cegar.keepgoingarepairnpe"; }
        }
        ;
        public abstract String getKey();
    }

    /**
     * The path to a default .properties file
     */
    public static final String DEFAULT_PROPERTIES = "icebar.properties";

    /**
     * The {@code ExperimentProperties} instance that will be returned by {@link ICEBARProperties#getInstance()}
     */
    private static ICEBARProperties instance = null;

    /**
     * @return a previously built instance or construct a new instance using {@code ExperimentProperties#DEFAULT_PROPERTIES}
     */
    public static ICEBARProperties getInstance() {
        if (instance == null) {
            try {
                instance = new ICEBARProperties();
            } catch (IOException e) {
                throw new IllegalStateException("Exception when trying to load properties");
            }
        }
        return instance;
    }

    private ICEBARProperties() throws IOException {
        prop = new Properties();
        loadPropertiesFromFile(null);
    }

    private void loadPropertiesFromFile(String fromFile) throws IOException {
        prop.clear();
        String cwd = System.getProperty("user.dir");
        String configFile = fromFile!=null?fromFile:(cwd + File.separator + DEFAULT_PROPERTIES);
        File propFile = createConfigFileIfMissing(configFile);
        InputStream inputStream = new FileInputStream(propFile);
        prop.load(inputStream);
    }

    private final Properties prop;

    private File createConfigFileIfMissing(String configFile) throws IOException {
        File propFile = new File(configFile);
        if (!propFile.exists())
            if (!propFile.createNewFile())
                throw new IllegalStateException("Couldn't create new file " + configFile);
        return propFile;
    }

    public void loadConfig(String configFile) throws IOException {
        loadPropertiesFromFile(configFile);
    }

    public boolean argumentExist(ConfigKey key) {
        return prop.get(key.getKey()) != null;
    }

    public boolean getBooleanArgument(ConfigKey key) {
        if (!isBooleanKey(key))
            throw new IllegalStateException("Config key is not boolean " + key);
        if (isUndefined(key))
            return false;
        String propValue = prop.getProperty(key.getKey());
        if (propValue == null)
            return false;
        return Boolean.parseBoolean(propValue);
    }

    public int getIntArgument(ConfigKey key) {
        if (!isIntKey(key))
            throw new IllegalStateException("Config key is not int " + key);
        if (isUndefined(key))
            return 0;
        String propValue = prop.getProperty(key.getKey());
        if (propValue == null)
            return 0;
        return Integer.parseInt(propValue);
    }

    public String getStringArgument(ConfigKey key) {
        if (!isStringKey(key))
            throw new IllegalStateException("Config key is not String " + key);
        if (isUndefined(key))
            return "";
        return prop.getProperty(key.getKey(), "");
    }

    private boolean isUndefined(ConfigKey key) {
        return !prop.containsKey(key.getKey());
    }


    private boolean isBooleanKey(ConfigKey key) {
        switch (key) {
            case CEGAR_GLOBAL_TRUSTED_TESTS:
            case CEGAR_ENABLE_RELAXEDFACTS_GENERATION:
            case CEGAR_PRIORIZATION:
            case CEGAR_ENABLE_FORCE_ASSERTION_TESTS:
            case CEGAR_UPDATE_AREPAIR_SCOPE_FROM_ORACLE:
            case CEGAR_KEEP_GOING_ON_AREPAIR_NPE:
            case BEAFIX_AREPAIR_COMPAT_RELAXED_MODE:
            case BEAFIX_INSTANCE_TESTS: return true;
            default: return false;
        }
    }

    private boolean isIntKey(ConfigKey key) {
        switch (key) {
            case BEAFIX_TESTS :
            case CEGAR_TIMEOUT:
            case CEGAR_LAPS : return true;
            default : return false;
        }
    }

    private boolean isStringKey(ConfigKey key) {
        switch (key) {
            case CEGAR_SEARCH:
            case BEAFIX_JAR :
            case BEAFIX_MODEL_OVERRIDES_FOLDER :
            case BEAFIX_BUGGY_FUNCS_FILE :
            case AREPAIR_ROOT : return true;
            default : return false;
        }
    }
    
}
