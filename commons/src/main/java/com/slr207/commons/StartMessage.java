package com.slr207.commons;

import java.util.List;

public class StartMessage extends Message {
    private static final long serialVersionUID = 1L;
    private final int totalNodes;
    private final List<String> nodeIPList;
    private final String masterIP;

    public StartMessage(int totalNodes, List<String> nodeIPList, String masterIP) {
        super();
        this.totalNodes = totalNodes;
        this.nodeIPList = nodeIPList;
        this.masterIP = masterIP;
    }
    
    public int getTotalNodes() {
        return totalNodes;
    }
    
    public List<String> getNodeIPList() {
        return nodeIPList;
    }
    
    public String getMasterIP() {
        return masterIP;
    }
}
