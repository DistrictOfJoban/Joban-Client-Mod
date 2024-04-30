package com.lx862.jcm.mod.data;
public class Entry {
    private final String station;
    private final long fare;
    private final String date;
    private final int balance;

    public Entry(String station, long fare, String date, int balance) {
        this.station = station;
        this.fare = fare;
        this.date = date;
        this.balance = balance;
    }

    public String station() {
        return station;
    }

    public long fare() {
        return fare;
    }

    public String date() {
        return date;
    }

    public int balance() {
        return balance;
    }
}