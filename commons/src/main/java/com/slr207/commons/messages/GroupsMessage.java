package com.slr207.commons.messages;

import java.util.Map;

public class GroupsMessage extends Message {
    private static final long serialVersionUID = 1L;
    private Map<String, Map<String, Integer>> groups;

    public GroupsMessage(Map<String, Map<String, Integer>> groups) {
        super(MessageType.GROUPS);
        this.groups = groups;
    }

    public Map<String, Map<String, Integer>> getGroups() {
        return groups;
    }
}
