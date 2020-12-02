package ar.edu.unrc.exa.dc.tools;

import ar.edu.unrc.exa.dc.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ar.edu.unrc.exa.dc.util.Utils.exceptionToString;
import static ar.edu.unrc.exa.dc.util.Utils.isValidPath;

public final class BeAFix {

    /*
    Usage:
    <path to model> TESTS [options]

    Options:
    --generate <int>                     :     How many tests to generate (default is 4).
    --out <path to existing folder>      :     Where to store tests (default is the model's folder).
    --arepair <boolean>                  :     Enables/disables arepair integration (default is false).
    --tname <name>                       :     Base name for generated tests, all tests will start with name and be followed by an index.
                                               if name is empty (or a string with all blank space) the base name will be that of the command
                                               from which the counterexample came, in this case no index will be used.
    --tindex <int>                       :     A positive number to be used with a non empty tname that defines the starting index for generated tests
    --modeloverriding <boolean>          :     Enables/disables model overriding (can ignore signatures and use functions instead of fields). This requires
                                               a folder with files <model name>.overrides with the following lines
                                                  * signatures.<signature name>=IGNORE : to ignore a signature in the generated test.
                                                  * field.<field name>=IGNORE : to ignore a field in the generated test.
                                                  * field.<signature name>=function.<no arguments function> : to use a function instead of a field in the generated test
                                               An example can be found in modelOverrides/ordering.overrides.
                                               This feature is disabled by default.
    --mofolder <path to existing folder> :     From which directory to load the .overrides files, default is modelOverrides.
    --itests <boolean>                   :     Enables/disables instance based test generation. This will generate three types of tests:
                                                  * positive trusted tests : the instances from which these tests come from are positive and
                                                    does not they don't involve calling a bugged function or predicate.
                                                  * positive untrusted tests : the instances from which these tests come from are positive but
                                                    involve calling at least one buggy function or predicate.
                                                  * negative tests : tests are made such that the originating property is negated.
                                               This tests are created from run expect > 0 commands, buggy facts are not supported.
                                               This feature is disabled by default.
    --buggyfuncs <path to existing file> :     When there are no expressions marked, lines in this file will be used to define which functions/predicates
                                               are to be considered as buggy. This is used in conjunction with the <itests> feature.
                                               The default value is empty.
    */

    public static final int TESTS_TO_GENERATE_DEFAULT = 4;
    public static final String BASE_TESTS_NAME_DEFAULT = "test";
    public static final boolean CREATE_OUT_DIR_DEFAULT = false;
    public static final boolean AREPAIR_COMPAT_DEFAULT = true;
    public static final int TESTS_STARTING_INDEX_DEFAULT = 1;
    public static final boolean MODEL_OVERRIDES_DEFAULT = true;
    public static final Path MODEL_OVERRIDES_FOLDER_DEFAULT = Paths.get("modelOverrides");
    public static final boolean INSTANCE_TESTS_DEFAULT = true;
    public static final Path BUGGY_FUNCTIONS_DEFAULT = null;


    private Path beAFixJar;
    private Path pathToModel;
    private Path outputDirectory;
    private boolean createOutDirIfNonExistent = CREATE_OUT_DIR_DEFAULT;
    private int testsToGenerate = TESTS_TO_GENERATE_DEFAULT;
    private boolean aRepairCompatibility = AREPAIR_COMPAT_DEFAULT;
    private String baseTestsName = BASE_TESTS_NAME_DEFAULT;
    private int testsStartingIndex = TESTS_STARTING_INDEX_DEFAULT;
    private boolean modelOverrides = MODEL_OVERRIDES_DEFAULT;
    private Path modelOverridesFolder = MODEL_OVERRIDES_FOLDER_DEFAULT;
    private boolean instanceTests = INSTANCE_TESTS_DEFAULT;
    private Path buggyFunctions = BUGGY_FUNCTIONS_DEFAULT;

    public BeAFixResult run() {
        if (!readyToRun())
            throw new IllegalArgumentException("Missing or invalid path related arguments\n" + pathsInformation());
        return executeBeAFix();
    }

    public BeAFix setBeAFixJar(Path beAFixJar) {
        this.beAFixJar = beAFixJar;
        return this;
    }

    public BeAFix setOutputDir(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    public BeAFix createOutDirIfNonExistent(boolean createOutDirIfNonExistent) {
        this.createOutDirIfNonExistent = createOutDirIfNonExistent;
        return this;
    }

    public BeAFix testsToGenerate(int testsToGenerate) {
        if (testsToGenerate < 1)
            throw new IllegalArgumentException("Test to generate less than 1 (" + testsToGenerate + ")");
        this.testsToGenerate = testsToGenerate;
        return this;
    }

    public BeAFix aRepairCompatibility(boolean aRepairCompatibility) {
        this.aRepairCompatibility = aRepairCompatibility;
        return this;
    }

    public BeAFix baseTestsName(String baseTestsName) {
        if (baseTestsName == null || baseTestsName.trim().isEmpty())
            throw new IllegalArgumentException("null or empty base name for tests");
        this.baseTestsName = baseTestsName;
        return this;
    }

    public BeAFix testsStartingIndex(int testsStartingIndex) {
        if (testsStartingIndex < 0)
            throw new IllegalArgumentException("negative value for tests starting index (" + testsStartingIndex + ")");
        this.testsStartingIndex = testsStartingIndex;
        return this;
    }

    public BeAFix modelOverrides(boolean modelOverrides) {
        this.modelOverrides = modelOverrides;
        return this;
    }

    public BeAFix modelOverridesFolder(Path modelOverridesFolder) {
        this.modelOverridesFolder = modelOverridesFolder;
        return this;
    }

    public BeAFix instanceTests(boolean instanceTests) {
        this.instanceTests = instanceTests;
        return this;
    }

    public BeAFix buggyFunctions(Path buggyFunctions) {
        this.buggyFunctions = buggyFunctions;
        return this;
    }

    //AUXILIARY METHODS

    private BeAFixResult executeBeAFix() {
        BeAFixResult beAFixResult = new BeAFixResult();
        try {
            String[] args = getBeAFixCommand();
            ProcessBuilder pb = new ProcessBuilder(args);
            File errorLog = new File("beAFixExternalError.log");
            if (errorLog.exists() && !errorLog.delete())
                throw new IllegalStateException("An error occurred while trying to delete " + errorLog.toString());
            pb.redirectError(ProcessBuilder.Redirect.appendTo(errorLog));
            File outputLog = new File("beAFixExternalOutput.log");
            if (outputLog.exists() && !outputLog.delete())
                throw new IllegalStateException("An error occurred while trying to delete " + outputLog.toString());
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outputLog));
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                beAFixResult.message("BeAFix ended with exit code " + exitCode + " but no exception was caught");
                beAFixResult.error(true);
            } else {
                beAFixResult = getResults();
            }
        } catch (IOException | InterruptedException  e) {
            beAFixResult.message("An exception was caught when executing ARepair\n" + exceptionToString(e));
            beAFixResult.error(true);
        }
        return beAFixResult;
    }

    private static final String CE_POSTFIX = ".tests";
    private static final String INU_POSTFIX = "_negative_untrusted.tests";
    private static final String IPU_POSTFIX = "_positive_untrusted.tests";
    private static final String IPT_POSTFIX = "_positive_trusted.tests";
    private BeAFixResult getResults() {
        Path ceTests = Paths.get(outputDirectory.toString(), pathToModel.getFileName().toString().replace(".als", CE_POSTFIX));
        Path inuTests = Paths.get(outputDirectory.toString(), pathToModel.getFileName().toString().replace(".als", INU_POSTFIX));
        Path ipuTests = Paths.get(outputDirectory.toString(), pathToModel.getFileName().toString().replace(".als", IPU_POSTFIX));
        Path iptTests = Paths.get(outputDirectory.toString(), pathToModel.getFileName().toString().replace(".als", IPT_POSTFIX));
        BeAFixResult testsResults = new BeAFixResult();
        if (ceTests.toFile().exists()) testsResults.counterexampleTests(ceTests);
        if (inuTests.toFile().exists()) testsResults.untrustedNegativeTests(inuTests);
        if (ipuTests.toFile().exists()) testsResults.untrustedPositiveTests(ipuTests);
        if (iptTests.toFile().exists()) testsResults.trustedPositiveTests(iptTests);
        return testsResults;
    }

    private boolean readyToRun() {
        if (outputDirectory != null && !outputDirectory.toFile().exists() && createOutDirIfNonExistent) {
            try {
                Files.createDirectory(outputDirectory);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        if (!isValidPath(beAFixJar, Utils.PathCheck.JAR))
            return false;
        if (!isValidPath(pathToModel, Utils.PathCheck.ALS))
            return false;
        if (outputDirectory != null && !isValidPath(outputDirectory, Utils.PathCheck.DIR))
            return false;
        if (modelOverridesFolder != null && !isValidPath(modelOverridesFolder, Utils.PathCheck.DIR))
            return false;
        return buggyFunctions == null || isValidPath(buggyFunctions, Utils.PathCheck.FILE);
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
        String[] args = new String[23];
        args[0] = "java";
        args[1] = "-jar"; args[2] = beAFixJar.toString();
        args[3] = "\"" + pathToModel.toString() + "\"";
        args[4] = "TESTS";
        args[5] = "--generate"; args[6] = Integer.toString(testsToGenerate);
        args[7] = "--out"; args[8] = "\"" + outputDirectory.toString() + "\"";
        args[9] = "--arepair"; args[10] = Boolean.toString(aRepairCompatibility);
        args[11] = "--tname"; args[12] = "\"" + baseTestsName + "\"";
        args[13] = "--tindex"; args[14] = Integer.toString(testsStartingIndex);
        args[15] = "--modeloverriding"; args[16] = Boolean.toString(modelOverrides);
        args[17] = "--mofolder"; args[18] = "\"" + (modelOverridesFolder == null?" ":modelOverridesFolder.toString()) + "\"";
        args[19] = "--itests"; args[20] = Boolean.toString(instanceTests);
        args[21] = "--buggyfuncs"; args[22] = "\"" + (buggyFunctions == null?" ":buggyFunctions.toString()) + "\"";
        return args;
    }

}
