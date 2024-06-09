package com.slr207.commons;

public class ReduceFinishedMessage extends Message{
    private static final long serialVersionUID = 1L;
    private int min;
    private int max;

    public ReduceFinishedMessage(int min, int max) {
        super();
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }
}
