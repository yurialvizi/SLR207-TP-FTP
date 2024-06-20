package com.slr207.commons.messages;

public class SecondShuffleFinishedMessage extends FinishedPhaseMessage {
    private static final long serialVersionUID = 1L;

    public SecondShuffleFinishedMessage() {
    }

    @Override
    public FinishedPhase getPhase() {
        return FinishedPhase.SECOND_SHUFFLE;
    }

}
