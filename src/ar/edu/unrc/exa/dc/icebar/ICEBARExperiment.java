package ar.edu.unrc.exa.dc.icebar;

import ar.edu.unrc.exa.dc.icebar.properties.ICEBARProperties;
import ar.edu.unrc.exa.dc.util.Utils;

import java.io.IOException;
import java.nio.file.Path;

public final class ICEBARExperiment {

    private Path modelPath;
    private Path oraclePath;
    private Path propertiesPath;
    private Path initialTestsPath;
    private Path failedTestSuitesFolderPath;

    private static ICEBARExperiment instance;

    public static ICEBARExperiment getInstance() {
        if (instance == null)
            instance = new ICEBARExperiment();
        return instance;
    }

    private ICEBARExperiment() {}

    public void modelPath(Path modelPath) throws IOException {
        if (!Utils.isValidPath(modelPath, Utils.PathCheck.ALS))
            throw new IllegalArgumentException("invalid model path (" + modelPath + ")");
        this.modelPath = modelPath;
        if (ICEBARProperties.getInstance().saveAllTestSuites()) {
            String modelFileName = modelPath.getFileName().toString();
            String modelName = modelFileName;
            int lastDot = modelFileName.lastIndexOf(".");
            if (lastDot > 0) {
                modelName = modelFileName.substring(0, lastDot);
            }
            failedTestSuitesFolderPath = Utils.createFailedTestSuitesFolder(modelName);
        }
    }

    public void oraclePath(Path oraclePath) {
        if (!Utils.isValidPath(oraclePath, Utils.PathCheck.ALS))
            throw new IllegalArgumentException("invalid oracle path (" + oraclePath + ")");
        this.oraclePath = oraclePath;
    }

    public void propertiesPath(Path propertiesPath) {
        if (!Utils.isValidPath(propertiesPath, Utils.PathCheck.PROPERTIES))
            throw new IllegalArgumentException("invalid properties path (" + propertiesPath + ")");
        this.propertiesPath = propertiesPath;
    }

    public void initialTestsPath(Path initialTestsPath) {
        if (!Utils.isValidPath(initialTestsPath, Utils.PathCheck.TESTS))
            throw new IllegalArgumentException("invalid initial tests path (" + initialTestsPath + ")");
        this.initialTestsPath = initialTestsPath;
    }

    public Path modelPath() {
        return modelPath;
    }

    public Path oraclePath() {
        return oraclePath;
    }

    public Path propertiesPath() {
        return propertiesPath;
    }

    public Path initialTestsPath() {
        return initialTestsPath;
    }

    public Path failedTestSuitesFolderPath() {
        return failedTestSuitesFolderPath;
    }

    public boolean hasModel() {
        return modelPath != null;
    }

    public boolean hasOracle() {
        return oraclePath != null;
    }

    public boolean hasProperties() {
        return propertiesPath != null;
    }

    public boolean hasInitialTests() {
        return initialTestsPath != null;
    }

    public boolean hasFailedTestSuitesFolderPath() {
        return failedTestSuitesFolderPath != null;
    }

}
