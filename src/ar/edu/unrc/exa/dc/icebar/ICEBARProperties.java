package ar.edu.unrc.exa.dc.icebar;

import java.io.*;
import java.util.Properties;

public class ICEBARProperties {

    private static final String BEAFIX_PREFIX = "icebar.tools.beafix";
    private static final String AREPAIR_PREFIX = "icebar.tools.arepair";
    
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
        BEAFIX_NO_INSTANCE_TEST_FOR_NEGATIVE_TEST_WHEN_NO_FACTS {
            @Override
            public String getKey() { return BEAFIX_PREFIX + ".noinstancetestsfornegativetestwhennofacts"; }
        },
        AREPAIR_ROOT {
            @Override
            public String getKey() {
                return AREPAIR_PREFIX + ".root";
            }
        },
        AREPAIR_TREAT_PARTIAL_REPAIRS_AS_FIXES {
            @Override
            public String getKey() { return AREPAIR_PREFIX + ".partialrepairasfixes";}
        },
        ICEBAR_LAPS {
            @Override
            public String getKey() { return "icebar.laps"; }
        },
        ICEBAR_PRIORIZATION {
            @Override
            public String getKey() { return "icebar.priorization"; }
        },
        ICEBAR_SEARCH {
            @Override
            public String getKey() { return "icebar.search"; }
        },
        ICEBAR_ENABLE_RELAXEDFACTS_GENERATION {
            @Override
            public String getKey() { return "icebar.allowrelaxedfacts"; }
        },
        ICEBAR_ENABLE_FORCE_ASSERTION_TESTS {
            @Override
            public String getKey() { return "icebar.forceassertiontests"; }
        },
        ICEBAR_GLOBAL_TRUSTED_TESTS {
            @Override
            public String getKey() { return "icebar.globaltrustedtests"; }
        },
        ICEBAR_TIMEOUT {
            @Override
            public String getKey() { return "icebar.timeout"; }
        },
        ICEBAR_UPDATE_AREPAIR_SCOPE_FROM_ORACLE {
            @Override
            public String getKey() { return "icebar.updatescopefromoracle"; }
        },
        ICEBAR_KEEP_GOING_ON_AREPAIR_NPE {
            @Override
            public String getKey() { return "icebar.keepgoingarepairnpe"; }
        },
        ICEBAR_NO_FIX_ONLY_TRUSTED_KEEP_GOING {
            @Override
            public String getKey() { return "icebar.nofixonlytrustedkeepgoing"; }
        },
        ICEBAR_EMPTY_SEARCH_SPACE_BUT_MAYBE_MORE_TESTS_RETRY {
            @Override
            public String getKey() { return "icebar.search.emptysearchspace.retryformoretests"; }
        },
        ICEBAR_PRINT_PROCESS_GRAPH {
            @Override
            public String getKey() { return "icebar.search.printprocessgraph"; }
        },
        ICEBAR_PRINT_PROCESS_GRAPH_FOLDER {
            @Override
            public String getKey() { return "icebar.search.printprocessgraph.folder"; }
        },
        ICEBAR_PRINT_PROCESS_GRAPH_FOLDER_CLEAN {
            @Override
            public String getKey() { return "icebar.search.printprocessgraph.folder.clean"; }
        },
        ICEBAR_PRINT_PROCESS_GRAPH_STORE_TESTS {
            @Override
            public String getKey() { return "icebar.search.printprocessgraph.storetests"; }
        },
        ICEBAR_CHECK_REPEATED_TESTS {
            @Override
            public String getKey() { return "icebar.search.filterrepeatedtests"; }
        },
        ICEBAR_INITIAL_TESTS_POSITION {
            @Override
            public String getKey() { return "icebar.search.initialtests.position"; }
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
            case ICEBAR_GLOBAL_TRUSTED_TESTS:
            case ICEBAR_ENABLE_RELAXEDFACTS_GENERATION:
            case ICEBAR_PRIORIZATION:
            case ICEBAR_ENABLE_FORCE_ASSERTION_TESTS:
            case ICEBAR_UPDATE_AREPAIR_SCOPE_FROM_ORACLE:
            case ICEBAR_KEEP_GOING_ON_AREPAIR_NPE:
            case ICEBAR_NO_FIX_ONLY_TRUSTED_KEEP_GOING:
            case ICEBAR_EMPTY_SEARCH_SPACE_BUT_MAYBE_MORE_TESTS_RETRY:
            case BEAFIX_AREPAIR_COMPAT_RELAXED_MODE:
            case ICEBAR_PRINT_PROCESS_GRAPH:
            case ICEBAR_PRINT_PROCESS_GRAPH_FOLDER_CLEAN:
            case ICEBAR_CHECK_REPEATED_TESTS:
            case ICEBAR_PRINT_PROCESS_GRAPH_STORE_TESTS:
            case AREPAIR_TREAT_PARTIAL_REPAIRS_AS_FIXES:
            case BEAFIX_NO_INSTANCE_TEST_FOR_NEGATIVE_TEST_WHEN_NO_FACTS:
            case BEAFIX_INSTANCE_TESTS: return true;
            default: return false;
        }
    }

    private boolean isIntKey(ConfigKey key) {
        switch (key) {
            case BEAFIX_TESTS :
            case ICEBAR_TIMEOUT:
            case ICEBAR_LAPS: return true;
            default : return false;
        }
    }

    private boolean isStringKey(ConfigKey key) {
        switch (key) {
            case ICEBAR_SEARCH:
            case BEAFIX_JAR :
            case BEAFIX_MODEL_OVERRIDES_FOLDER :
            case BEAFIX_BUGGY_FUNCS_FILE :
            case ICEBAR_PRINT_PROCESS_GRAPH_FOLDER:
            case ICEBAR_INITIAL_TESTS_POSITION:
            case AREPAIR_ROOT : return true;
            default : return false;
        }
    }
    
}
