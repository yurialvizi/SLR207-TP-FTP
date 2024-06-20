package com.slr207.commons.messages;

public class FirstReduceMessage extends Message {
    private static final long serialVersionUID = 1L;

    public FirstReduceMessage() {
        super(MessageType.FIRST_REDUCE);
    }
}
