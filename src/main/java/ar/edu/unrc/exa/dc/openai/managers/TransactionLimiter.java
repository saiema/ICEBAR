package ar.edu.unrc.exa.dc.openai.managers;

import ar.edu.unrc.exa.dc.util.TimeCounter;

public class TransactionLimiter {

    private final TimeCounter clock;
    private int transactionsDone;
    private final int maximumTransactionsPerMinute;

    public TransactionLimiter(int maximumTransactionsPerMinute) {
        this.clock = new TimeCounter();
        this.transactionsDone = 0;
        this.maximumTransactionsPerMinute = maximumTransactionsPerMinute;
    }

    public boolean markTransaction() {
        if (clock.isRunning()) {
            clock.updateTotalTime();
            if (clock.toMinutes() > 1) {
                clock.clockEnd();
                transactionsDone = 0;
            }
        }
        if (transactionsDone == maximumTransactionsPerMinute) {
            return false;
        }
        transactionsDone++;
        if (!clock.isRunning()) {
            clock.clockStart();
        }
        return true;
    }

}
