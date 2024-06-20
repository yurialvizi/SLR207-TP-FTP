package com.slr207.commons.messages;

import java.io.Serializable;
public abstract class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private MessageType type;

    public Message(MessageType type) {
        this.type = type;
    }

    public MessageType getType() {
        return type;
    }
}