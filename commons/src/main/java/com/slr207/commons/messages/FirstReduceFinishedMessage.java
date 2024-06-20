package com.slr207.commons.messages;

public class FirstReduceFinishedMessage extends FinishedPhaseMessage {
    private static final long serialVersionUID = 1L;
    private int min;
    private int max;

    public FirstReduceFinishedMessage(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    @Override
    public FinishedPhase getPhase() {
        return FinishedPhase.FIRST_REDUCE;
    }
}