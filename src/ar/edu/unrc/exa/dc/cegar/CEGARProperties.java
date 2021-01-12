package ar.edu.unrc.exa.dc.cegar;

import java.io.*;
import java.util.Properties;

public class CEGARProperties {

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
            public String getKey() {
                return "cegar.laps";
            }
        }
        ;
        public abstract String getKey();
    }

    /**
     * The path to a default .properties file
     */
    private static final String DEFAULT_PROPERTIES = "cegar.properties";

    /**
     * The {@code ExperimentProperties} instance that will be returned by {@link CEGARProperties#getInstance()}
     */
    private static CEGARProperties instance = null;

    /**
     * @return a previously built instance or construct a new instance using {@code ExperimentProperties#DEFAULT_PROPERTIES}
     */
    public static CEGARProperties getInstance() {
        if (instance == null) {
            try {
                instance = new CEGARProperties();
            } catch (IOException e) {
                throw new IllegalStateException("Exception when trying to load properties");
            }
        }
        return instance;
    }

    private CEGARProperties() throws IOException {
        prop = new Properties();
        loadPropertiesFromFile();
    }

    private void loadPropertiesFromFile() throws IOException {
        String cwd = System.getProperty("user.dir");
        String configFile = cwd + File.separator + DEFAULT_PROPERTIES;
        File propFile = createConfigFileIfMissing(configFile);
        InputStream inputStream = new FileInputStream(propFile);
        prop.load(inputStream);
    }

    private Properties prop;

    private File createConfigFileIfMissing(String configFile) throws IOException {
        File propFile = new File(configFile);
        if (!propFile.exists())
            if (!propFile.createNewFile())
                throw new IllegalStateException("Couldn't create new file " + configFile);
        return propFile;
    }

    public void loadConfig() throws IOException {
        loadPropertiesFromFile();
    }

    public boolean argumentExist(ConfigKey key) {
        return prop.get(key.getKey()) != null;
    }

    public boolean getBooleanArgument(ConfigKey key) {
        if (!isBooleanKey(key))
            throw new IllegalStateException("Config key is not boolean " + key.toString());
        if (!isDefined(key))
            return false;
        String propValue = prop.getProperty(key.getKey());
        if (propValue == null)
            return false;
        return Boolean.parseBoolean(propValue);
    }

    public int getIntArgument(ConfigKey key) {
        if (!isIntKey(key))
            throw new IllegalStateException("Config key is not int " + key.toString());
        if (!isDefined(key))
            return 0;
        String propValue = prop.getProperty(key.getKey());
        if (propValue == null)
            return 0;
        return Integer.parseInt(propValue);
    }

    public String getStringArgument(ConfigKey key) {
        if (!isStringKey(key))
            throw new IllegalStateException("Config key is not String " + key.toString());
        if (!isDefined(key))
            return "";
        return prop.getProperty(key.getKey(), "");
    }

    private boolean isDefined(ConfigKey key) {
        return prop.containsKey(key.getKey());
    }


    private boolean isBooleanKey(ConfigKey key) {
        return key == ConfigKey.BEAFIX_INSTANCE_TESTS;
    }

    private boolean isIntKey(ConfigKey key) {
        switch (key) {
            case BEAFIX_TESTS :
            case CEGAR_LAPS : return true;
            default : return false;
        }
    }

    private boolean isStringKey(ConfigKey key) {
        switch (key) {
            case BEAFIX_JAR :
            case BEAFIX_MODEL_OVERRIDES_FOLDER :
            case BEAFIX_BUGGY_FUNCS_FILE :
            case AREPAIR_ROOT : return true;
            default : return false;
        }
    }
    
}
