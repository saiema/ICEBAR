package ar.edu.unrc.exa.dc.util;

import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest;

import java.util.HashSet;
import java.util.Set;

public final class TestHashes {

    private final Set<Integer> hashes = new HashSet<>();

    private static TestHashes instance;
    private TestHashes() {}

    public static TestHashes getInstance() {
        if (instance == null)
            instance = new TestHashes();
        return instance;
    }

    public boolean add(BeAFixTest test) {
        int hash = test.hashCode();
        return hashes.add(hash);
    }

    public boolean remove(BeAFixTest test) {
        int hash = test.hashCode();
        return hashes.remove(hash);
    }

}
