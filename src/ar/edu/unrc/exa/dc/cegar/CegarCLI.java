package ar.edu.unrc.exa.dc.cegar;

import ar.edu.unrc.exa.dc.search.FixCandidate;
import ar.edu.unrc.exa.dc.search.IterativeCEBasedAlloyRepair;
import ar.edu.unrc.exa.dc.tools.ARepair;
import ar.edu.unrc.exa.dc.tools.BeAFix;
import ar.edu.unrc.exa.dc.util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class CegarCLI {

    private static final String VERSION = "0.1.0";

    private static final String AREPAIR_SAT_SOLVERS = "sat-solvers";
    private static final String AREPAIR_LIBS_ROOT = "libs";
    private static final String AREPAIR_TARGET_ROOT = "target";
    private static final String ALLOY_JAR = "alloy.jar";
    private static final String APARSER_JAR = "aparser-1.0.jar";
    private static final String AREPAIR_JAR = "arepair-1.0-jar-with-dependencies.jar";

    /**
     * Runs CEGAR with a model, oracle, and a properties file
     * @param args the arguments to be used, must be three, two als files and one .properties file
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 1 && args[0].trim().equals("--help")) {
            help();
            return;
        }
        if (args.length != 3) {
            help();
            throw new IllegalArgumentException("Expecting 3 arguments (model, oracle, properties)");
        }
        String model = args[0];
        String oracle = args[1];
        String properties = args[2];
        if (!Utils.isValidPath(model, Utils.PathCheck.ALS))
            throw new IllegalArgumentException("invalid model path (" + model + ")");
        if (!Utils.isValidPath(oracle, Utils.PathCheck.ALS))
            throw new IllegalArgumentException("invalid oracle path (" + oracle + ")");
        if (!Utils.isValidPath(properties, Utils.PathCheck.FILE))
            throw new IllegalArgumentException("invalid properties path (" + properties + ")");
        CEGARProperties.getInstance().loadConfig(properties);
        BeAFix beafix = beafix();
        ARepair arepair = arepair();
        IterativeCEBasedAlloyRepair iterativeCEBasedAlloyRepair = new IterativeCEBasedAlloyRepair(Paths.get(model).toAbsolutePath(), Paths.get(oracle), arepair, beafix);
        Optional<FixCandidate> fix = iterativeCEBasedAlloyRepair.repair();
        if (fix.isPresent()) {
            System.out.println("Fix found\n" + fix.get().toString() + "\n");
        } else {
            System.out.println("No Fix Found for model: " + model + "\n");
        }
    }

    private static void help() {
        String help = "CEGAR CLI\nVERSION " + VERSION + "\n" +
                "CounterExample Guided Alloy Repair\n" +
                "Usage:\n" +
                "\t--help                         :     Shows this message\n" +
                "\t<model> <oracle> <properties>  :     Tries to repair a model (.als file) using an oracle (.als file) with a set of properties (.properties file)\n";
        System.out.println(help);
    }

    private static BeAFix beafix() {
        BeAFix beAFix = new BeAFix().setBeAFixJar(Paths.get(CEGARProperties.getInstance().getStringArgument(CEGARProperties.ConfigKey.BEAFIX_JAR)))
                .setOutputDir(Paths.get("BeAFixOutput").toAbsolutePath())
                .createOutDirIfNonExistent(true);
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
        return new ARepair().setWorkingDirectory(aRepairRoot)
                .setClasspath(classpath)
                .setSatSolversPath(aRepairSatSolvers);
    }

}
