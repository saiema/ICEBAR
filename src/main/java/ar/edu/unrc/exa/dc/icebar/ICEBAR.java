package ar.edu.unrc.exa.dc.icebar;

import ar.edu.unrc.exa.dc.icebar.properties.ICEBARFileBasedProperties;
import ar.edu.unrc.exa.dc.icebar.properties.ICEBARProperties;
import ar.edu.unrc.exa.dc.logging.LocalLogging;
import ar.edu.unrc.exa.dc.search.FixCandidate;
import ar.edu.unrc.exa.dc.search.IterativeCEBasedAlloyRepair;
import ar.edu.unrc.exa.dc.tools.ARepair;
import ar.edu.unrc.exa.dc.tools.BeAFix;
import ar.edu.unrc.exa.dc.tools.BeAFixResult;
import ar.edu.unrc.exa.dc.tools.InitialTests;
import ar.edu.unrc.exa.dc.util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static ar.edu.unrc.exa.dc.util.Utils.getMaxScopeFromAlsFile;
import static ar.edu.unrc.exa.dc.util.Utils.startCandidateInfoFile;

public class ICEBAR {

    private static final String VERSION = "3.0.0";

    private static final String BEAFIX_MIN_VERSION = "2.12.1";
    private static final String AREPAIR_MIN_VERSION = "*";

    private static final String AREPAIR_SAT_SOLVERS = "sat-solvers";
    private static final String AREPAIR_LIBS_ROOT = "libs";
    private static final String AREPAIR_TARGET_ROOT = "target";
    private static final String ALLOY_JAR = "alloy.jar";
    private static final String AREPAIR_PARSER_JAR = "aparser-1.0.jar";
    private static final String AREPAIR_JAR = "arepair-1.0-jar-with-dependencies.jar";

    private static final String HELP_FLAG = "--help";
    private static final String VERSION_FLAG = "--version";

    private static final String OPTIONS_FLAG = "--options";

    private static final String GENERATE_TEMPLATE_PROPERTIES_FLAG = "--generateTemplateProperties";

    /**
     * Runs ICEBAR with a model, oracle, and a properties file
     * @param args the arguments to be used, must be three, two als files and one .properties file
     */
    public static void main(String[] args) throws IOException {
        if (args[0].trim().compareToIgnoreCase(HELP_FLAG) == 0) {
            help();
            return;
        }
        if (args[0].trim().compareToIgnoreCase(VERSION_FLAG) == 0) {
            version();
            return;
        }
        if (args[0].trim().compareToIgnoreCase(OPTIONS_FLAG) == 0) {
            options();
            return;
        }
        if (args[0].trim().compareToIgnoreCase(GENERATE_TEMPLATE_PROPERTIES_FLAG) == 0) {
            if (args.length != 2) {
                System.err.println("Expected a path to a .properties file as second argument");
                return;
            }
            String path = args[1];
            generateTemplateProperties(path);
            return;
        }
        parseCommandLine(args);
        if (ICEBARExperiment.getInstance().hasProperties()) {
            ICEBARFileBasedProperties.ICEBAR_PROPERTIES = ICEBARExperiment.getInstance().propertiesPath().toAbsolutePath().toString();
        }
        Logger logger = LocalLogging.getLogger(ICEBAR.class, ICEBARProperties.getInstance().icebarConsoleLoggingLevel(), ICEBARProperties.getInstance().icebarFileLoggingLevel());
        if (ICEBARExperiment.getInstance().hasProperties()) {
            logger.info("Using custom .properties file: " + ICEBARExperiment.getInstance().propertiesPath());
        }
        updateICEBARExperimentTestSuiteProperty();
        BeAFix beafix = beafix();
        ARepair arepair = arepair();
        int laps = ICEBARProperties.getInstance().icebarLaps();
        IterativeCEBasedAlloyRepair iterativeCEBasedAlloyRepair = new IterativeCEBasedAlloyRepair(
                ICEBARExperiment.getInstance().modelPath(),
                ICEBARExperiment.getInstance().oraclePath(),
                arepair,
                beafix,
                laps,
                logger
        );
        boolean updateScopeFromOracle = ICEBARProperties.getInstance().updateARepairScopeFromOracle();
        if (updateScopeFromOracle)
            arepair.setScope(Math.max(arepair.scope(), getMaxScopeFromAlsFile(ICEBARExperiment.getInstance().oraclePath())));
        if (ICEBARExperiment.getInstance().hasInitialTests()) {
            InitialTests initialTests = new InitialTests(ICEBARExperiment.getInstance().initialTestsPath());
            iterativeCEBasedAlloyRepair.setInitialTests(initialTests);
            beafix.testsStartingIndex(initialTests.getMaxIndex() + 1);
            arepair.setScope(Math.max(arepair.scope(), initialTests.getMaxScope()));
        }
        iterativeCEBasedAlloyRepair.usePrioritization(ICEBARProperties.getInstance().enableCandidatePrioritization());
        iterativeCEBasedAlloyRepair.setSearch(ICEBARProperties.getInstance().icebarSearchAlgorithm());
        iterativeCEBasedAlloyRepair.allowFactsRelaxation(ICEBARProperties.getInstance().enableRelaxedFactsTestGeneration());
        iterativeCEBasedAlloyRepair.globalTrustedTests(ICEBARProperties.getInstance().globalTrustedTests());
        iterativeCEBasedAlloyRepair.forceAssertionGeneration(ICEBARProperties.getInstance().forceAssertionTestGeneration());
        iterativeCEBasedAlloyRepair.timeout(ICEBARProperties.getInstance().icebarTimeout());
        iterativeCEBasedAlloyRepair.keepGoingAfterARepairNPE(ICEBARProperties.getInstance().keepGoingOnARepairNPE());
        iterativeCEBasedAlloyRepair.enableOpenAISuggestions(ICEBARProperties.getInstance().icebarOpenAI());
        arepair.treatPartialRepairsAsFixes(ICEBARProperties.getInstance().arepairTreatPartialRepairsAsFixes());
        startCandidateInfoFile();
        Optional<FixCandidate> fix = iterativeCEBasedAlloyRepair.repair();
        String timingsAndARepairCalls = "\n\tBeAFix total time (Candidate validation and test generation): " + iterativeCEBasedAlloyRepair.beafixTimeCounter().toMilliSeconds() + "ms" +
                "\n\tARepair total time: " + iterativeCEBasedAlloyRepair.arepairTimeCounter().toMilliSeconds() + "ms" +
                "\n\tICEBAR total time: " + iterativeCEBasedAlloyRepair.totalTime().toMilliSeconds() + "ms" +
                "\n\tARepair total calls: " + iterativeCEBasedAlloyRepair.arepairCalls() +
                "\n\tTotal tests generated: " + iterativeCEBasedAlloyRepair.totalTestsGenerated();
        if (fix.isPresent()) {
            logger.info("Fix found\n" + fix.get() +
                    "\n\tRepaired model located at " + Paths.get(
                            ICEBARProperties.getInstance().arepairRootFolder().toAbsolutePath().toString(),
                            ARepair.FIX_FILE
                    ) +
                    "\n\tTest suite used located at " + ICEBARExperiment.getInstance().modelPath().toAbsolutePath().toString().replace(".als", "_tests.als") +
                    timingsAndARepairCalls
            );
        } else {
            logger.info(
                    "No Fix Found for model: " +
                    ICEBARExperiment.getInstance().modelPath() + timingsAndARepairCalls
            );
        }
        if (ICEBARProperties.getInstance().saveAllTestSuites()) {
            logger.info(
                    "All test suites that did not produce a valid fix, can be found at " +
                         ICEBARExperiment.getInstance().failedTestSuitesFolderPath().toAbsolutePath()
            );
        }
    }

    private static void updateICEBARExperimentTestSuiteProperty() throws IOException {
        if (ICEBARProperties.getInstance().saveAllTestSuites()) {
            String modelFileName = ICEBARExperiment.getInstance().modelPath().getFileName().toString();
            String modelName = modelFileName;
            int lastDot = modelFileName.lastIndexOf(".");
            if (lastDot > 0) {
                modelName = modelFileName.substring(0, lastDot);
            }
            Path failedTestSuitesFolderPath = Utils.createFailedTestSuitesFolder(modelName);
            ICEBARExperiment.getInstance().failedTestSuitesFolderPath(failedTestSuitesFolderPath);
        }
    }

    private static void parseCommandLine(String[] args) {
        if (args.length == 0)
            return;
        boolean configKeyRead = false;
        String configKey = null;
        for (String arg : args) {
            if (isVMArgument(arg)) {
                throw new IllegalArgumentException("VM argument detected as program argument (-D<key>=<value), VM arguments must go before the classname or before `-jar <jar file>` if running a jar.");
            }
            arg = arg.replaceAll("\"", "");
            if (arg.trim().startsWith("--")) {
                if (configKeyRead)
                    throw new IllegalArgumentException("Expecting value for " + configKey + " got " + arg.trim() + " instead");
                configKeyRead = true;
                configKey = arg.trim().substring(2);
            } else {
                if (!configKeyRead)
                    throw new IllegalArgumentException("Expecting config key but got a value instead " + arg.trim());
                setConfig(configKey, arg.trim());
                configKeyRead = false;
                configKey = null;
            }
        }
    }

    private static boolean isVMArgument(String argument) {
        return argument.startsWith("-D");
    }

    private static final String MODEL_KEY = "model";
    private static final String ORACLE_KEY = "oracle";
    private static final String PROPERTIES_KEY = "properties";
    private static final String INITIAL_TESTS_KEY = "initialtests";
    private static void setConfig(String key, String value) {
        Path path = Paths.get(value);
        switch (key.toLowerCase()) {
            case MODEL_KEY: {
                if (ICEBARExperiment.getInstance().hasModel())
                    throw new IllegalArgumentException("Already a model path has been defined (current: " + ICEBARExperiment.getInstance().modelPath().toString() + " | new: " + value + ")");
                Path modelPath = path.toAbsolutePath();
                ICEBARExperiment.getInstance().modelPath(modelPath);
                break;
            }
            case ORACLE_KEY: {
                if (ICEBARExperiment.getInstance().hasOracle())
                    throw new IllegalArgumentException("Already an oracle path has been defined (current: " + ICEBARExperiment.getInstance().oraclePath().toString() + " | new: " + value + ")");
                Path oraclePath = path.toAbsolutePath();
                ICEBARExperiment.getInstance().oraclePath(oraclePath);
                break;
            }
            case PROPERTIES_KEY: {
                if (ICEBARExperiment.getInstance().hasProperties())
                    throw new IllegalArgumentException("Already a properties path has been defined (current: " + ICEBARExperiment.getInstance().propertiesPath().toString() + " | new: " + value + ")");
                Path propertiesPath = path.toAbsolutePath();
                ICEBARExperiment.getInstance().propertiesPath(propertiesPath);
                break;
            }
            case INITIAL_TESTS_KEY: {
                if (ICEBARExperiment.getInstance().hasInitialTests())
                    throw new IllegalArgumentException("Already an initial tests path has been defined (current: " + ICEBARExperiment.getInstance().initialTestsPath().toString() + " | new: " + value + ")");
                Path initialTestsPath = path.toAbsolutePath();
                ICEBARExperiment.getInstance().initialTestsPath(initialTestsPath);
                break;
            }
            default : throw new IllegalArgumentException("Invalid configuration key (" + key + ")");
        }
    }

    private static void help() {
        String help = "ICEBAR CLI" +
                "\nVERSION " + VERSION +
                "\nBeAFix Minimum Version " + BEAFIX_MIN_VERSION +
                "\nARepair Minimum Version " + AREPAIR_MIN_VERSION +
                "\n" +
                "Iterative Counter Example-Based Alloy Repair\n" +
                "Usage: java -jar Icebar.jar [CONFIGURATION VALUES] <ARGUMENTS>\n" +
                "Where <ARGUMENTS> can be one of the following:\n" +
                "  --help...............................................................Shows this message\n" +
                "  --options............................................................Show all configuration options\n" +
                "  --generateTemplateProperties <path to new .properties file>..........Generates a template properties file\n" +
                "  --version............................................................Shows the current version of ICEBAR\n" +
                "  --" + MODEL_KEY + "<path to .als file>...........................................The path to the model to repair (anything in this file can be modified to repair) (*).\n" +
                "  --" + ORACLE_KEY + "<path to .als file>..........................................The path to the oracle (containing predicates, assertions, and anything related to those which can't be modified to repair) (*).\n" +
                "  --" + PROPERTIES_KEY + "<path to .properties file>...............................ICEBAR properties, please look at 'icebar.properties' as an example (**).\n" +
                "  --" + INITIAL_TESTS_KEY + "<path to .tests file>..................................Initial tests set which will be used in conjunction with counterexample based tests (***).\n" +
                "The CONFIGURATION VALUES are defined by `-D<key>=<value>`, where each key is an ICEBAR option (see --options argument)\n" +
                "Each configuration defined in this way, will override the configuration defined by the .properties file\n" +
                "(*)   : This is a required argument.\n" +
                "(**)  : Default properties will be used instead (from icebar.properties).\n" +
                "(***) : Optional argument, default is no initial tests.\n" +
                "About initial tests:\n" +
                "A test is defined as a predicate and a run <predicate's name> expect (0|1) command\n" +
                "Each test must be separated by a line containing " + BeAFixResult.TEST_SEPARATOR + "\n" +
                "Each predicate must be between a line with " + BeAFixResult.BeAFixTest.PREDICATE_START_DELIMITER.replace("\n", "")  + " and a line with " + BeAFixResult.BeAFixTest.PREDICATE_END_DELIMITER;
        System.out.println(help);
    }

    private static void version() {
        System.out.println(VERSION);
    }

    private static void options() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nICEBAR's properties/configuration arguments\n");
        sb.append("ICEBAR can be configure both by a .properties file having lines with <key>=<value> or by using `-D<key>=<value>` command line arguments.\n");
        sb.append("The command line arguments will always override the configurations defined in the .properties file, available options are:\n\n");
        for (Map.Entry<String, String> optionAndDescription : ICEBARProperties.getOptionsAndDescriptions().entrySet()) {
            sb.append("Key: ").append(optionAndDescription.getKey()).append("\n");
            sb.append("Description: ").append(optionAndDescription.getValue()).append("\n");
            sb.append("\n");
        }
        System.out.println(sb);
    }

    private static void generateTemplateProperties(String path) throws IOException {
        Path newPropertiesTemplate = ICEBARProperties.generateTemplatePropertiesFile(path);
        if (newPropertiesTemplate == null) {
            System.err.println("The path to the new .properties template appears to be incorrect, please check it and try again (" + path + ")");
        } else {
            System.out.println("The new .properties template was successfully created at " + path);
        }
    }

    private static BeAFix beafix() {
        BeAFix beAFix = new BeAFix();
        beAFix.setBeAFixJar(ICEBARProperties.getInstance().beafixJar());
        beAFix.setOutputDir(Paths.get("BeAFixOutput").toAbsolutePath());
        beAFix.createOutDirIfNonExistent(true);
        beAFix.testsToGenerate(ICEBARProperties.getInstance().testsToGenerateUpperBound());
        beAFix.instanceTests(ICEBARProperties.getInstance().enableBeAFixInstanceTestsGeneration());
        beAFix.noInstanceTestForNegativeTestWhenNoFacts(ICEBARProperties.getInstance().noInstanceTestForNegativeBranchWhenNoFacts());
        beAFix.modelOverrides(ICEBARProperties.getInstance().beafixModelOverridesFolder() != null);
        beAFix.modelOverridesFolder(ICEBARProperties.getInstance().beafixModelOverridesFolder());
        return beAFix;
    }

    private static ARepair arepair() {
        List<Path> classpath = new LinkedList<>();
        Path aRepairRoot = ICEBARProperties.getInstance().arepairRootFolder();
        Path aRepairSatSolvers = Paths.get(AREPAIR_SAT_SOLVERS);
        Path aRepairAlloyJar = Paths.get(aRepairRoot.toString(), AREPAIR_LIBS_ROOT, ALLOY_JAR);
        Path aRepairAParserJar = Paths.get(aRepairRoot.toString(), AREPAIR_LIBS_ROOT, AREPAIR_PARSER_JAR);
        Path aRepairJar = Paths.get(aRepairRoot.toString(), AREPAIR_TARGET_ROOT, AREPAIR_JAR);
        classpath.add(aRepairJar);
        classpath.add(aRepairAParserJar);
        classpath.add(aRepairAlloyJar);
        ARepair aRepair = new ARepair();
        aRepair.setWorkingDirectory(aRepairRoot);
        aRepair.setClasspath(classpath);
        aRepair.setSatSolversPath(aRepairSatSolvers);
        return aRepair;
    }

}
