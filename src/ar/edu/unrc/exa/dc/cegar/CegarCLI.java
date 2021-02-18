package ar.edu.unrc.exa.dc.cegar;

import ar.edu.unrc.exa.dc.search.FixCandidate;
import ar.edu.unrc.exa.dc.search.IterativeCEBasedAlloyRepair;
import ar.edu.unrc.exa.dc.tools.ARepair;
import ar.edu.unrc.exa.dc.tools.BeAFix;
import ar.edu.unrc.exa.dc.tools.InitialTests;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static ar.edu.unrc.exa.dc.util.Utils.startCandidateInfoFile;

public class CegarCLI {

    private static final String VERSION = "1.6.2";

    private static final String AREPAIR_SAT_SOLVERS = "sat-solvers";
    private static final String AREPAIR_LIBS_ROOT = "libs";
    private static final String AREPAIR_TARGET_ROOT = "target";
    private static final String ALLOY_JAR = "alloy.jar";
    private static final String APARSER_JAR = "aparser-1.0.jar";
    private static final String AREPAIR_JAR = "arepair-1.0-jar-with-dependencies.jar";

    private static final String HELP = "--help";

    /**
     * Runs CEGAR with a model, oracle, and a properties file
     * @param args the arguments to be used, must be three, two als files and one .properties file
     */
    public static void main(String[] args) throws IOException {
        if (args[0].trim().compareTo(HELP) == 0) {
            help();
            return;
        }
        parseCommandLine(args);
        CEGARProperties.getInstance().loadConfig(
                CEGARExperiment.getInstance().hasProperties()?
                        CEGARExperiment.getInstance().propertiesPath().toString():
                        CEGARProperties.DEFAULT_PROPERTIES
        );
        BeAFix beafix = beafix();
        ARepair arepair = arepair();
        int laps = IterativeCEBasedAlloyRepair.LAPS_DEFAULT;
        if (CEGARProperties.getInstance().argumentExist(CEGARProperties.ConfigKey.CEGAR_LAPS))
            laps = CEGARProperties.getInstance().getIntArgument(CEGARProperties.ConfigKey.CEGAR_LAPS);
        IterativeCEBasedAlloyRepair iterativeCEBasedAlloyRepair = new IterativeCEBasedAlloyRepair(
                CEGARExperiment.getInstance().modelPath(),
                CEGARExperiment.getInstance().oraclePath(),
                arepair,
                beafix,
                laps
        );
        if (CEGARExperiment.getInstance().hasInitialTests()) {
            InitialTests initialTests = new InitialTests(CEGARExperiment.getInstance().initialTestsPath());
            iterativeCEBasedAlloyRepair.setInitialTests(initialTests);
            beafix.testsStartingIndex(initialTests.getMaxIndex() + 1);
        }
        if (CEGARProperties.getInstance().argumentExist(CEGARProperties.ConfigKey.CEGAR_PRIORIZATION)) {
            iterativeCEBasedAlloyRepair.usePriorization(CEGARProperties.getInstance().getBooleanArgument(CEGARProperties.ConfigKey.CEGAR_PRIORIZATION));
        }
        if (CEGARProperties.getInstance().argumentExist(CEGARProperties.ConfigKey.CEGAR_SEARCH)) {
            String search = CEGARProperties.getInstance().getStringArgument(CEGARProperties.ConfigKey.CEGAR_SEARCH);
            if (search.trim().compareToIgnoreCase(IterativeCEBasedAlloyRepair.CegarSearch.DFS.toString()) == 0) {
                iterativeCEBasedAlloyRepair.setSearch(IterativeCEBasedAlloyRepair.CegarSearch.DFS);
            } else if (search.trim().compareToIgnoreCase(IterativeCEBasedAlloyRepair.CegarSearch.BFS.toString()) == 0) {
                iterativeCEBasedAlloyRepair.setSearch(IterativeCEBasedAlloyRepair.CegarSearch.BFS);
            } else {
                throw new IllegalArgumentException("Invalid configuration value for " + CEGARProperties.ConfigKey.CEGAR_SEARCH.getKey() + " (" + search + ")");
            }
        }
        if (CEGARProperties.getInstance().argumentExist(CEGARProperties.ConfigKey.CEGAR_ENABLE_RELAXEDFACTS_GENERATION)) {
            boolean allowNoFacts = CEGARProperties.getInstance().getBooleanArgument(CEGARProperties.ConfigKey.CEGAR_ENABLE_RELAXEDFACTS_GENERATION);
            iterativeCEBasedAlloyRepair.allowFactsRelaxation(allowNoFacts);
        }
        if (CEGARProperties.getInstance().argumentExist(CEGARProperties.ConfigKey.CEGAR_GLOBAL_TRUSTED_TESTS)) {
            boolean globalTrustedTests = CEGARProperties.getInstance().getBooleanArgument(CEGARProperties.ConfigKey.CEGAR_GLOBAL_TRUSTED_TESTS);
            iterativeCEBasedAlloyRepair.globalTrustedTests(globalTrustedTests);
        }
        if (CEGARProperties.getInstance().argumentExist(CEGARProperties.ConfigKey.CEGAR_ENABLE_FORCE_ASSERTION_TESTS)) {
            boolean forceAssertionsTests = CEGARProperties.getInstance().getBooleanArgument(CEGARProperties.ConfigKey.CEGAR_ENABLE_FORCE_ASSERTION_TESTS);
            iterativeCEBasedAlloyRepair.forceAssertionGeneration(forceAssertionsTests);
        }
        if (CEGARProperties.getInstance().argumentExist(CEGARProperties.ConfigKey.CEGAR_TIMEOUT)) {
            long timeout = CEGARProperties.getInstance().getIntArgument(CEGARProperties.ConfigKey.CEGAR_TIMEOUT);
            if (timeout < 0)
                throw new IllegalArgumentException("invalid value for " + CEGARProperties.ConfigKey.CEGAR_TIMEOUT + " (" + timeout + ")");
            iterativeCEBasedAlloyRepair.timeout(timeout);
        }
        startCandidateInfoFile();
        Optional<FixCandidate> fix = iterativeCEBasedAlloyRepair.repair();
        if (fix.isPresent()) {
            System.out.println("Fix found\n" + fix.get().toString() + "\n");
        } else {
            System.out.println("No Fix Found for model: " + CEGARExperiment.getInstance().modelPath().toString() + "\n");
        }
    }

    private static void parseCommandLine(String[] args) {
        if (args.length == 0)
            return;
        boolean configKeyRead = false;
        String configKey = null;
        for (String arg : args) {
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

    private static final String MODEL_KEY = "model";
    private static final String ORACLE_KEY = "oracle";
    private static final String PROPERTIES_KEY = "properties";
    private static final String INITIAL_TESTS_KEY = "initialtests";
    private static void setConfig(String key, String value) {
        switch (key.toLowerCase()) {
            case MODEL_KEY: {
                if (CEGARExperiment.getInstance().hasModel())
                    throw new IllegalArgumentException("Already a model path has been defined (current: " + CEGARExperiment.getInstance().modelPath().toString() + " | new: " + value + ")");
                Path modelPath = Paths.get(value).toAbsolutePath();
                CEGARExperiment.getInstance().modelPath(modelPath);
                break;
            }
            case ORACLE_KEY: {
                if (CEGARExperiment.getInstance().hasOracle())
                    throw new IllegalArgumentException("Already an oracle path has been defined (current: " + CEGARExperiment.getInstance().oraclePath().toString() + " | new: " + value + ")");
                Path oraclePath = Paths.get(value).toAbsolutePath();
                CEGARExperiment.getInstance().oraclePath(oraclePath);
                break;
            }
            case PROPERTIES_KEY: {
                if (CEGARExperiment.getInstance().hasProperties())
                    throw new IllegalArgumentException("Already a properties path has been defined (current: " + CEGARExperiment.getInstance().propertiesPath().toString() + " | new: " + value + ")");
                Path propertiesPath = Paths.get(value).toAbsolutePath();
                CEGARExperiment.getInstance().propertiesPath(propertiesPath);
                break;
            }
            case INITIAL_TESTS_KEY: {
                if (CEGARExperiment.getInstance().hasInitialTests())
                    throw new IllegalArgumentException("Already an initial tests path has been defined (current: " + CEGARExperiment.getInstance().initialTestsPath().toString() + " | new: " + value + ")");
                Path initialTestsPath = Paths.get(value).toAbsolutePath();
                CEGARExperiment.getInstance().initialTestsPath(initialTestsPath);
                break;
            }
            default : throw new IllegalArgumentException("Invalid configuration key (" + key + ")");
        }
    }

    private static void help() {
        String help = "CEGAR CLI\nVERSION " + VERSION + "\n" +
                "CounterExample Guided Alloy Repair\n" +
                "Usage:\n" +
                "\t--help                                             :  Shows this message\n" +
                "\t--" + MODEL_KEY + "<path to .als file>             :  The path to the model to repair (anything in this file can be modified to repair) (*).\n" +
                "\t--" + ORACLE_KEY + "<path to .als file>            :  The path to the oracle (containing predicates, assertions, and anything related to those which can't be modified to repair) (*).\n" +
                "\t--" + PROPERTIES_KEY + "<path to .properties file> :  CEGAR properties, please look at 'cegar_stein.properties' as an example (**).\n" +
                "\t--" + INITIAL_TESTS_KEY + "<path to .als file>     :  Initial tests set which will be used in conjunction with counterexample based tests (***).\n" +
                "(*)   : This is a required argument.\n" +
                "(**)  : Default properties will be used instead (from cegar.properties).\n" +
                "(***) : Optional argument, default is no initial tests.\n";
        System.out.println(help);
    }

    private static BeAFix beafix() {
        BeAFix beAFix = new BeAFix();
        beAFix.setBeAFixJar(Paths.get(CEGARProperties.getInstance().getStringArgument(CEGARProperties.ConfigKey.BEAFIX_JAR)));
        beAFix.setOutputDir(Paths.get("BeAFixOutput").toAbsolutePath());
        beAFix.createOutDirIfNonExistent(true);
        if (CEGARProperties.getInstance().argumentExist(CEGARProperties.ConfigKey.BEAFIX_INSTANCE_TESTS))
            beAFix.instanceTests(CEGARProperties.getInstance().getBooleanArgument(CEGARProperties.ConfigKey.BEAFIX_INSTANCE_TESTS));
        if (CEGARProperties.getInstance().argumentExist(CEGARProperties.ConfigKey.BEAFIX_TESTS))
            beAFix.testsToGenerate(CEGARProperties.getInstance().getIntArgument(CEGARProperties.ConfigKey.BEAFIX_TESTS));
        if (CEGARProperties.getInstance().argumentExist(CEGARProperties.ConfigKey.BEAFIX_MODEL_OVERRIDES_FOLDER)) {
            String modelOverridesFolderValue = CEGARProperties.getInstance().getStringArgument(CEGARProperties.ConfigKey.BEAFIX_MODEL_OVERRIDES_FOLDER);
            Path modelOverridesFolder = modelOverridesFolderValue.trim().isEmpty()?null:Paths.get(modelOverridesFolderValue);
            beAFix.modelOverridesFolder(modelOverridesFolder);
            beAFix.modelOverrides(modelOverridesFolder != null);
        }
        if (CEGARProperties.getInstance().argumentExist(CEGARProperties.ConfigKey.BEAFIX_BUGGY_FUNCS_FILE)) {
            String buggyFuncsFileValue = CEGARProperties.getInstance().getStringArgument(CEGARProperties.ConfigKey.BEAFIX_BUGGY_FUNCS_FILE);
            Path buggyFuncsFile = buggyFuncsFileValue.trim().isEmpty()?null:Paths.get(buggyFuncsFileValue);
            beAFix.buggyFunctions(buggyFuncsFile);
        }
        if (CEGARProperties.getInstance().argumentExist(CEGARProperties.ConfigKey.BEAFIX_AREPAIR_COMPAT_RELAXED_MODE)) {
            beAFix.aRepairCompatibilityRelaxedMode(CEGARProperties.getInstance().getBooleanArgument(CEGARProperties.ConfigKey.BEAFIX_AREPAIR_COMPAT_RELAXED_MODE));
        }
        return beAFix;
    }

    private static ARepair arepair() {
        List<Path> classpath = new LinkedList<>();
        Path aRepairRoot = Paths.get(CEGARProperties.getInstance().getStringArgument(CEGARProperties.ConfigKey.AREPAIR_ROOT));
        Path aRepairSatSolvers = Paths.get(AREPAIR_SAT_SOLVERS);
        Path aRepairAlloyJar = Paths.get(aRepairRoot.toString(), AREPAIR_LIBS_ROOT, ALLOY_JAR);
        Path aRepairAParserJar = Paths.get(aRepairRoot.toString(), AREPAIR_LIBS_ROOT, APARSER_JAR);
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
