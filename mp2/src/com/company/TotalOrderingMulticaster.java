package com.company;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TotalOrderingMulticaster extends Multicaster {
    private boolean isSequencer = false;
    // for sequencer use only
    private int sequencerCount = 0;

    private int seqenuceNumber = 0;

    private String sequencerId = new String();

    private static class MessageLabel implements Serializable {
        String sender;
        String uuid;
        int messageId;
        public MessageLabel(String sender, String uuid, int messageId) {
            this.sender = sender;
            this.uuid = uuid;
            this.messageId = messageId;
        }
    }

    public TotalOrderingMulticaster(BasicSystem basicSystem, Console console) {
        super(basicSystem, console);
        int count = 0;
        for(String k : nodeSet) {
            if(basicSystem.existNode(k))
                count++;
        }
        if(count <= 1)
            isSequencer = true;
    }

    private Map<String, BasicSystem.Message> messageMap = new HashMap<>();
    private Map<Integer, BasicSystem.Message> seqMap = new HashMap<>();

    private boolean tryDeliverMessage(int messageId) {
        BasicSystem.Message message = seqMap.get(messageId);
        if(message != null && message.data != null) {
            MessageLabel messageLabel = (MessageLabel) message.privateData;
            String messagekey = messageLabel.sender + ":" + messageLabel.uuid;

            console.onDelivery(messageLabel.sender, message.data);
            messageMap.remove(messagekey);
            seqMap.remove(messageId);
            return true;
        }
        return false;
    }

    private void cleanMessageMap() {
        while(tryDeliverMessage(seqenuceNumber)) {
            seqenuceNumber++;
        }
    }

    @Override
    public synchronized boolean onMessage(BasicSystem.Message message) {
        if(!super.onMessage(message) && message.type.startsWith("total_")) {
            MessageLabel messageLabel = (MessageLabel) message.privateData;
            String messagekey = messageLabel.sender + ":" + messageLabel.uuid;
            if(message.type.endsWith("message")) {
                if(isSequencer && !messageMap.containsKey(messagekey)) {
                    BasicSystem.Message orderMessage = new BasicSystem.Message("total_order",
                            new MessageLabel(message.id, messageLabel.uuid, sequencerCount), null);
                    seqMap.put(sequencerCount++, null);
                    basicMulticast(orderMessage);
                }
                if(messageMap.get(messagekey) == null)
                    messageMap.put(messagekey, message);
                else
                    messageMap.get(messagekey).data = message.data;
                cleanMessageMap();
            } else if(message.type.endsWith("order")) {
                sequencerId = message.id;
                if(messageMap.get(messagekey) != null)
                    messageMap.get(messagekey).privateData = messageLabel;
                else
                    messageMap.put(messagekey, message);
                seqMap.put(messageLabel.messageId, messageMap.get(messagekey));
                cleanMessageMap();
            }
        }
        return false;
    }

    @Override
    public void multicast(String data) {
        BasicSystem.Message message = new BasicSystem.Message("total_message",
                new MessageLabel(id, UUID.randomUUID().toString(), -1), data);
        basicMulticast(message);
    }
}
