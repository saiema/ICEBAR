package ar.edu.unrc.exa.dc.util;

import java.util.HashSet;
import java.util.Set;

public class OneTypePair<A> extends Pair<A, A> {
    public OneTypePair(A fst, A snd) {
        super(fst, snd);
    }

    public Set<A> asSet() {
        Set<A> set = new HashSet<>();
        set.add(fst());
        set.add(snd());
        return set;
    }

}
