package ar.edu.unrc.exa.dc.util;

public class Pair<A, B> {

    private A fst;
    private B snd;

    public Pair(A fst, B snd) {
        this.fst = fst;
        this.snd = snd;
    }

    public A fst() {
        return fst;
    }

    public void fst(A fst) {
        this.fst = fst;
    }

    public B snd() {
        return snd;
    }

    public void snd(B snd) {
        this.snd = snd;
    }

}
