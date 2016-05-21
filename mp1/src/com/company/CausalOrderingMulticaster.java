package com.company;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class CausalOrderingMulticaster extends Multicaster{
    private Map<String, Integer> clockVector = new HashMap<>();

    private List<BasicSystem.Message> messageList = new ArrayList<>();

    private int selfSeq = 0;

    public CausalOrderingMulticaster(BasicSystem basicSystem, Console console) {
        super(basicSystem, console);
        for(String k : nodeSet) {
            clockVector.put(k, 0);
        }
    }

    private boolean isGoodDelivery(BasicSystem.Message message) {
        Map<String, Integer> vec = (Map<String, Integer>) message.privateData;
        int recvId = -1;
        for(String k : nodeSet) {
            if(!k.equals(message.id)) {
                if(clockVector.get(k) < vec.get(k)) {
                    return false;
                }
            } else {
                if(message.id.equals(id)) {
                    recvId = selfSeq;
                } else {
                    recvId = clockVector.get(message.id);
                }
            }
        }
        return recvId + 1 == vec.get(message.id);
    }

    private int cleanMessageList() {
        int count = 0;
        // clean message list
        for(Iterator<BasicSystem.Message> iterator = messageList.iterator(); iterator.hasNext();) {
            BasicSystem.Message message = iterator.next();
            if (isGoodDelivery(message)) {
                deliverMessage(message);
                iterator.remove();
                count++;
            }
        }
        return count;
    }

    private void deliverMessage(BasicSystem.Message message) {
        console.onDelivery(message.id, message.data);
        if(!message.id.equals(this.id))
            clockVector.put(message.id, clockVector.get(message.id) + 1);
        else
            selfSeq += 1;
    }

    @Override
    public synchronized boolean onMessage(BasicSystem.Message message) {
        if(!super.onMessage(message) && message.type.equals("causal")) {
            Map<String, Integer> vec = (Map<String, Integer>) message.privateData;
            if(isGoodDelivery(message)) {
                deliverMessage(message);
                // go into message list to clean some messages
                int count = 1;
                while(count > 0) {
                    count = cleanMessageList();
                }
            } else if((!message.id.equals(id) && vec.get(message.id) > clockVector.get(message.id) + 1) ||
                    (message.id.equals(id) && vec.get(message.id) > selfSeq + 1)) {
                messageList.add(message);
            }
            return true;
        }
        return false;
    }

    @Override
    public void multicast(String data) {
        clockVector.put(id, clockVector.get(id) + 1);

        // deep copy clock vector
        Map<String, Integer> sentVector = new HashMap<>();
        for(String k : nodeSet) {
            sentVector.put(k, clockVector.get(k));
        }
        BasicSystem.Message message = new BasicSystem.Message("causal", sentVector, data);
        basicMulticast(message);
    }
}