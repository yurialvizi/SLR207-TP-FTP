package com.slr207.commons.messages;

public class SecondReduceFinishedMessage extends FinishedPhaseMessage {
    private static final long serialVersionUID = 1L;

    public SecondReduceFinishedMessage() {
    }

    @Override
    public FinishedPhase getPhase() {
        return FinishedPhase.SECOND_REDUCE;
    }
}
