package com.slr207.commons.messages;

public class FirstShuffleFinishedMessage extends FinishedPhaseMessage {
    private static final long serialVersionUID = 1L;

    public FirstShuffleFinishedMessage() {
    }

    @Override
    public FinishedPhase getPhase() {
        return FinishedPhase.FIRST_MAP;
    }
    
}
