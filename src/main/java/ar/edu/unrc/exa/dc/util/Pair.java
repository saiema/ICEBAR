package ar.edu.unrc.exa.dc.util;

public class Pair<A, B> {

    private final A fst;
    private final B snd;

    public Pair(A fst, B snd) {
        this.fst = fst;
        this.snd = snd;
    }

    public A fst() {
        return fst;
    }

    public B snd() {
        return snd;
    }

}
