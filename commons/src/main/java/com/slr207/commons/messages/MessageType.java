package com.slr207.commons.messages;

public enum MessageType {
    START("Start"),
    FIRST_SHUFFLE("FirstShuffle"),
    FIRST_REDUCE("FirstReduce"),
    GROUPS("Groups"),
    SECOND_SHUFFLE("SecondShuffle"),
    SECOND_REDUCE("SecondReduce"),
    FINISHED("Finished");

    private final String displayName;

    MessageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
