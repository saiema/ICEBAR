package ar.edu.unrc.exa.dc.search;

import ar.edu.unrc.exa.dc.icebar.Report;
import ar.edu.unrc.exa.dc.icebar.properties.ICEBARProperties;
import ar.edu.unrc.exa.dc.icebar.properties.ICEBARProperties.IcebarSearchAlgorithm;
import ar.edu.unrc.exa.dc.openai.managers.SuggestionManager;
import ar.edu.unrc.exa.dc.tools.*;
import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest;
import ar.edu.unrc.exa.dc.util.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static ar.edu.unrc.exa.dc.util.Utils.*;

public class IterativeCEBasedAlloyRepair {

    private final Logger logger;

    private final ARepair aRepair;
    private final BeAFix beAFix;
    private final Set<BeAFixTest> trustedCounterexampleTests;
    private final ModelToRepair modelToRepair;
    private final int laps;
    private int totalTestsGenerated;

    public int totalTestsGenerated() {
        return totalTestsGenerated;
    }

    private int arepairCalls;

    public int arepairCalls() {
        return arepairCalls;
    }

    private int evaluatedCandidates;
    private int evaluatedCandidatesLeadingToNoFix;
    private int evaluatedCandidatesLeadingToSpurious;
    private TestHashes trustedTests;
    private TestHashes untrustedTests;

    private IcebarSearchAlgorithm search = IcebarSearchAlgorithm.DFS;
    private final TimeCounter arepairTimeCounter = new TimeCounter();
    private final TimeCounter beafixTimeCounter = new TimeCounter();
    private final TimeCounter totalTime = new TimeCounter();

    public TimeCounter arepairTimeCounter() {
        return arepairTimeCounter;
    }

    public TimeCounter beafixTimeCounter() {
        return beafixTimeCounter;
    }

    public TimeCounter totalTime() {
        return totalTime;
    }

    public void setSearch(IcebarSearchAlgorithm search) {
        this.search = search;
    }

    private boolean allowFactsRelaxation = false;
    public void allowFactsRelaxation(boolean allowFactsRelaxation) { this.allowFactsRelaxation = allowFactsRelaxation; }

    private boolean globalTrustedTests = true;
    public void globalTrustedTests(boolean globalTrustedTests) { this.globalTrustedTests = globalTrustedTests; }

    private boolean forceAssertionGeneration = false;
    public void forceAssertionGeneration(boolean forceAssertionGeneration) { this.forceAssertionGeneration = forceAssertionGeneration; }

    private long timeout = 0;
    public void timeout(long timeout) { this.timeout = timeout; }

    private boolean keepGoingAfterARepairNPE = false;
    public void keepGoingAfterARepairNPE(boolean keepGoingAfterARepairNPE) { this.keepGoingAfterARepairNPE =keepGoingAfterARepairNPE; }

    private boolean enableOpenAISuggestions = false;
    public void enableOpenAISuggestions(boolean enableOpenAISuggestions) { this.enableOpenAISuggestions = enableOpenAISuggestions; }

    public IterativeCEBasedAlloyRepair(Path modelToRepair, Path oracle, ARepair aRepair, BeAFix beAFix, int laps, Logger logger) {
        if (!isValidPath(modelToRepair, Utils.PathCheck.ALS))
            throw new IllegalArgumentException("Invalid model to repair path (" + (modelToRepair==null?"NULL":modelToRepair.toString()) + ")");
        if (!isValidPath(oracle, Utils.PathCheck.ALS))
            throw new IllegalArgumentException("Invalid oracle path (" + (oracle==null?"NULL":oracle.toString()) + ")");
        if (aRepair == null)
            throw new IllegalArgumentException("null ARepair instance");
        if (beAFix == null)
            throw new IllegalArgumentException("null BeAFix instance");
        if (laps < 0)
            throw new IllegalArgumentException("Negative value for laps");
        this.logger = logger;
        this.modelToRepair = new ModelToRepair(modelToRepair, oracle);
        this.aRepair = aRepair;
        this.beAFix = beAFix;
        this.trustedCounterexampleTests = new HashSet<>();
        this.laps = laps;
        this.totalTestsGenerated = 0;
        this.arepairCalls = 0;
        this.evaluatedCandidates = 0;
        this.evaluatedCandidatesLeadingToNoFix = 0;
        this.evaluatedCandidatesLeadingToSpurious = 0;
    }

    private InitialTests initialTests;
    public void setInitialTests(InitialTests initialTests) { this.initialTests = initialTests; }

    private boolean usePrioritization = false;
    public void usePrioritization(boolean usePrioritization) {
        this.usePrioritization = usePrioritization;
    }

    public boolean justRunningARepairOnce() {
        return laps == 0;
    }

    private CandidateSpace searchSpace = null;

    public Optional<FixCandidate> repair() throws IOException {
        //watches for different time process recording
        logger.info("Starting ICEBAR process with:\n" +
                "\tModel: " + modelToRepair.path().toString() + "\n" +
                "\tProperty-based Oracle: " + modelToRepair.oraclePath().toString() + "\n" +
                "\tInitial tests: " + (initialTests==null?"NONE":initialTests.getInitialTestsPath().toString()) + "\n" +
                "\tLaps: " + laps + "\n");
        logger.fine("Full ICEBAR configuration:\n\t" +
                String.join("\n\t", ICEBARProperties.getInstance().getAllRawProperties()));
        initializeSearchSpaces();
        int maxReachedLap = 0;
        trustedTests = new TestHashes();
        untrustedTests = new TestHashes();
        totalTime.clockStart();
        FixCandidate current;
        while ((current = nextCandidate()) != null) {
            evaluatedCandidates++;
            maxReachedLap = Math.max(maxReachedLap, current.depth());
            ARepairResult aRepairResult = runARepair(arepairTimeCounter, current);
            logger.info("Running ARepair ended with " +
                    (aRepairResult.equals(ARepairResult.NO_TESTS)?
                            "NO TESTS (will consider as spurious fix to bootstrap ICEBAR process)"
                            :aRepairResult.hasRepair()?
                                "FIX"
                                :"NO FIX"
                    )
            );
            if (aRepairResult.equals(ARepairResult.ERROR)) {
                logger.severe("ARepair call ended in error:\n" + aRepairResult.message());
                if (aRepairResult.nullPointerExceptionFound() && keepGoingAfterARepairNPE) {
                    logger.warning("ARepair ended with a NullPointerException but we are going to ignore that and hope for the best");
                    continue;
                }
                Report report = Report.arepairFailed(current, current.untrustedTests().size() + current.trustedTests().size() + trustedCounterexampleTests.size(), arepairTimeCounter, beafixTimeCounter, arepairCalls, generateTestsAndCandidateCounters());
                writeReport(report);
                return Optional.empty();
            }
            boolean repairFound = aRepairResult.hasRepair();
            boolean noTests = aRepairResult.equals(ARepairResult.NO_TESTS);
            boolean checkAndGenerate = repairFound || noTests;
            if (checkAndGenerate) {
                boolean fromOriginal = aRepairResult.equals(ARepairResult.NO_TESTS);
                FixCandidate repairCandidate;
                if (fromOriginal) {
                    repairCandidate = current;
                } else {
                    ModelToRepair aRepairFix = new ModelToRepair(aRepairResult.repair(), current.modelToRepair().oraclePath());
                    repairCandidate = FixCandidate.aRepairCheckCandidate(aRepairFix, current.depth());
                }
                BeAFixResult beAFixCheckResult = runBeAFixCheck(beafixTimeCounter, repairCandidate, repairCandidate.modelToRepair().oraclePath());
                logger.info("Validating ARepair fix against property-based oracle: DOES" + (beAFixCheckResult.checkResult()?"":" NOT") + " SATISFIES ORACLE");
                int repairedPropertiesForCurrent = beAFixCheckResult.passingProperties();
                Pair<Boolean, FixCandidate> beAFixCheckAnalysis = analyzeBeAFixCheck(beAFixCheckResult, beafixTimeCounter, arepairTimeCounter, totalTime, current, repairCandidate);
                if (beAFixCheckAnalysis.fst())
                    return Optional.ofNullable(beAFixCheckAnalysis.snd());
                saveFailingTestSuite(aRepairResult.usedTests(), repairCandidate.modelName(), false);
                if (current.depth() < laps) {
                    if (enableOpenAISuggestions) {
                        Pair<SuggestionResult, FixCandidate> suggestion = updateModelWithSuggestion(beAFixCheckResult, repairCandidate);
                        switch (suggestion.fst()) {
                            case SUGGESTION_IS_FIX: {
                                logger.info("OpenAI suggestion fixed the model.");
                                return Optional.ofNullable(suggestion.snd());
                            }
                            case SUGGESTION_IS_BETTER: {
                                logger.info("OpenAI suggestion is better than current model, changing to suggestion.");
                                repairCandidate = suggestion.snd();
                                break;
                            }
                            case SUGGESTION_KEEP_ORIGINAL: {
                                logger.info("OpenAI suggestion is worse or non existent, keeping current candidate and model-to-repair.");
                                break;
                            }
                            case SUGGESTION_CREATES_BRANCH: {
                                logger.info("OpenAI suggestion will create a new ICEBAR branch with a different model-to-repair.");
                                searchSpace.push(suggestion.snd());
                                break;
                            }
                        }
                    }
                    beafixTimeCounter.clockStart();
                    BeAFixResult beAFixResult = runBeAFixWithCurrentConfig(repairCandidate, BeAFixMode.TESTS, false, false, repairCandidate.modelToRepair().oraclePath());
                    beafixTimeCounter.clockEnd();
                    if (checkIfInvalidAndReportBeAFixResults(beAFixResult, current, beafixTimeCounter, arepairTimeCounter))
                        return Optional.empty();
                    logger.info("Generated " + beAFixResult.generatedTests() + " tests from spurious fix");
                    List<BeAFixTest> counterexampleTests = beAFixResult.getCounterexampleTests();
                    List<BeAFixTest> counterexampleUntrustedTests = beAFixResult.getCounterExampleUntrustedTests();
                    List<BeAFixTest> predicateTests = beAFixResult.getPredicateTests();
                    List<BeAFixTest> relaxedPredicateTests = null;
                    List<BeAFixTest> relaxedAssertionsTests = null;
                    if (allowFactsRelaxation && ((counterexampleTests.isEmpty() && counterexampleUntrustedTests.isEmpty())) && predicateTests.isEmpty()) {
                        logger.fine("No tests available, generating with relaxed facts...");
                        beafixTimeCounter.clockStart();
                        beAFixResult = runBeAFixWithCurrentConfig(repairCandidate, BeAFixMode.TESTS, true, false, repairCandidate.modelToRepair().oraclePath());
                        beafixTimeCounter.clockEnd();
                        if (checkIfInvalidAndReportBeAFixResults(beAFixResult, current, beafixTimeCounter, arepairTimeCounter))
                            return Optional.empty();
                        logger.info("Generated " + beAFixResult.generatedTests() + " tests from spurious fix by relaxing facts");
                        relaxedPredicateTests = beAFixResult.getPredicateTests();
                        if (forceAssertionGeneration) {
                            logger.fine("Generating with assertion forced test generation...");
                            beAFix.testsStartingIndex(Math.max(beAFix.testsStartingIndex(), beAFixResult.getMaxIndex()) + 1);
                            beafixTimeCounter.clockStart();
                            BeAFixResult beAFixResult_forcedAssertionTestGeneration = runBeAFixWithCurrentConfig(repairCandidate, BeAFixMode.TESTS, false, true, repairCandidate.modelToRepair().oraclePath());
                            beafixTimeCounter.clockEnd();
                            if (checkIfInvalidAndReportBeAFixResults(beAFixResult_forcedAssertionTestGeneration, current, beafixTimeCounter, arepairTimeCounter))
                                return Optional.empty();
                            logger.info("Generated " + beAFixResult_forcedAssertionTestGeneration.generatedTests() + " tests from spurious fix by forcing generation from assertions");
                            relaxedAssertionsTests = beAFixResult_forcedAssertionTestGeneration.getCounterExampleUntrustedTests();
                        }
                    }
                    boolean trustedTestsAdded;
                    boolean addLocalTrustedTests;
                    boolean globalTestsAdded = false;
                    if (globalTrustedTests || (current.untrustedTests().isEmpty() && current.trustedTests().isEmpty())) {
                        trustedTestsAdded = this.trustedCounterexampleTests.addAll(counterexampleTests);
                        globalTestsAdded = trustedTestsAdded;
                        addLocalTrustedTests = false;
                    } else { //local trusted tests except from original
                        trustedTestsAdded = !counterexampleTests.isEmpty();
                        addLocalTrustedTests = true;
                    }
                    int newBranches = 0;
                    if (!counterexampleTests.isEmpty()) {
                        Set<BeAFixTest> localTrustedTests = new HashSet<>(current.trustedTests());
                        Set<BeAFixTest> localUntrustedTests = new HashSet<>(current.untrustedTests());
                        if (addLocalTrustedTests) {
                            localTrustedTests.addAll(counterexampleTests);
                        }
                        if (trustedTestsAdded) {
                            FixCandidate newCandidate = FixCandidate.descendant(modelToRepair, localUntrustedTests, localTrustedTests, current);
                            newCandidate.repairedProperties(repairedPropertiesForCurrent);
                            if (newCandidate.hasLocalTests() || globalTestsAdded) {
                                searchSpace.push(newCandidate);
                                newBranches = 1;
                                counterexampleTests.forEach(trustedTests::addHash);
                            } else {
                                logger.warning("Candidate " + newCandidate.id() + " is invalid (no new tests could be added)");
                            }
                        }
                    }
                    boolean counterexamples = !counterexampleTests.isEmpty();
                    boolean untrustedCounterexamples = !counterexampleUntrustedTests.isEmpty();
                    boolean predicates = !predicateTests.isEmpty();
                    boolean relaxedPredicates = relaxedPredicateTests != null && !relaxedPredicateTests.isEmpty();
                    boolean relaxedAssertions = relaxedAssertionsTests != null && !relaxedAssertionsTests.isEmpty();
                    if ((!counterexamples) && untrustedCounterexamples) {
                        if ((newBranches = createBranches(current, counterexampleUntrustedTests, true, searchSpace, repairedPropertiesForCurrent)) == BRANCHING_ERROR) {
                            logger.severe("Branching error!");
                            return Optional.empty();
                        }
                    }
                    if (((!counterexamples && !untrustedCounterexamples)) && predicates) {
                        if ((newBranches = createBranches(current, predicateTests, false, searchSpace, repairedPropertiesForCurrent)) == BRANCHING_ERROR) {
                            logger.severe("Branching error!");
                            return Optional.empty();
                        }
                    }
                    if (((!counterexamples && !untrustedCounterexamples && !predicates)) && relaxedPredicates) {
                        if ((newBranches = createBranches(current, relaxedPredicateTests, false, searchSpace, repairedPropertiesForCurrent)) == BRANCHING_ERROR) {
                            logger.severe("Branching error!");
                            return Optional.empty();
                        }
                    }
                    if (((!counterexamples && !untrustedCounterexamples && !predicates && !relaxedPredicates)) && relaxedAssertions) {
                        if ((newBranches = createBranches(current, relaxedAssertionsTests, false, searchSpace, repairedPropertiesForCurrent)) == BRANCHING_ERROR) {
                            logger.severe("Branching error!");
                            return Optional.empty();
                        }
                    }
                    totalTestsGenerated += beAFixResult.generatedTests() + (relaxedPredicateTests==null?0:relaxedPredicateTests.size()) + (relaxedAssertionsTests==null?0:relaxedAssertionsTests.size());
                    logger.info("Total tests generated: " + totalTestsGenerated);
                    logger.info("Generated branches: " + newBranches);
                    beAFix.testsStartingIndex(Math.max(beAFix.testsStartingIndex(), beAFixResult.getMaxIndex()) + 1);
                } else if (!justRunningARepairOnce()) {
                    logger.info("max laps reached (" + laps + "), ending branch");
                }
            } else {
                evaluatedCandidatesLeadingToNoFix++;
                saveFailingTestSuite(aRepairResult.usedTests(), current.modelName(), true);
                if (aRepairResult.hasMessage()) {
                    logger.fine("ARepair ended with the following message:\n" + aRepairResult.message());
                }
            }
            if (justRunningARepairOnce() && !noTests && !repairFound) {
                logger.info("ICEBAR running ARepair once could not find a fix");
                Report report = Report.arepairOnceNoFixFound(totalTestsGenerated, beafixTimeCounter, arepairTimeCounter, arepairCalls, generateTestsAndCandidateCounters());
                writeReport(report);
                return Optional.empty();
            }
            if (justRunningARepairOnce() && !noTests && repairFound) {
                logger.info("ICEBAR running ARepair once found a spurious fix");
                Report report = Report.arepairOnceSpurious(totalTestsGenerated, beafixTimeCounter, arepairTimeCounter, arepairCalls, generateTestsAndCandidateCounters());
                writeReport(report);
                return Optional.empty();
            }
        }
        logger.info("ICEBAR ended with no more candidates");
        Report report = Report.exhaustedSearchSpace(maxReachedLap, totalTestsGenerated, beafixTimeCounter, arepairTimeCounter, arepairCalls, generateTestsAndCandidateCounters());
        writeReport(report);
        return Optional.empty();
    }

    private SuggestionManager suggestionManager = null;

    private enum SuggestionResult {
        SUGGESTION_IS_FIX,
        SUGGESTION_IS_BETTER,
        SUGGESTION_KEEP_ORIGINAL,
        SUGGESTION_CREATES_BRANCH
    };
    private Pair<SuggestionResult, FixCandidate> updateModelWithSuggestion(BeAFixResult beAFixCheckResult, FixCandidate repairCandidate) {
        logger.info("Asking for OpenAI suggestion");
        if (suggestionManager == null) {
            initSuggestionManager(repairCandidate.modelToRepair().path().getParent());
        }
        try {
            Optional<Path> suggestedFix = suggestionManager.askForSuggestion(repairCandidate.modelToRepair());
            if (suggestedFix.isEmpty()) {
                logger.info("No suggestion obtained, continuing with ARepair fix and the original model");
            } else {
                logger.info("Got suggestion, saved at " + suggestedFix.get());
                ModelToRepair suggestedModel = new ModelToRepair(suggestedFix.get(), repairCandidate.modelToRepair().oraclePath());
                FixCandidate suggestedFixCandidate = FixCandidate.aRepairCheckCandidate(suggestedModel, repairCandidate.depth());
                BeAFixResult suggestionBeAFixCheckResult = runBeAFixCheck(beafixTimeCounter, suggestedFixCandidate, suggestedFixCandidate.modelToRepair().oraclePath());
                logger.info("Validating OpenAI suggestion against property-based oracle: DOES" + (suggestionBeAFixCheckResult.checkResult()?"":" NOT") + " SATISFIES ORACLE");
                Pair<Boolean, FixCandidate> beAFixCheckAnalysis = analyzeBeAFixCheck(beAFixCheckResult, beafixTimeCounter, arepairTimeCounter, totalTime, suggestedFixCandidate, repairCandidate);
                if (beAFixCheckAnalysis.fst())
                    return new Pair<>(SuggestionResult.SUGGESTION_IS_FIX, beAFixCheckAnalysis.snd());
                List<BeAFixTest> tests = new LinkedList<>();
                if (initialTests != null && !initialTests.getInitialTests().isEmpty()) {
                    tests.addAll(initialTests.getInitialTests());
                } else if (!trustedCounterexampleTests.isEmpty()) {
                    tests.addAll(trustedCounterexampleTests);
                } else {
                    tests.addAll(repairCandidate.trustedTests());
                    tests.addAll(repairCandidate.untrustedTests());
                }
                boolean changeToSuggestion = false;
                int repairedPropertiesForCurrent = 0;
                int suggestionRepairedProperties = 0;
                boolean usingTests = false;
                if (tests.isEmpty()) {
                    repairedPropertiesForCurrent = beAFixCheckResult.passingProperties();
                    suggestionRepairedProperties = suggestionBeAFixCheckResult.passingProperties();
                } else {
                    usingTests = true;
                    BeAFixResult testCheckCurrentCandidate = runBeAFixTestsCheck(beafixTimeCounter, repairCandidate, tests);
                    BeAFixResult testCheckSuggestedCandidate = runBeAFixTestsCheck(beafixTimeCounter, suggestedFixCandidate, tests);
                    if (testCheckCurrentCandidate.error() || testCheckSuggestedCandidate.error()) {
                        logger.warning("Test check failed for at least one candidate " +
                                "[Current : " + (testCheckCurrentCandidate.error()?"FAILED":"OK") + "]" +
                                "[Suggested : " + (testCheckSuggestedCandidate.error()?"FAILED":"OK") + "]" +
                                "\nWill continue working with current fix candidate");
                    } else {
                        repairedPropertiesForCurrent = testCheckCurrentCandidate.passingProperties();
                        suggestionRepairedProperties = testCheckSuggestedCandidate.passingProperties();
                    }
                }
                logger.info("Suggested model make " +
                        (suggestionRepairedProperties>repairedPropertiesForCurrent?"MORE":"SAME OR LESS") +
                        " " + (usingTests?"TEST":"ORACLE") + " properties than the ARepair fix " +
                        "(" + suggestionRepairedProperties + " satisfied properties for the suggestion " +
                        "VS " + repairedPropertiesForCurrent + " satisfied properties for the ARepair fix" + ")");
                if (suggestionRepairedProperties > repairedPropertiesForCurrent) {
                    changeToSuggestion = true;
                }
                if (changeToSuggestion && usingTests) {
                    logger.info("Changing current buggy model to OpenAI suggestion");
                    return new Pair<>(SuggestionResult.SUGGESTION_IS_BETTER, beAFixCheckAnalysis.snd());
                } else if (changeToSuggestion) {
                    logger.info("Creating new branch with OpenAI suggestion");
                    return new Pair<>(SuggestionResult.SUGGESTION_CREATES_BRANCH, beAFixCheckAnalysis.snd());
                }
            }
            return new Pair<>(SuggestionResult.SUGGESTION_KEEP_ORIGINAL, repairCandidate);
        } catch (IOException e) {
            logger.severe(
                    "An error occurred while trying to get a suggestion" +
                            "(will continue with the original model) :\n" + Utils.exceptionToString(e));
            return new Pair<>(SuggestionResult.SUGGESTION_KEEP_ORIGINAL, repairCandidate);
        }
    }

    private void initSuggestionManager(Path suggestionsFolder) {
        Dotenv dotenv = Dotenv.configure().filename(ICEBARProperties.getInstance().icebarOpenAIEnvFile().toString()).load();
        int maximumContent = Integer.parseInt(dotenv.get("ICEBAR_OPENAI_CONTEXT_MAX_MESSAGES", "1"));
        suggestionManager = new SuggestionManager(suggestionsFolder, maximumContent);
        logger.info("Initialized SuggestionManager with:" +
                "\n\tSuggestions folder: " + suggestionsFolder +
                "\n\tMaximum suggestion memory: " + maximumContent);
    }

    private Pair<Boolean, FixCandidate> analyzeBeAFixCheck(BeAFixResult beAFixCheckResult, TimeCounter beafixTimeCounter, TimeCounter arepairTimeCounter, TimeCounter totalTime, FixCandidate current, FixCandidate repairCandidate) throws IOException {
        totalTime.updateTotalTime();
        if (beAFixCheckResult.error()) {
            logger.severe("BeAFix check ended in error, ending search");
            Report report = Report.beafixCheckFailed(current, current.untrustedTests().size() + current.trustedTests().size() + trustedCounterexampleTests.size(), beafixTimeCounter, arepairTimeCounter, arepairCalls, generateTestsAndCandidateCounters());
            writeReport(report);
            return new Pair<>(false, null);
        } else if (beAFixCheckResult.checkResult()) {
            logger.fine("BeAFix validated the repair, fix found");
            Report report = Report.repairFound(current, current.untrustedTests().size() + current.trustedTests().size() + trustedCounterexampleTests.size(), beafixTimeCounter, arepairTimeCounter, arepairCalls, generateTestsAndCandidateCounters());
            writeReport(report);
            return new Pair<>(true, repairCandidate);
        } else {
            logger.fine("BeAFix found the model to be invalid, generate tests and continue searching");
            evaluatedCandidatesLeadingToSpurious++;
            if (current.depth() < laps) {
                if (timeout > 0) {
                    if (totalTime.toMinutes() >= timeout) {
                        logger.fine("ICEBAR timeout (" + timeout + " minutes) reached");
                        Report report = Report.timeout(current, current.untrustedTests().size() + current.trustedTests().size() + trustedCounterexampleTests.size(), beafixTimeCounter, arepairTimeCounter, arepairCalls, generateTestsAndCandidateCounters());
                        writeReport(report);
                        return new Pair<>(true, null);
                    }
                }
            }
            return new Pair<>(false, null);
        }
    }

    private BeAFixResult runBeAFixTestsCheck(TimeCounter beafixTimeCounter, FixCandidate repairCandidate, List<BeAFixTest> tests) {
        Path testsPath = Paths.get(repairCandidate.modelToRepair().path().toAbsolutePath().toString().replace(".als", "_tests.als"));
        int testsCount;
        try {
            testsCount = generateTestsFile(testsPath, tests);
            if (testsCount < 0) {
                logger.severe("Couldn't delete tests file (" + testsPath + ")");
                return BeAFixResult.error("Couldn't delete tests file (" + testsPath + ")");
            } else if (testsCount == 0) {
                logger.severe("No tests to run");
                return BeAFixResult.error("No tests to run");
            } else {
                return runBeAFixCheck(beafixTimeCounter, repairCandidate, testsPath);
            }
        } catch (IOException e) {
            String exceptionAsString = Utils.exceptionToString(e);
            logger.severe("Exception occurred while checking model against tests\n" + exceptionAsString);
            return BeAFixResult.error(exceptionAsString);
        }

    }

    private BeAFixResult runBeAFixCheck(TimeCounter beafixTimeCounter, FixCandidate repairCandidate, Path oracle) {
        logger.fine("Validating current candidate with BeAFix");
        beafixTimeCounter.clockStart();
        BeAFixResult beAFixCheckResult = runBeAFixWithCurrentConfig(repairCandidate, BeAFixMode.CHECK, false, false, oracle);
        beafixTimeCounter.clockEnd();
        logger.fine( "BeAFix check finished\n" + beAFixCheckResult.toString());
        return beAFixCheckResult;
    }

    private ARepairResult runARepair(TimeCounter arepairTimeCounter, FixCandidate current) throws IOException {
        logger.fine("Repairing current candidate\n" + current);
        arepairTimeCounter.clockStart();
        ARepairResult aRepairResult = runARepairWithCurrentConfig(current);
        arepairTimeCounter.clockEnd();
        writeCandidateInfo(current, trustedCounterexampleTests, aRepairResult);
        logger.fine("ARepair finished\n" + aRepairResult.toString());
        return aRepairResult;
    }

    private void initializeSearchSpaces() {
        if (search.equals(IcebarSearchAlgorithm.DFS)) {
            searchSpace = usePrioritization ?CandidateSpace.priorityStack():CandidateSpace.normalStack();
        } else if (search.equals(IcebarSearchAlgorithm.BFS)) {
            searchSpace = usePrioritization ?CandidateSpace.priorityQueue():CandidateSpace.normalQueue();
        } else {
            throw new IllegalStateException("Search mode unavailable (" + search + ")");
        }
        FixCandidate originalCandidate = FixCandidate.initialCandidate(modelToRepair);
        searchSpace.push(originalCandidate);
    }

    private FixCandidate nextCandidate() {
        if (!searchSpace.isEmpty())
            return searchSpace.pop();
        return null;
    }


    private static final int BRANCHING_ERROR = -1; //TODO: currently not in use
    private int createBranches(FixCandidate current, List<BeAFixTest> fromTests, boolean multipleBranches, CandidateSpace searchSpace, int repairedPropertiesForCurrent) {
        int branches = 0;
        for (List<BeAFixTest> combination : createBranchesCombinations(fromTests, multipleBranches)) {
            if (combination.isEmpty())
                continue;
            Set<BeAFixTest> localTrustedTests = new HashSet<>(current.trustedTests());
            Set<BeAFixTest> localUntrustedTests = new HashSet<>(current.untrustedTests());
            localUntrustedTests.addAll(combination);
            FixCandidate newCandidate = FixCandidate.descendant(current.modelToRepair(), localUntrustedTests, localTrustedTests, current);
            newCandidate.repairedProperties(repairedPropertiesForCurrent);
            if (newCandidate.hasLocalTests()) {
                searchSpace.push(newCandidate);
                branches++;
                combination.forEach(untrustedTests::addHash);
            } else {
                logger.warning("Candidate " + newCandidate.id() + " is invalid (no new tests could be added)");
            }
        }
        return branches;
    }

    private List<List<BeAFixTest>> createBranchesCombinations(List<BeAFixTest> fromTests, boolean multipleBranches) {
        List<List<BeAFixTest>> allCombinations = new LinkedList<>();
        allCombinations.add(new LinkedList<>());
        for (BeAFixTest branchingTest : fromTests) {
            boolean oneBranch = false;
            if ((multipleBranches && !branchingTest.isMultipleBranch()) || (!multipleBranches && !branchingTest.isPositiveAndNegativeBranch())) {
                logger.severe((multipleBranches?"An untrusted counterexample test must have at least two alternate cases":"A predicate test must have a positive and negative branch") +
                        "\nThis could be caused by a repeated branch, could occur for unexpected instance tests, we will generate only one branch, but you should check the hashes log");
                oneBranch = true;
            }
            if (oneBranch) {
                for (List<BeAFixTest> combination : allCombinations) {
                    combination.add(branchingTest);
                }
            } else if (multipleBranches) {
                List<List<BeAFixTest>> newCombinations = new LinkedList<>();
                for (List<BeAFixTest> combination : allCombinations) {
                    for (BeAFixTest branch : branchingTest.getAlternateBranches()) {
                        List<BeAFixTest> newCombination = new LinkedList<>(combination);
                        newCombination.add(branch);
                        newCombinations.add(newCombination);
                    }
                }
                allCombinations.clear();
                allCombinations.addAll(newCombinations);
            } else {
                List<List<BeAFixTest>> newCombinations = new LinkedList<>();
                OneTypePair<BeAFixTest> positiveAndNegativeBranch = branchingTest.getPositiveAndNegativeBranches();
                BeAFixTest positive = positiveAndNegativeBranch.fst();
                BeAFixTest negative = positiveAndNegativeBranch.snd();
                for (List<BeAFixTest> combination : allCombinations) {
                    updateCombinations(newCombinations, positive, combination);
                    updateCombinations(newCombinations, negative, combination);
                }
                allCombinations.clear();
                allCombinations.addAll(newCombinations);
            }
        }
        return allCombinations;
    }

    private void updateCombinations(List<List<BeAFixTest>> newCombinations, BeAFixTest currentBranch, List<BeAFixTest> combination) {
        if (currentBranch.isMultipleBranch()) {
            for (BeAFixTest positiveBranch : currentBranch.getAlternateBranches()) {
                List<BeAFixTest> newCombination = new LinkedList<>(combination);
                newCombination.add(positiveBranch);
                newCombinations.add(newCombination);
            }
        } else {
            List<BeAFixTest> newCombination = new LinkedList<>(combination);
            newCombination.add(currentBranch);
            newCombinations.add(newCombination);
        }
    }

    private boolean checkIfInvalidAndReportBeAFixResults(BeAFixResult beAFixResult, FixCandidate current, TimeCounter beafixTimeCounter, TimeCounter arepairTimeCounter) throws IOException {
        if (!beAFixResult.error()) {
            String beafixMsg = "BeAFix finished\n";
            if (!beAFixResult.isCheck()) {
                beAFixResult.parseAllTests();
            }
            beafixMsg += beAFixResult + "\n";
            logger.fine(beafixMsg);
            return false;
        } else {
            logger.severe("BeAFix test generation ended in error, ending search");
            Report report = Report.beafixGenFailed(current, totalTestsGenerated, beafixTimeCounter, arepairTimeCounter, arepairCalls, generateTestsAndCandidateCounters());
            writeReport(report);
            return true;
        }
    }

    private int generateTestsFile(Path testsPath, @NotNull List<BeAFixTest> tests) throws IOException {
        if (tests.isEmpty()) {
            return 0;
        }
        File testsFile = testsPath.toFile();
        if (testsFile.exists()) {
            if (!testsFile.delete()) {
                return -1;
            }
        }
        return Utils.writeTestsFile(tests, testsPath);
    }

    private ARepairResult runARepairWithCurrentConfig(FixCandidate candidate) {
        if (!aRepair.cleanFixDirectory())
            logger.warning("There was a problem cleaning ARepair .hidden folder, will keep going (cross your fingers)");
        List<BeAFixTest> tests = new LinkedList<>(trustedCounterexampleTests);
        tests.addAll(candidate.untrustedTests());
        tests.addAll(candidate.trustedTests());
        if (tests.isEmpty() && (initialTests == null || initialTests.getInitialTests().isEmpty()))
            return ARepairResult.NO_TESTS;
        if (initialTests != null) {
            tests.addAll(0, initialTests.getInitialTests());
        }
        aRepair.modelToRepair(candidate.modelToRepair().path());
        Path testsPath = Paths.get(candidate.modelToRepair().path().toAbsolutePath().toString().replace(".als", "_tests.als"));
        int testCount;
        try {
            testCount = generateTestsFile(testsPath, tests);
            if (testCount < 0) {
                logger.severe("Couldn't delete tests file (" + testsPath + ")");
                ARepairResult error = ARepairResult.ERROR;
                error.message("Couldn't delete tests file (" + testsPath + ")");
                return error;
            }
        } catch (IOException e) {
            logger.severe("An exception occurred while trying to generate tests file\n" + Utils.exceptionToString(e) + "\n");
            ARepairResult error = ARepairResult.ERROR;
            error.message(Utils.exceptionToString(e));
            return error;
        }
        logger.fine("Running ARepair with " + testCount + " tests");
        writeTestsToLog(tests, logger);
        aRepair.testsPath(testsPath);
        logger.fine("Executing ARepair:\n" + aRepair.aRepairCommandToString());
        ARepairResult aRepairResult = aRepair.run();
        aRepairResult.usedTests(tests);
        arepairCalls++;
        return aRepairResult;
    }

    private enum BeAFixMode {TESTS, CHECK}

    private BeAFixResult runBeAFixWithCurrentConfig(FixCandidate candidate, BeAFixMode mode, boolean relaxedFacts, boolean forceAssertionGeneration, Path oracle) {
        try {
            if (!beAFix.cleanOutputDir()) {
                return BeAFixResult.error("Couldn't delete BeAFix output directory");
            }
        } catch (IOException e) {
            logger.severe("An exception occurred when trying to clean BeAFix output directory\n" + exceptionToString(e));
            return BeAFixResult.error("An exception occurred when trying to clean BeAFix output directory\n" + exceptionToString(e));
        }
        Path modelToCheckWithOraclePath = Paths.get(candidate.modelToRepair().path().toAbsolutePath().toString().replace(".als", "_withOracle.als"));
        File modelToCheckWithOracleFile = oracle.toFile();
        if (modelToCheckWithOracleFile.exists()) {
            if (!modelToCheckWithOracleFile.delete()) {
                logger.severe("Couldn't delete model with oracle file (" + (modelToCheckWithOracleFile) + ")");
                return BeAFixResult.error("Couldn't delete model with oracle file (" + (modelToCheckWithOracleFile) + ")");
            }
        }
        try {
            mergeFiles(candidate.modelToRepair().path(), oracle, modelToCheckWithOraclePath);
        } catch (IOException e) {
            logger.severe("An exception occurred while trying to generate model with oracle file\n" + Utils.exceptionToString(e) + "\n");
            return BeAFixResult.error(Utils.exceptionToString(e));
        }
        BeAFixResult beAFixResult = null;
        beAFix.pathToModel(modelToCheckWithOraclePath);
        beAFix.factsRelaxationGeneration(relaxedFacts);
        beAFix.forceAssertionTestsGeneration(forceAssertionGeneration);
        switch (mode) {
            case TESTS: {
                beAFixResult = beAFix.runTestGeneration();
                break;
            }
            case CHECK: {
                beAFixResult = beAFix.runModelCheck();
                break;
            }
        }
        return beAFixResult;
    }

    private Report.TestsAndCandidatesCounters generateTestsAndCandidateCounters() {
        int totalTests = trustedTests.count() + untrustedTests.count();
        int trustedTestsUsed = trustedTests.count();
        int untrustedTestsUsed = untrustedTests.count();
        return new Report.TestsAndCandidatesCounters(totalTests, trustedTestsUsed, untrustedTestsUsed, evaluatedCandidates, evaluatedCandidatesLeadingToNoFix, evaluatedCandidatesLeadingToSpurious);
    }

}
