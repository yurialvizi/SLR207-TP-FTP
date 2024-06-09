package com.slr207.commons;

import java.util.Map;

public class GroupsMessage extends Message {
    private static final long serialVersionUID = 1L;
    Map<String, Map<String, Integer>> groups;

    public GroupsMessage(Map<String, Map<String, Integer>> groups) {
        super();
        this.groups = groups;
    }
}
