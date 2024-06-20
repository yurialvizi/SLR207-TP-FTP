package com.slr207.commons;

import java.io.ObjectOutputStream;

import com.slr207.commons.messages.Message;

public interface MessageProcessor {

    void process(Message message, ObjectOutputStream out);

}
