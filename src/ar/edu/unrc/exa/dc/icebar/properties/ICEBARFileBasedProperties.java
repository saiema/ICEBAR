package ar.edu.unrc.exa.dc.icebar.properties;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import static ar.edu.unrc.exa.dc.icebar.properties.ICEBARPropertiesUtils.*;

public class ICEBARFileBasedProperties {
    
    /**
     * The path to a default .properties file
     */
    public static String ICEBAR_PROPERTIES = "icebar.properties";

    /**
     * The {@code ExperimentProperties} instance that will be returned by {@link ICEBARFileBasedProperties#getInstance()}
     */
    private static ICEBARFileBasedProperties instance = null;

    /**
     * @return a previously built instance or construct a new instance using {@code ExperimentProperties#DEFAULT_PROPERTIES}
     */
    public static ICEBARFileBasedProperties getInstance() {
        if (instance == null) {
            try {
                instance = new ICEBARFileBasedProperties();
            } catch (IOException e) {
                throw new IllegalStateException("Exception when trying to load properties");
            }
        }
        return instance;
    }

    private ICEBARFileBasedProperties() throws IOException {
        prop = new Properties();
        loadPropertiesFromFile();
    }

    private void loadPropertiesFromFile() {
        prop.clear();
        String cwd = System.getProperty("user.dir");
        File propFile;
        try {
            if (Paths.get(ICEBAR_PROPERTIES).isAbsolute()) {
                propFile = Paths.get(ICEBAR_PROPERTIES).toFile();
            } else {
                propFile = Paths.get(cwd, ICEBAR_PROPERTIES).toFile();
            }
            InputStream inputStream = Files.newInputStream(propFile.toPath());
            prop.load(inputStream);
            noPropertiesMode = false;
        } catch (IOException e) {
            noPropertiesMode = true;
        }
    }

    private final Properties prop;
    private boolean noPropertiesMode;

    boolean argumentExist(Property property) {
        if (noPropertiesMode) {
            return defaultValue(property).isPresent();
        }
        return prop.get(property.getKey()) != null;
    }

    String getValue(Property property) {
        if (noPropertiesMode) {
            return defaultValue(property).orElse(null);
        }
        return prop.getProperty(property.getKey());
    }
    
}
