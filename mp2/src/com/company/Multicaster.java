package com.company;

import java.io.ObjectOutputStream;
import java.util.Set;

public class Multicaster
{
    protected BasicSystem basicSystem;
    protected Console console;
    protected Set<String> nodeSet;
    protected String id;
    public Multicaster(BasicSystem basicSystem, Console console) {
        this.basicSystem = basicSystem;
        basicSystem.setMulticaster(this);
        this.nodeSet = basicSystem.getNodeSet();
        this.console = console;
        this.id = basicSystem.getId();
    }

    public boolean singleSend(String id, String data) {
        if(!basicSystem.existNode(id))
            return false;
        basicSystem.unicastSend(id, new BasicSystem.Message("single", null, data));
        return true;
    }

    public void multicast(String data) {
        basicMulticast(new BasicSystem.Message("basic", null, data));
    }

    protected void basicMulticast(BasicSystem.Message message) {
        for(String id : nodeSet) {
            basicSystem.unicastSend(id, message);
        }
    }

    // return true if consumed, otherwise false
    public synchronized boolean onMessage(BasicSystem.Message message) {
        if(message.type.equals("single") || message.type.equals("basic")) {
            console.onDelivery(message.id, message.data);
            return true;
        }
        return false;
    }

    // should be the client data
    public void onClientData(String id, String data, ObjectOutputStream objectOutputStream) {
        console.onClientData(id, data, objectOutputStream);
    }
}
