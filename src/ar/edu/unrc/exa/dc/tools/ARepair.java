package ar.edu.unrc.exa.dc.tools;

import ar.edu.unrc.exa.dc.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static ar.edu.unrc.exa.dc.util.Utils.exceptionToString;
import static ar.edu.unrc.exa.dc.util.Utils.isValidPath;

public final class ARepair {

    public enum ARepairResult {

        REPAIRED,
        NOT_REPAIRED,
        ERROR,
        NO_TESTS;

        private String message = null;
        private Path repair = null;

        public String message() {
            return message;
        }

        public void message(String msg) {
            this.message = msg;
        }

        public boolean hasMessage() {
            return message != null && !message.trim().isEmpty();
        }

        public Path repair() {
            return repair;
        }

        public void repair(Path repair) {
            this.repair = repair;
        }

        public boolean hasRepair() {
            return repair != null;
        }

        @Override
        public String toString() {
            String rep = "{\n\t"  + name();
            if (!equals(NO_TESTS)) {
                if (hasMessage()) {
                    rep += "\n\tMessage: " + message;
                }
                if (hasRepair()) {
                    rep += "\n\tRepair found: " + repair.toAbsolutePath().toString();
                }
            }
            rep += "\n}";
            return rep;
        }

    }

    private enum SearchStrategy {
        BASE_CHOICE {
            @Override
            public String getName() {
                return "base-choice";
            }
        },
        ALL_COMBINATIONS {
            @Override
            public String getName() {
                return "all-combinations";
            }
        };

        public abstract String getName();
    }

    public static final int SCOPE_DEFAULT = 3;
    public static final int MINIMUM_COST_DEFAULT = 3;
    public static final int MEMORY_DEFAULT = 16;
    public static final SearchStrategy SEARCH_STRATEGY_DEFAULT = SearchStrategy.BASE_CHOICE;
    public static final boolean ENABLE_CACHE_DEFAULT = true;
    public static final int MAX_TRY_PER_HOLE_DEFAULT = 1000;
    public static final int PARTITION_NUM_DEFAULT = 10;
    public static final int MAX_TRY_PER_DEPTH_DEFAULT = 10000;
    public static final Path WORKING_DIRECTORY_DEFAULT = Paths.get("");

    /*
    java
    -Xmx16g
    -Xmx16g
    -Djava.library.path="sat-solvers"
    -cp
    "arepair-1.0-jar-with-dependencies.jar:libs/aparser-1.0.jar:libs/alloy.jar"
    patcher.Patcher
    --model-path
    ".hidden/toFix.als"
    --test-path
    ".hidden/tests.als"
    --scope
    "3"
    --minimum-cost
    "3"
    --search-strategy
    "base-choice" "all-combination"
    --enable-cache
    --max-try-per-hole
    1000
    --partition-num
    10           //
    --max-try-per-depth
    10000    //
     */
    /*
    -e,--enable-cache: This argument is optional. If this argument is specified, ARepair uses the hierarchical caching for repair. Otherwise, it does not.
    -h,--max-try-per-hole: This argument is optional and is used when the search strategy is base-choice. Pass the maximum number of candidate expressions to consider for each hole during repair as the argument. If the argument is not specified, a default value of 1000 is used.
    -p,--partition-num: This argument is optional and is used when the search strategy is all-combinations. Pass the number of partitions of the search space for a given hole as the argument. If the argument is not specified, a default value of 10 is used.
    -d,--max-try-per-depth: This argument is optional and is used when the search strategy is all-combinations. Pass the maximum number of combinations of candidate expressions to consider for each depth of holes during repair as the argument. If the argument is not specified, a default value of 10000 is used.
    */
    //repair is saved at the same location as model-path inside a folder named .hidden with name fix.als
    private int memory = MEMORY_DEFAULT;
    private Path satSolvers;
    private List<Path> classpath;
    private Path workingDirectory = WORKING_DIRECTORY_DEFAULT;
    private static final String PATCHER_CLASS = "patcher.Patcher";
    private static final String FIX_FILE = ".hidden/fix.als";
    private Path modelToRepair;
    private Path testsPath;
    private SearchStrategy searchStrategy = SEARCH_STRATEGY_DEFAULT;
    private int scope = SCOPE_DEFAULT; //Alloy's scope
    private int minimumCost = MINIMUM_COST_DEFAULT; //Has to do with expression generator
    private boolean enableCache = ENABLE_CACHE_DEFAULT;
    private int maxTryPerHole = MAX_TRY_PER_HOLE_DEFAULT;
    private int partitionNum = PARTITION_NUM_DEFAULT;
    private int maxTryPerDepth = MAX_TRY_PER_DEPTH_DEFAULT;


    public ARepairResult run() {
        if (!readyToRun())
            throw new IllegalArgumentException("Missing or invalid path related arguments\n" + pathsInformation());
        return executeARepair();
    }

    public ARepair setMemory(int memory) {
        if (memory <= 0)
            throw new IllegalArgumentException("non positive memory (" + memory + ")");
        this.memory = memory;
        return this;
    }

    public ARepair setSatSolversPath(Path satSolversPath) {
        this.satSolvers = satSolversPath;
        return this;
    }

    public ARepair setClasspath(List<Path> classpath) {
        this.classpath = classpath;
        return this;
    }

    public ARepair addToClasspath(Path path) {
        this.classpath.add(path);
        return this;
    }

    public ARepair modelToRepair(Path modelToRepair) {
        this.modelToRepair = modelToRepair;
        return this;
    }

    public ARepair testsPath(Path testsPath) {
        this.testsPath = testsPath;
        return this;
    }

    public ARepair setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    public ARepair searchStrategyToUse(SearchStrategy searchStrategy) {
        if (searchStrategy == null)
            throw new IllegalArgumentException("null search strategy");
        this.searchStrategy = searchStrategy;
        return this;
    }

    public ARepair setScope(int scope) {
        if (scope <= 0)
            throw new IllegalArgumentException("non positive scope (" + scope + ")");
        this.scope = scope;
        return this;
    }

    public ARepair setMinimumCost(int minimumCost) {
        if (minimumCost <= 0)
            throw new IllegalArgumentException("non positive minimum cost (" + minimumCost + ")");
        this.minimumCost = minimumCost;
        return this;
    }

    public ARepair enableCache() {
        this.enableCache = true;
        return this;
    }

    public ARepair disableCache() {
        this.enableCache = false;
        return this;
    }

    public ARepair setMaxTryPerHole(int maxTryPerHole) {
        if (maxTryPerHole <= 0)
            throw new IllegalArgumentException("non positive max try per hole (" + maxTryPerHole + ")");
        this.maxTryPerHole = maxTryPerHole;
        return this;
    }

    public ARepair setPartitionNum(int partitionNum) {
        if (partitionNum <= 0)
            throw new IllegalArgumentException("non positive partition number (" + partitionNum + ")");
        this.partitionNum = partitionNum;
        return this;
    }

    public ARepair setMaxTryPerDepth(int maxTryPerDepth) {
        if (maxTryPerDepth <= 0)
            throw new IllegalArgumentException("non positive max try per depth (" + maxTryPerDepth + ")");
        this.maxTryPerDepth = maxTryPerDepth;
        return this;
    }

    //AUXILIARY METHODS

    private ARepairResult executeARepair() {
        ARepairResult aRepairResult;
        try {
            String[] args = getARepairCommand();
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(workingDirectory.toFile());
            File errorLog = new File("aRepairExternalError.log");
            if (errorLog.exists() && !errorLog.delete())
                throw new IllegalStateException("An error occurred while trying to delete " + errorLog.toString());
            pb.redirectError(ProcessBuilder.Redirect.appendTo(errorLog));
            File outputLog = new File("aRepairExternalOutput.log");
            if (outputLog.exists() && !outputLog.delete())
                throw new IllegalStateException("An error occurred while trying to delete " + outputLog.toString());
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outputLog));
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                aRepairResult = ARepairResult.ERROR;
                aRepairResult.message("ARepair ended with exit code " + exitCode + " but no exception was caught");
                aRepairResult.repair(null);
            } else {
                aRepairResult = checkFix();
            }
        } catch (IOException | InterruptedException  e) {
            aRepairResult = ARepairResult.ERROR;
            aRepairResult.message("An exception was caught when executing ARepair\n" + exceptionToString(e));
        }
        return aRepairResult;
    }

    private ARepairResult checkFix() {
        File repair = Paths.get(workingDirectory.toAbsolutePath().toString(), FIX_FILE).toFile();
        ARepairResult result;
        if (!repair.exists()) {
            result = ARepairResult.NOT_REPAIRED;
            result.message("No fix found in " + repair.toString());
        } else {
            result = ARepairResult.REPAIRED;
            result.message("Fix found in " + repair.toString());
            result.repair(repair.toPath());
        }
        return result;
    }

    private boolean readyToRun() {
        if (!isValidPath(Paths.get(workingDirectory.toString(), satSolvers.toString()), Utils.PathCheck.DIR))
            return false;
        if (!isValidPath(modelToRepair, Utils.PathCheck.ALS))
            return false;
        if (!isValidPath(testsPath, Utils.PathCheck.ALS))
            return false;
        if (!isValidPath(workingDirectory, Utils.PathCheck.DIR))
            return false;
        if (classpath == null)
            return false;
        for (Path p : classpath) {
            if (!isValidPath(p, Utils.PathCheck.EXISTS))
                return false;
        }
        return true;
    }

    private String pathsInformation() {
        String pinfo = "";
        pinfo += "working directory : " + (workingDirectory==null?"NULL":workingDirectory.toAbsolutePath().toString()) + "\n";
        pinfo += "sat-solvers path  : " + (satSolvers==null?"NULL":satSolvers.toString() + " (this directory is relative to the working directory)") + "\n";
        pinfo += "classpath         : " + (classpath==null?"NULL":classpath.stream().map(Path::toString).collect(Collectors.joining(","))) + "\n";
        pinfo += "model to repair   : " + (modelToRepair==null?"NULL":modelToRepair.toString()) + "\n";
        pinfo += "tests path        : " + (testsPath==null?"NULL":testsPath.toString()) + "\n";
        return pinfo;
    }

    /*
    java
    -Xmx16g
    -Xmx16g
    -Djava.library.path="sat-solvers"
    -cp
    "arepair-1.0-jar-with-dependencies.jar:libs/aparser-1.0.jar:libs/alloy.jar"
    patcher.Patcher
    --model-path
    ".hidden/toFix.als"
    --test-path
    ".hidden/tests.als"
    --scope
    "3"
    --minimum-cost
    "3"
    --search-strategy
    "base-choice" "all-combination"
    --enable-cache
    --max-try-per-hole
    1000
    --partition-num
    10           //
    --max-try-per-depth
    10000
     */

    private String[] getARepairCommand() {
        String classpath = this.classpath.isEmpty()?".":this.classpath.stream().map(Path::toString).collect(Collectors.joining(":"));
        String[] args = new String[23 + (enableCache?1:0)];
        args[0] = "java";
        args[1] = "-Xms" + memory + "g"; args[2] = "-Xmx" + memory + "g";
        args[3] = "-Djava.library.path=" + satSolvers.toString();
        args[4] = "-cp"; args[5] = classpath;
        args[6] = PATCHER_CLASS;
        args[7] = "--model-path"; args[8] = "\"" + modelToRepair.toString() + "\"";
        args[9] = "--test-path"; args[10] = "\"" + testsPath.toString() + "\"";
        args[11] = "--scope"; args[12] = Integer.toString(scope);
        args[13] = "--minimum-cost"; args[14] = Integer.toString(minimumCost);
        args[15] = "--search-strategy"; args[16] = searchStrategy.getName();
        args[17] = "--max-try-per-hole"; args[18] = Integer.toString(maxTryPerHole);
        args[19] = "--partition-num"; args[20] = Integer.toString(partitionNum);
        args[21] = "--max-try-per-depth"; args[22] = Integer.toString(maxTryPerDepth);
        if (enableCache) {
            args[23] = "--enable-cache";
        }
        return args;
    }


}
