package ar.edu.unrc.exa.dc.tools;

import ar.edu.unrc.exa.dc.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ar.edu.unrc.exa.dc.util.Utils.*;

public final class ARepair {

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


    private int memory = MEMORY_DEFAULT;
    private Path satSolvers;
    private List<Path> classpath;
    private Path workingDirectory = WORKING_DIRECTORY_DEFAULT;
    private static final String PATCHER_CLASS = "patcher.Patcher";
    private static final String AREPAIR_HIDDEN_DIR = ".hidden";
    public static final String FIX_FILE = AREPAIR_HIDDEN_DIR + "/fix.als";
    private Path modelToRepair;
    private Path testsPath;
    private SearchStrategy searchStrategy = SEARCH_STRATEGY_DEFAULT;
    private int scope = SCOPE_DEFAULT; //Alloy's scope
    private int minimumCost = MINIMUM_COST_DEFAULT; //Has to do with expression generator
    private boolean enableCache = ENABLE_CACHE_DEFAULT;
    private int maxTryPerHole = MAX_TRY_PER_HOLE_DEFAULT;
    private int partitionNum = PARTITION_NUM_DEFAULT;
    private int maxTryPerDepth = MAX_TRY_PER_DEPTH_DEFAULT;
    private boolean treatPartialRepairsAsFixes = false;


    public ARepairResult run() {
        if (!readyToRun())
            throw new IllegalArgumentException("Missing or invalid path related arguments\n" + pathsInformation());
        return executeARepair();
    }

    public void setMemory(int memory) {
        if (memory <= 0)
            throw new IllegalArgumentException("non positive memory (" + memory + ")");
        this.memory = memory;
    }

    public void setSatSolversPath(Path satSolversPath) {
        this.satSolvers = satSolversPath;
    }

    public void setClasspath(List<Path> classpath) {
        this.classpath = classpath;
    }

    public void addToClasspath(Path path) {
        this.classpath.add(path);
    }

    public void modelToRepair(Path modelToRepair) {
        this.modelToRepair = modelToRepair;
    }

    public void testsPath(Path testsPath) {
        this.testsPath = testsPath;
    }

    public void setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void searchStrategyToUse(SearchStrategy searchStrategy) {
        if (searchStrategy == null)
            throw new IllegalArgumentException("null search strategy");
        this.searchStrategy = searchStrategy;
    }

    public void setScope(int scope) {
        if (scope <= 0)
            throw new IllegalArgumentException("non positive scope (" + scope + ")");
        this.scope = scope;
    }

    public int scope() {
        return scope;
    }

    public void setMinimumCost(int minimumCost) {
        if (minimumCost <= 0)
            throw new IllegalArgumentException("non positive minimum cost (" + minimumCost + ")");
        this.minimumCost = minimumCost;
    }

    public void cache(boolean enableCache) {
        this.enableCache = enableCache;
    }

    public void setMaxTryPerHole(int maxTryPerHole) {
        if (maxTryPerHole <= 0)
            throw new IllegalArgumentException("non positive max try per hole (" + maxTryPerHole + ")");
        this.maxTryPerHole = maxTryPerHole;
    }

    public void setPartitionNum(int partitionNum) {
        if (partitionNum <= 0)
            throw new IllegalArgumentException("non positive partition number (" + partitionNum + ")");
        this.partitionNum = partitionNum;
    }

    public void setMaxTryPerDepth(int maxTryPerDepth) {
        if (maxTryPerDepth <= 0)
            throw new IllegalArgumentException("non positive max try per depth (" + maxTryPerDepth + ")");
        this.maxTryPerDepth = maxTryPerDepth;
    }

    public void treatPartialRepairsAsFixes(boolean treatPartialRepairsAsFixes) {
        this.treatPartialRepairsAsFixes = treatPartialRepairsAsFixes;
    }

    public boolean treatPartialRepairsAsFixes() {
        return this.treatPartialRepairsAsFixes;
    }

    public boolean cleanFixDirectory() {
        Path hiddenDir = Paths.get(workingDirectory.toAbsolutePath().toString(), AREPAIR_HIDDEN_DIR);
        try {
            Utils.deleteFolderAndItsContent(hiddenDir);
        } catch (IOException e) {
            System.err.println("Let's hope this error does not represents a problem");
            e.printStackTrace();
        }
        return hiddenDir.toFile().mkdir();
    }

    //AUXILIARY METHODS

    private static final File aRepairStdOut = new File("aRepairExternalOutput.log");
    private static final File aRepairStdErr = new File("aRepairExternalError.log");
    private static final String NO_FIX_FOUND = "[INFO] Cannot fix the model";
    private static final String FIX_FOUND = "[INFO] Fixed by";
    private static final String ALL_TESTS_PASS = "[INFO] All tests pass";

    private ARepairResult executeARepair() {
        ARepairResult aRepairResult;
        try {
            String[] args = getARepairCommand();
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(workingDirectory.toFile());
            File errorLog = aRepairStdErr;
            if (errorLog.exists() && !errorLog.delete())
                throw new IllegalStateException("An error occurred while trying to delete " + errorLog);
            pb.redirectError(ProcessBuilder.Redirect.appendTo(errorLog));
            File outputLog = aRepairStdOut;
            if (outputLog.exists() && !outputLog.delete())
                throw new IllegalStateException("An error occurred while trying to delete " + outputLog);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outputLog));
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                aRepairResult = ARepairResult.ERROR;
                aRepairResult.message("ARepair ended with exit code " + exitCode + " but no exception was caught");
                aRepairResult.repair(null);
                if (findNullPointerExceptionInLog(errorLog.toPath()))
                    aRepairResult.npeFound();
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
            result.message("No fix file found in " + repair);
            return result;
        }
        Optional<String> fixNotFound;
        Optional<String> fixFound;
        Optional<String> allTestsPass;
        try {
            fixNotFound = findStringInFile(aRepairStdOut.toPath(), NO_FIX_FOUND);
            fixFound = findStringInFile(aRepairStdOut.toPath(), FIX_FOUND);
            allTestsPass = findStringInFile(aRepairStdOut.toPath(), ALL_TESTS_PASS);
        } catch (IOException e) {
            result = ARepairResult.ERROR;
            result.message("Error while reading output log:\n" + exceptionToString(e));
            return result;
        }
        if (fixNotFound.isPresent() && !treatPartialRepairsAsFixes) {
            result = ARepairResult.NOT_REPAIRED;
            result.message("No fix found");
        } else if (fixFound.isPresent() || allTestsPass.isPresent()) {
            if (fixNotFound.isPresent()){
                result = ARepairResult.PARTIAL_REPAIR;
                result.message("Fix is only a partial fix");
            } else if (!fixFound.isPresent()) {
                result = ARepairResult.REPAIRED;
                result.message("All tests passed with no modifications required");
            } else {
                result = ARepairResult.REPAIRED;
                String repairFoundBy = fixFound.get().replace(FIX_FOUND, "");
                result.message("Fix found (" + repairFoundBy + ") in " + repair);
            }
            result.repair(repair.toPath());
        } else if (fixNotFound.isPresent()) {
            result = ARepairResult.NOT_REPAIRED;
            result.message("No fix found");
        } else {
            result = ARepairResult.ERROR;
            result.message("No 'fix found'/'fix not found' line found in ARepair's output log");
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
        pinfo += "sat-solvers path  : " + (satSolvers==null?"NULL": satSolvers + " (this directory is relative to the working directory)") + "\n";
        pinfo += "classpath         : " + (classpath==null?"NULL":classpath.stream().map(Path::toString).collect(Collectors.joining(","))) + "\n";
        pinfo += "model to repair   : " + (modelToRepair==null?"NULL":modelToRepair.toString()) + "\n";
        pinfo += "tests path        : " + (testsPath==null?"NULL":testsPath.toString()) + "\n";
        return pinfo;
    }

    public String aRepairCommandToString() {
        return String.join(" ", getARepairCommand());
    }

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
