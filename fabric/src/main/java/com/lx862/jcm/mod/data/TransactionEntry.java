package com.lx862.jcm.mod.data;

public class TransactionEntry {
    private final String source;
    private final long time;
    private final long amount;

    public TransactionEntry(String source, long amount, long time) {
        this.source = source;
        this.amount = amount;
        this.time = time;
    }

    public static TransactionEntry createNow(String source, long amount) {
        return new TransactionEntry(source, amount, System.currentTimeMillis());
    }

    public String source() {
        return this.source;
    }

    public long time() {
        return this.time;
    }

    public long amount() {
        return this.amount;
    }
}