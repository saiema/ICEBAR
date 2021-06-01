package ar.edu.unrc.exa.dc.util;

import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class TestHashes {

    private final Set<Integer> hashes = new HashSet<>();
    private final Set<Integer> lastHashes = new HashSet<>();

    private static TestHashes instance;
    private TestHashes() {}

    public static TestHashes getInstance() {
        if (instance == null)
            instance = new TestHashes();
        return instance;
    }

    public boolean add(BeAFixTest test) {
        int hash = test.currentTestHashCode();
        boolean added = hashes.add(hash);
        if (added)
            lastHashes.add(hash);
        return added;
    }

    public void undoLatestExceptFor(Collection<BeAFixTest> exceptions) {
        if (exceptions.stream().anyMatch(BeAFixTest::isBranchedTest))
            throw new IllegalArgumentException("Tests in exceptions list can't be branching tests");
        Set<Integer> exceptionsHashes = exceptions.stream().map(BeAFixTest::currentTestHashCode).collect(Collectors.toSet());
        Set<Integer> relatedTestsExceptions = exceptions.stream().filter(BeAFixTest::isRelated).map(BeAFixTest::relatedBeAFixTest).map(BeAFixTest::currentTestHashCode).collect(Collectors.toSet());
        int removed = 0;
        for (int undoHash : lastHashes) {
            if (exceptionsHashes.contains(undoHash) || relatedTestsExceptions.contains(undoHash))
                continue;
            if (!hashes.remove(undoHash))
                throw new IllegalStateException("Removing a nonexistent hash");
            removed++;
        }
        int expectedToRemove = lastHashes.size() - exceptions.size() - relatedTestsExceptions.size();
        if (removed != expectedToRemove)
            throw new IllegalStateException("Was expecting to remove " + expectedToRemove + " hashes, but only removed " + removed);
        clearUndoCache();
    }

    public void clearUndoCache() {
        lastHashes.clear();
    }

}
