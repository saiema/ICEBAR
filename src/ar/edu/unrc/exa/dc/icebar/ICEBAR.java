package ar.edu.unrc.exa.dc.icebar;

import ar.edu.unrc.exa.dc.search.FixCandidate;
import ar.edu.unrc.exa.dc.search.IterativeCEBasedAlloyRepair;
import ar.edu.unrc.exa.dc.tools.ARepair;
import ar.edu.unrc.exa.dc.tools.BeAFix;
import ar.edu.unrc.exa.dc.tools.BeAFixResult;
import ar.edu.unrc.exa.dc.tools.InitialTests;
import ar.edu.unrc.exa.dc.util.TestHashes;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static ar.edu.unrc.exa.dc.util.Utils.getMaxScopeFromAlsFile;
import static ar.edu.unrc.exa.dc.util.Utils.startCandidateInfoFile;

public class ICEBAR {

    private static final String VERSION = "2.2.0";

    private static final String AREPAIR_SAT_SOLVERS = "sat-solvers";
    private static final String AREPAIR_LIBS_ROOT = "libs";
    private static final String AREPAIR_TARGET_ROOT = "target";
    private static final String ALLOY_JAR = "alloy.jar";
    private static final String APARSER_JAR = "aparser-1.0.jar";
    private static final String AREPAIR_JAR = "arepair-1.0-jar-with-dependencies.jar";

    private static final String HELP_FLAG = "--help";
    private static final String VERSION_FLAG = "--version";

    /**
     * Runs CEGAR with a model, oracle, and a properties file
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
        parseCommandLine(args);
        ICEBARProperties.getInstance().loadConfig(
                ICEBARExperiment.getInstance().hasProperties()?
                        ICEBARExperiment.getInstance().propertiesPath().toString():
                        ICEBARProperties.DEFAULT_PROPERTIES
        );
        BeAFix beafix = beafix();
        ARepair arepair = arepair();
        int laps = IterativeCEBasedAlloyRepair.LAPS_DEFAULT;
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.ICEBAR_LAPS))
            laps = ICEBARProperties.getInstance().getIntArgument(ICEBARProperties.ConfigKey.ICEBAR_LAPS);
        IterativeCEBasedAlloyRepair iterativeCEBasedAlloyRepair = new IterativeCEBasedAlloyRepair(
                ICEBARExperiment.getInstance().modelPath(),
                ICEBARExperiment.getInstance().oraclePath(),
                arepair,
                beafix,
                laps
        );
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.ICEBAR_UPDATE_AREPAIR_SCOPE_FROM_ORACLE)) {
            boolean updateScopeFromOracle = ICEBARProperties.getInstance().getBooleanArgument(ICEBARProperties.ConfigKey.ICEBAR_UPDATE_AREPAIR_SCOPE_FROM_ORACLE);
            if (updateScopeFromOracle)
                arepair.setScope(Math.max(arepair.scope(), getMaxScopeFromAlsFile(ICEBARExperiment.getInstance().oraclePath())));
        }
        if (ICEBARExperiment.getInstance().hasInitialTests()) {
            InitialTests initialTests = new InitialTests(ICEBARExperiment.getInstance().initialTestsPath());
            iterativeCEBasedAlloyRepair.setInitialTests(initialTests);
            beafix.testsStartingIndex(initialTests.getMaxIndex() + 1);
            arepair.setScope(Math.max(arepair.scope(), initialTests.getMaxScope()));
        }
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.ICEBAR_PRIORIZATION)) {
            iterativeCEBasedAlloyRepair.usePriorization(ICEBARProperties.getInstance().getBooleanArgument(ICEBARProperties.ConfigKey.ICEBAR_PRIORIZATION));
        }
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.ICEBAR_SEARCH)) {
            String search = ICEBARProperties.getInstance().getStringArgument(ICEBARProperties.ConfigKey.ICEBAR_SEARCH);
            if (search.trim().compareToIgnoreCase(IterativeCEBasedAlloyRepair.ICEBARSearch.DFS.toString()) == 0) {
                iterativeCEBasedAlloyRepair.setSearch(IterativeCEBasedAlloyRepair.ICEBARSearch.DFS);
            } else if (search.trim().compareToIgnoreCase(IterativeCEBasedAlloyRepair.ICEBARSearch.BFS.toString()) == 0) {
                iterativeCEBasedAlloyRepair.setSearch(IterativeCEBasedAlloyRepair.ICEBARSearch.BFS);
            } else {
                throw new IllegalArgumentException("Invalid configuration value for " + ICEBARProperties.ConfigKey.ICEBAR_SEARCH.getKey() + " (" + search + ")");
            }
        }
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.ICEBAR_ENABLE_RELAXEDFACTS_GENERATION)) {
            boolean allowNoFacts = ICEBARProperties.getInstance().getBooleanArgument(ICEBARProperties.ConfigKey.ICEBAR_ENABLE_RELAXEDFACTS_GENERATION);
            iterativeCEBasedAlloyRepair.allowFactsRelaxation(allowNoFacts);
        }
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.ICEBAR_GLOBAL_TRUSTED_TESTS)) {
            boolean globalTrustedTests = ICEBARProperties.getInstance().getBooleanArgument(ICEBARProperties.ConfigKey.ICEBAR_GLOBAL_TRUSTED_TESTS);
            iterativeCEBasedAlloyRepair.globalTrustedTests(globalTrustedTests);
        }
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.ICEBAR_ENABLE_FORCE_ASSERTION_TESTS)) {
            boolean forceAssertionsTests = ICEBARProperties.getInstance().getBooleanArgument(ICEBARProperties.ConfigKey.ICEBAR_ENABLE_FORCE_ASSERTION_TESTS);
            iterativeCEBasedAlloyRepair.forceAssertionGeneration(forceAssertionsTests);
        }
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.ICEBAR_TIMEOUT)) {
            long timeout = ICEBARProperties.getInstance().getIntArgument(ICEBARProperties.ConfigKey.ICEBAR_TIMEOUT);
            if (timeout < 0)
                throw new IllegalArgumentException("invalid value for " + ICEBARProperties.ConfigKey.ICEBAR_TIMEOUT + " (" + timeout + ")");
            iterativeCEBasedAlloyRepair.timeout(timeout);
        }
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.ICEBAR_KEEP_GOING_ON_AREPAIR_NPE)) {
            boolean keepGoingAfterARepairNPE = ICEBARProperties.getInstance().getBooleanArgument(ICEBARProperties.ConfigKey.ICEBAR_KEEP_GOING_ON_AREPAIR_NPE);
            iterativeCEBasedAlloyRepair.keepGoingAfterARepairNPE(keepGoingAfterARepairNPE);
        }
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.ICEBAR_NO_FIX_ONLY_TRUSTED_KEEP_GOING)) {
            boolean keepGoingARepairNoFixAndOnlyTrustedTests = ICEBARProperties.getInstance().getBooleanArgument(ICEBARProperties.ConfigKey.ICEBAR_NO_FIX_ONLY_TRUSTED_KEEP_GOING);
            iterativeCEBasedAlloyRepair.keepGoingARepairNoFixAndOnlyTrustedTests(keepGoingARepairNoFixAndOnlyTrustedTests);
        }
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.ICEBAR_EMPTY_SEARCH_SPACE_BUT_MAYBE_MORE_TESTS_RETRY)) {
            boolean restartForMoreUnseenTests = ICEBARProperties.getInstance().getBooleanArgument(ICEBARProperties.ConfigKey.ICEBAR_EMPTY_SEARCH_SPACE_BUT_MAYBE_MORE_TESTS_RETRY);
            iterativeCEBasedAlloyRepair.restartForMoreUnseenTests(restartForMoreUnseenTests);
        }
        boolean printProcessGraph = false;
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.ICEBAR_PRINT_PROCESS_GRAPH)) {
            printProcessGraph = ICEBARProperties.getInstance().getBooleanArgument(ICEBARProperties.ConfigKey.ICEBAR_PRINT_PROCESS_GRAPH);
        }
        iterativeCEBasedAlloyRepair.printProcessGraph(printProcessGraph);
        boolean checkRepeated = false;
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.ICEBAR_CHECK_REPEATED_TESTS)) {
            checkRepeated = ICEBARProperties.getInstance().getBooleanArgument(ICEBARProperties.ConfigKey.ICEBAR_CHECK_REPEATED_TESTS);
        }
        FixCandidate.checkRepeated(checkRepeated);
        startCandidateInfoFile();
        Optional<FixCandidate> fix = iterativeCEBasedAlloyRepair.repair();
        if (fix.isPresent()) {
            System.out.println("Fix found\n" + fix.get() + "\n");
        } else {
            System.out.println("No Fix Found for model: " + ICEBARExperiment.getInstance().modelPath().toString() + "\n");
        }
        if (printProcessGraph)
            iterativeCEBasedAlloyRepair.printProcessGraph();
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
                if (ICEBARExperiment.getInstance().hasModel())
                    throw new IllegalArgumentException("Already a model path has been defined (current: " + ICEBARExperiment.getInstance().modelPath().toString() + " | new: " + value + ")");
                Path modelPath = Paths.get(value).toAbsolutePath();
                ICEBARExperiment.getInstance().modelPath(modelPath);
                break;
            }
            case ORACLE_KEY: {
                if (ICEBARExperiment.getInstance().hasOracle())
                    throw new IllegalArgumentException("Already an oracle path has been defined (current: " + ICEBARExperiment.getInstance().oraclePath().toString() + " | new: " + value + ")");
                Path oraclePath = Paths.get(value).toAbsolutePath();
                ICEBARExperiment.getInstance().oraclePath(oraclePath);
                break;
            }
            case PROPERTIES_KEY: {
                if (ICEBARExperiment.getInstance().hasProperties())
                    throw new IllegalArgumentException("Already a properties path has been defined (current: " + ICEBARExperiment.getInstance().propertiesPath().toString() + " | new: " + value + ")");
                Path propertiesPath = Paths.get(value).toAbsolutePath();
                ICEBARExperiment.getInstance().propertiesPath(propertiesPath);
                break;
            }
            case INITIAL_TESTS_KEY: {
                if (ICEBARExperiment.getInstance().hasInitialTests())
                    throw new IllegalArgumentException("Already an initial tests path has been defined (current: " + ICEBARExperiment.getInstance().initialTestsPath().toString() + " | new: " + value + ")");
                Path initialTestsPath = Paths.get(value).toAbsolutePath();
                ICEBARExperiment.getInstance().initialTestsPath(initialTestsPath);
                break;
            }
            default : throw new IllegalArgumentException("Invalid configuration key (" + key + ")");
        }
    }

    private static void help() {
        String help = "ICEBAR CLI\nVERSION " + VERSION + "\n" +
                "Iterative Counter Example-Based Alloy Repair\n" +
                "Usage:\n" +
                "\t--help                                   :  Shows this message\n" +
                "\t--version                                :  Shows the current version of ICEBAR\n" +
                "\t--" + MODEL_KEY + "<path to .als file>               :  The path to the model to repair (anything in this file can be modified to repair) (*).\n" +
                "\t--" + ORACLE_KEY + "<path to .als file>              :  The path to the oracle (containing predicates, assertions, and anything related to those which can't be modified to repair) (*).\n" +
                "\t--" + PROPERTIES_KEY + "<path to .properties file>   :  ICEBAR properties, please look at 'icebar_stein.properties' as an example (**).\n" +
                "\t--" + INITIAL_TESTS_KEY + "<path to .tests file>      :  Initial tests set which will be used in conjunction with counterexample based tests (***).\n" +
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

    private static BeAFix beafix() {
        BeAFix beAFix = new BeAFix();
        beAFix.setBeAFixJar(Paths.get(ICEBARProperties.getInstance().getStringArgument(ICEBARProperties.ConfigKey.BEAFIX_JAR)));
        beAFix.setOutputDir(Paths.get("BeAFixOutput").toAbsolutePath());
        beAFix.createOutDirIfNonExistent(true);
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.BEAFIX_INSTANCE_TESTS))
            beAFix.instanceTests(ICEBARProperties.getInstance().getBooleanArgument(ICEBARProperties.ConfigKey.BEAFIX_INSTANCE_TESTS));
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.BEAFIX_TESTS))
            beAFix.testsToGenerate(ICEBARProperties.getInstance().getIntArgument(ICEBARProperties.ConfigKey.BEAFIX_TESTS));
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.BEAFIX_MODEL_OVERRIDES_FOLDER)) {
            String modelOverridesFolderValue = ICEBARProperties.getInstance().getStringArgument(ICEBARProperties.ConfigKey.BEAFIX_MODEL_OVERRIDES_FOLDER);
            Path modelOverridesFolder = modelOverridesFolderValue.trim().isEmpty()?null:Paths.get(modelOverridesFolderValue);
            beAFix.modelOverridesFolder(modelOverridesFolder);
            beAFix.modelOverrides(modelOverridesFolder != null);
        }
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.BEAFIX_BUGGY_FUNCS_FILE)) {
            String buggyFuncsFileValue = ICEBARProperties.getInstance().getStringArgument(ICEBARProperties.ConfigKey.BEAFIX_BUGGY_FUNCS_FILE);
            Path buggyFuncsFile = buggyFuncsFileValue.trim().isEmpty()?null:Paths.get(buggyFuncsFileValue);
            beAFix.buggyFunctions(buggyFuncsFile);
        }
        if (ICEBARProperties.getInstance().argumentExist(ICEBARProperties.ConfigKey.BEAFIX_AREPAIR_COMPAT_RELAXED_MODE)) {
            beAFix.aRepairCompatibilityRelaxedMode(ICEBARProperties.getInstance().getBooleanArgument(ICEBARProperties.ConfigKey.BEAFIX_AREPAIR_COMPAT_RELAXED_MODE));
        }
        return beAFix;
    }

    private static ARepair arepair() {
        List<Path> classpath = new LinkedList<>();
        Path aRepairRoot = Paths.get(ICEBARProperties.getInstance().getStringArgument(ICEBARProperties.ConfigKey.AREPAIR_ROOT));
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
