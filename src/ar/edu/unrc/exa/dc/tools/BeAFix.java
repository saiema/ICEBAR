package ar.edu.unrc.exa.dc.tools;

import ar.edu.unrc.exa.dc.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

import static ar.edu.unrc.exa.dc.util.Utils.exceptionToString;
import static ar.edu.unrc.exa.dc.util.Utils.isValidPath;

public final class BeAFix {

    public static final int TESTS_TO_GENERATE_DEFAULT = 4;
    public static final String BASE_TESTS_NAME_DEFAULT = "test";
    public static final boolean CREATE_OUT_DIR_DEFAULT = true;
    public static final boolean AREPAIR_COMPAT_DEFAULT = true;
    public static final boolean AREPAIR_COMPAT_RELAX_MODE_DEFAULT = false;
    public static final int TESTS_STARTING_INDEX_DEFAULT = 1;
    public static final boolean MODEL_OVERRIDES_DEFAULT = true;
    public static final Path MODEL_OVERRIDES_FOLDER_DEFAULT = null;
    public static final boolean INSTANCE_TESTS_DEFAULT = true;
    public static final Path BUGGY_FUNCTIONS_DEFAULT = null;
    public static final boolean FACTSRELAXATION_DEFAULT = false;
    public static final boolean FORCE_ASSERTION_TESTS_GENERATION_DEFAULT = false;


    private Path beAFixJar;
    private Path pathToModel;
    private Path outputDirectory;
    private boolean createOutDirIfNonExistent = CREATE_OUT_DIR_DEFAULT;
    private int testsToGenerate = TESTS_TO_GENERATE_DEFAULT;
    private boolean aRepairCompatibility = AREPAIR_COMPAT_DEFAULT;
    private boolean aRepairCompatibilityRelaxedMode = AREPAIR_COMPAT_RELAX_MODE_DEFAULT;
    private String baseTestsName = BASE_TESTS_NAME_DEFAULT;
    private int testsStartingIndex = TESTS_STARTING_INDEX_DEFAULT;
    private boolean modelOverrides = MODEL_OVERRIDES_DEFAULT;
    private Path modelOverridesFolder = MODEL_OVERRIDES_FOLDER_DEFAULT;
    private boolean instanceTests = INSTANCE_TESTS_DEFAULT;
    private Path buggyFunctions = BUGGY_FUNCTIONS_DEFAULT;
    private boolean factsRelaxationGeneration = FACTSRELAXATION_DEFAULT;
    private boolean forceAssertionTestsGeneration = FORCE_ASSERTION_TESTS_GENERATION_DEFAULT;

    public BeAFixResult runTestGeneration() {
        if (invalidPaths())
            throw new IllegalArgumentException("Missing or invalid path related arguments\n" + pathsInformation());
        return executeBeAFix();
    }

    public BeAFixResult runModelCheck() {
        if (invalidPaths())
            throw new IllegalArgumentException("Missing or invalid path related arguments\n" + pathsInformation());
        return executeBeAFixCheck();
    }

    public void setBeAFixJar(Path beAFixJar) {
        this.beAFixJar = beAFixJar;
    }

    public void pathToModel(Path pathToModel) {
        this.pathToModel = pathToModel;
    }

    public void setOutputDir(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void createOutDirIfNonExistent(boolean createOutDirIfNonExistent) {
        this.createOutDirIfNonExistent = createOutDirIfNonExistent;
    }

    public void testsToGenerate(int testsToGenerate) {
        if (testsToGenerate < 1)
            throw new IllegalArgumentException("Test to generate less than 1 (" + testsToGenerate + ")");
        this.testsToGenerate = testsToGenerate;
    }

    public void aRepairCompatibility(boolean aRepairCompatibility) {
        this.aRepairCompatibility = aRepairCompatibility;
    }

    public void aRepairCompatibilityRelaxedMode(boolean aRepairCompatibilityRelaxedMode) {
        this.aRepairCompatibilityRelaxedMode = aRepairCompatibilityRelaxedMode;
    }

    public void baseTestsName(String baseTestsName) {
        if (baseTestsName == null || baseTestsName.trim().isEmpty())
            throw new IllegalArgumentException("null or empty base name for tests");
        this.baseTestsName = baseTestsName;
    }

    public void testsStartingIndex(int testsStartingIndex) {
        if (testsStartingIndex < 0)
            throw new IllegalArgumentException("negative value for tests starting index (" + testsStartingIndex + ")");
        this.testsStartingIndex = testsStartingIndex;
    }

    public int testsStartingIndex() {
        return testsStartingIndex;
    }

    public void modelOverrides(boolean modelOverrides) {
        this.modelOverrides = modelOverrides;
    }

    public void modelOverridesFolder(Path modelOverridesFolder) {
        this.modelOverridesFolder = modelOverridesFolder;
    }

    public void instanceTests(boolean instanceTests) {
        this.instanceTests = instanceTests;
    }

    public void buggyFunctions(Path buggyFunctions) {
        this.buggyFunctions = buggyFunctions;
    }

    public void factsRelaxationGeneration(boolean factsRelaxationGeneration) {
        this.factsRelaxationGeneration = factsRelaxationGeneration;
    }

    public void forceAssertionTestsGeneration(boolean forceAssertionTestsGeneration) {
        this.forceAssertionTestsGeneration = forceAssertionTestsGeneration;
    }

    public boolean cleanOutputDir() throws IOException {
        if (outputDirectory == null)
            throw new IllegalStateException("Output directory not defined");
        if (!isValidPath(outputDirectory, Utils.PathCheck.EXISTS))
            return true;
        AtomicBoolean result = new AtomicBoolean(false);
        Files.walk(outputDirectory)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(f -> result.set(f.delete()));
        return result.get();
    }

    //AUXILIARY METHODS

    private BeAFixResult executeBeAFix() {
        BeAFixResult beAFixResult;
        try {
            String[] args = getBeAFixCommand();
            ProcessBuilder pb = new ProcessBuilder(args);
            File errorLog = new File("beAFixExternalError.log");
            if (errorLog.exists() && !errorLog.delete())
                throw new IllegalStateException("An error occurred while trying to delete " + errorLog);
            pb.redirectError(ProcessBuilder.Redirect.appendTo(errorLog));
            File outputLog = new File("beAFixExternalOutput.log");
            if (outputLog.exists() && !outputLog.delete())
                throw new IllegalStateException("An error occurred while trying to delete " + outputLog);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outputLog));
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                beAFixResult = BeAFixResult.error("BeAFix ended with exit code " + exitCode + " but no exception was caught");
            } else {
                beAFixResult = getResults();
            }
        } catch (IOException | InterruptedException  e) {
            beAFixResult = BeAFixResult.error("An exception was caught when executing BeAFix\n" + exceptionToString(e));
        }
        return beAFixResult;
    }

    private BeAFixResult executeBeAFixCheck() {
        BeAFixResult beAFixResult;
        try {
            String[] args = getBeAFixCheckCommand();
            ProcessBuilder pb = new ProcessBuilder(args);
            File errorLog = new File("beAFixExternalError.log");
            if (errorLog.exists() && !errorLog.delete())
                throw new IllegalStateException("An error occurred while trying to delete " + errorLog);
            pb.redirectError(ProcessBuilder.Redirect.appendTo(errorLog));
            File outputLog = new File("beAFixExternalOutput.log");
            if (outputLog.exists() && !outputLog.delete())
                throw new IllegalStateException("An error occurred while trying to delete " + outputLog);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outputLog));
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                beAFixResult = BeAFixResult.error("BeAFix ended with exit code " + exitCode + " but no exception was caught");
            } else {
                beAFixResult = BeAFixResult.check(Paths.get(pathToModel.toAbsolutePath().toString().replace(".als", ".verification")));
            }
        } catch (IOException | InterruptedException  e) {
            beAFixResult = BeAFixResult.error("An exception was caught when executing BeAFix\n" + exceptionToString(e));
        }
        return beAFixResult;
    }

    private static final String CE_POSTFIX = "_counterexamples.tests";
    private static final String POS_TRUSTED_POSTFIX = "_positive_trusted.tests";
    private static final String POS_UNTRUSTED_POSTFIX = "_positive_untrusted.tests";
    private static final String NEG_TRUSTED_POSTFIX = "_negative_trusted.tests";
    private static final String NEG_UNTRUSTED_POSTFIX = "_negative_untrusted.tests";
    private BeAFixResult getResults() {
        Path ceTests = Paths.get(outputDirectory.toString(), pathToModel.getFileName().toString().replace(".als", CE_POSTFIX));
        Path nuTests = Paths.get(outputDirectory.toString(), pathToModel.getFileName().toString().replace(".als", NEG_UNTRUSTED_POSTFIX));
        Path ntTests = Paths.get(outputDirectory.toString(), pathToModel.getFileName().toString().replace(".als", NEG_TRUSTED_POSTFIX));
        Path puTests = Paths.get(outputDirectory.toString(), pathToModel.getFileName().toString().replace(".als", POS_UNTRUSTED_POSTFIX));
        Path ptTests = Paths.get(outputDirectory.toString(), pathToModel.getFileName().toString().replace(".als", POS_TRUSTED_POSTFIX));
        BeAFixResult testsResults = BeAFixResult.tests();
        if (ceTests.toFile().exists()) testsResults.counterexampleTests(ceTests);
        if (nuTests.toFile().exists()) testsResults.untrustedNegativeTests(nuTests);
        if (ntTests.toFile().exists()) testsResults.trustedNegativeTests(ntTests);
        if (puTests.toFile().exists()) testsResults.untrustedPositiveTests(puTests);
        if (ptTests.toFile().exists()) testsResults.trustedPositiveTests(ptTests);
        return testsResults;
    }

    private boolean invalidPaths() {
        if (outputDirectory != null && !outputDirectory.toFile().exists() && createOutDirIfNonExistent) {
            try {
                Files.createDirectory(outputDirectory);
            } catch (IOException e) {
                e.printStackTrace();
                return true;
            }
        }
        if (!isValidPath(beAFixJar, Utils.PathCheck.JAR))
            return true;
        if (!isValidPath(pathToModel, Utils.PathCheck.ALS))
            return true;
        if (outputDirectory != null && !isValidPath(outputDirectory, Utils.PathCheck.DIR))
            return true;
        if (modelOverridesFolder != null && !isValidPath(modelOverridesFolder, Utils.PathCheck.DIR))
            return true;
        return buggyFunctions != null && !isValidPath(buggyFunctions, Utils.PathCheck.FILE);
    }

    private String pathsInformation() {
        String pinfo = "";
        pinfo += "BeAFix Jar             : " + (beAFixJar==null?"NULL":beAFixJar.toString()) + "\n";
        pinfo += "Path to model          : " + (pathToModel==null?"NULL":pathToModel.toString()) + "\n";
        pinfo += "Output Directory       : " + (outputDirectory==null?"Unset (will use the directory of the model)":outputDirectory.toString()) + "\n";
        pinfo += "Model Overrides folder : " + (modelOverridesFolder==null?"Unset (no overrides will be used)":modelOverridesFolder.toString()) + "\n";
        pinfo += "Buggy Functions file   : " + (buggyFunctions==null?"Unset (no buggy functions file will be used)":buggyFunctions.toString()) + "\n";
        return pinfo;
    }

    private String[] getBeAFixCommand() {
        String[] args = new String[29];
        args[0] = "java";
        args[1] = "-jar"; args[2] = beAFixJar.toString();
        args[3] = pathToModel.toString();
        args[4] = "TESTS";
        args[5] = "--generate"; args[6] = Integer.toString(testsToGenerate);
        args[7] = "--out"; args[8] = outputDirectory.toString();
        args[9] = "--arepair"; args[10] = Boolean.toString(aRepairCompatibility);
        args[11] = "--arelaxed"; args[12] = Boolean.toString(aRepairCompatibilityRelaxedMode);
        args[13] = "--tname"; args[14] = baseTestsName;
        args[15] = "--tindex"; args[16] = Integer.toString(testsStartingIndex);
        args[17] = "--modeloverriding"; args[18] = Boolean.toString(modelOverrides);
        args[19] = "--mofolder"; args[20] = (modelOverridesFolder == null?"\" \"":modelOverridesFolder.toString());
        args[21] = "--itests"; args[22] = Boolean.toString(instanceTests);
        args[23] = "--buggyfuncs"; args[24] = (buggyFunctions == null?"\" \"":buggyFunctions.toString());
        args[25] = "--relaxedfacts"; args[26] = Boolean.toString(factsRelaxationGeneration);
        args[27] = "--fassertiontests"; args[28] = Boolean.toString(forceAssertionTestsGeneration);
        return args;
    }

    private String[] getBeAFixCheckCommand() {
        String[] args = new String[5];
        args[0] = "java";
        args[1] = "-jar"; args[2] = beAFixJar.toString();
        args[3] = pathToModel.toString();
        args[4] = "CHECK";
        return args;
    }

}
