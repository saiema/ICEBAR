package ar.edu.unrc.exa.dc.cegar;

import ar.edu.unrc.exa.dc.search.FixCandidate;
import ar.edu.unrc.exa.dc.search.IterativeCEBasedAlloyRepair;
import ar.edu.unrc.exa.dc.tools.ARepair;
import ar.edu.unrc.exa.dc.tools.ARepair.ARepairResult;
import ar.edu.unrc.exa.dc.tools.BeAFix;
import ar.edu.unrc.exa.dc.tools.BeAFixResult;
import ar.edu.unrc.exa.dc.util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public final class Test {

    //BEAFIX
    private static final Path BEAFIX_JAR = Paths.get("/home/stein/Desktop/Projects/ICEBAR/BeAFix/org.alloytools.alloy/out/artifacts/AStryker/BeAFixCLI-2.3.8.jar");

    //AREPAIR
    private static final String AREPAIR_ROOT = "/home/stein/Desktop/Projects/ICEBAR/ARepair";
    private static final String AREPAIR_SAT_SOLVERS = "sat-solvers";
    private static final String AREPAIR_LIBS_ROOT = "libs";
    private static final String AREPAIR_TARGET_ROOT = "target";
    private static final String ALLOY_JAR = "alloy.jar";
    private static final String APARSER_JAR = "aparser-1.0.jar";
    private static final String AREPAIR_JAR = "arepair-1.0-jar-with-dependencies.jar";

    private static final Path model = Paths.get("/home/stein/Desktop/Projects/ICEBAR/Benchmarks/ARepair/graphs_acyclic_23_(10_11).als");
    private static final Path tests = Paths.get("/home/stein/Desktop/Projects/ICEBAR/Benchmarks/ARepair/graphs_acyclic_23_(10_11)_tests.als");
    private static final Path oracle = Paths.get("/home/stein/Desktop/Projects/ICEBAR/Benchmarks/ARepair/Graph-testsPO.als");
    private static final Path modelWithOracle = Paths.get("/home/stein/Desktop/Projects/ICEBAR/Benchmarks/ARepair/graphs_acyclic_23_(10_11)_withOracle.als");

    public static void main(String[] args) throws IOException {
        ARepair aRepair = arepair();
        BeAFix beAFix = beafix();
        Optional<FixCandidate> fix = repair(model, oracle, aRepair, beAFix);
//        testARepair();
        //testBeAFix();
    }

    private static Optional<FixCandidate> repair(Path modelToRepair, Path oracle, ARepair aRepair, BeAFix beAFix) throws IOException {
        IterativeCEBasedAlloyRepair iterativeCEBasedAlloyRepair = new IterativeCEBasedAlloyRepair(modelToRepair, oracle, aRepair, beAFix);
        return iterativeCEBasedAlloyRepair.repair();
    }

    private static ARepair arepair() {
        List<Path> classpath = new LinkedList<>();
        Path aRepairRoot = Paths.get(AREPAIR_ROOT);
        Path aRepairSatSolvers = Paths.get(AREPAIR_SAT_SOLVERS);
        Path aRepairAlloyJar = Paths.get(AREPAIR_ROOT, AREPAIR_LIBS_ROOT, ALLOY_JAR);
        Path aRepairAParserJar = Paths.get(AREPAIR_ROOT, AREPAIR_LIBS_ROOT, APARSER_JAR);
        Path aRepairJar = Paths.get(AREPAIR_ROOT, AREPAIR_TARGET_ROOT, AREPAIR_JAR);
        classpath.add(aRepairJar);
        classpath.add(aRepairAParserJar);
        classpath.add(aRepairAlloyJar);
        return new ARepair().setWorkingDirectory(aRepairRoot)
                .setClasspath(classpath)
                .setSatSolversPath(aRepairSatSolvers)
                .testsPath(tests);
    }

    private static BeAFix beafix() {
        return new BeAFix().setBeAFixJar(BEAFIX_JAR)
                .setOutputDir(Paths.get("BeAFixOutput").toAbsolutePath())
                .createOutDirIfNonExistent(true);
    }

    private static void testARepair() {
        List<Path> classpath = new LinkedList<>();
        Path aRepairRoot = Paths.get(AREPAIR_ROOT);
        Path aRepairSatSolvers = Paths.get(AREPAIR_SAT_SOLVERS);
        Path aRepairAlloyJar = Paths.get(AREPAIR_ROOT, AREPAIR_LIBS_ROOT, ALLOY_JAR);
        Path aRepairAParserJar = Paths.get(AREPAIR_ROOT, AREPAIR_LIBS_ROOT, APARSER_JAR);
        Path aRepairJar = Paths.get(AREPAIR_ROOT, AREPAIR_TARGET_ROOT, AREPAIR_JAR);
        classpath.add(aRepairJar);
        classpath.add(aRepairAParserJar);
        classpath.add(aRepairAlloyJar);
        ARepair aRepair = new ARepair();
        ARepairResult aRepairResult = aRepair
                .setWorkingDirectory(aRepairRoot)
                .setClasspath(classpath)
                .setSatSolversPath(aRepairSatSolvers)
                .modelToRepair(model)
                .testsPath(tests)
                .run();
        System.out.println(aRepairResult.toString());
    }

    private static void testBeAFix() throws IOException {
        if (modelWithOracle.toFile().exists()) {
            if (!modelWithOracle.toFile().delete())
                throw new IllegalStateException("Failed to delete " + modelWithOracle.toString());
        }
        Utils.mergeFiles(model, oracle, modelWithOracle);
        BeAFix beAFix = new BeAFix();
        BeAFixResult beAFixResult = beAFix
                .setBeAFixJar(BEAFIX_JAR)
                .pathToModel(modelWithOracle)
                .setOutputDir(Paths.get("BeAFixOutput").toAbsolutePath())
                .createOutDirIfNonExistent(true)
                .runTestGeneration();
        System.out.println(beAFixResult.toString());
    }

}
