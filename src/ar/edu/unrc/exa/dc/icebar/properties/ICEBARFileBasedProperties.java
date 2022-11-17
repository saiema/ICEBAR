package ar.edu.unrc.exa.dc.icebar.properties;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

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

    private void loadPropertiesFromFile() throws IOException {
        prop.clear();
        String cwd = System.getProperty("user.dir");
        File propFile = createConfigFileIfMissing(Paths.get(cwd, ICEBAR_PROPERTIES).toString());
        InputStream inputStream = Files.newInputStream(propFile.toPath());
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

    boolean argumentExist(Property property) {
        return prop.get(property.getKey()) != null;
    }

    String getValue(Property property) {
        return prop.getProperty(property.getKey());
    }
    
}
