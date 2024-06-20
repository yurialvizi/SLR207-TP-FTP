package com.slr207.commons.messages;

import java.util.List;

public class StartMessage extends Message {
    private static final long serialVersionUID = 1L;
    private final int totalNodes;
    private final List<String> nodeServerList;
    private final String yourOwnServer;

    public StartMessage(int totalNodes, List<String> nodeServerList, String yourOwnServer) {
        super(MessageType.START);
        this.totalNodes = totalNodes;
        this.nodeServerList = nodeServerList;
        this.yourOwnServer = yourOwnServer;
    }
    
    public int getTotalNodes() {
        return totalNodes;
    }

    public String getYourOwnServer() {
        return yourOwnServer;
    }
    
    public List<String> getNodeServerList() {
        return nodeServerList;
    }
}
