package com.slr207.commons;

import java.io.ObjectOutputStream;

public interface MessageProcessor {


    void process(Message message, ObjectOutputStream out);

}
