package com.slr207.commons.messages;

public abstract class FinishedPhaseMessage extends Message{
    public FinishedPhaseMessage() {
        super(MessageType.FINISHED);
    }

    public abstract FinishedPhase getPhase();
}
