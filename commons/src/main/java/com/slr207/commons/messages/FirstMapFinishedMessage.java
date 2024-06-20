package com.slr207.commons.messages;

public class FirstMapFinishedMessage extends FinishedPhaseMessage {
    private static final long serialVersionUID = 1L;

    public FirstMapFinishedMessage() {
    }

    @Override
    public FinishedPhase getPhase() {
        return FinishedPhase.FIRST_MAP;
    }
    
}
