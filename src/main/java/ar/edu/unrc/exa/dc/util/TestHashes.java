package ar.edu.unrc.exa.dc.util;

import ar.edu.unrc.exa.dc.tools.BeAFixResult.BeAFixTest;

import java.util.HashSet;
import java.util.Set;

public final class TestHashes {

    private final Set<Integer> hashes = new HashSet<>();

    public void addHash(BeAFixTest test) {
        int hash = test.currentTestHashCode();
        hashes.add(hash);
    }

    public boolean contains(BeAFixTest test) {
        int hash = test.currentTestHashCode();
        return hashes.contains(hash);
    }

    public int count() {
        return hashes.size();
    }

}
