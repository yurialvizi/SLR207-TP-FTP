package com.slr207.commons.messages;

public class SecondMapFinishedMessage extends FinishedPhaseMessage{
    private static final long serialVersionUID = 1L;

    public SecondMapFinishedMessage() {
    }

    @Override
    public FinishedPhase getPhase() {
        return FinishedPhase.SECOND_MAP;
    }
    
}
