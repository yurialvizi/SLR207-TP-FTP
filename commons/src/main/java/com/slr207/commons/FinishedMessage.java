package com.slr207.commons;



public class FinishedMessage extends Message{
    public enum FinishedPhase {
        FIRST_MAP,
        FIRST_SHUFFLE,
        SECOND_SHUFFLE,
        SECOND_MAP,
        SECOND_REDUCE,
    }
    
    private static final long serialVersionUID = 1L;
    private final FinishedPhase finishedPhase;

    public FinishedMessage(FinishedPhase finishedPhase) {
        super();
        this.finishedPhase = finishedPhase;
    }

    public FinishedPhase getFinishedPhase() {
        return finishedPhase;
    }
}
