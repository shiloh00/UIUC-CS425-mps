package com.company;

import java.io.IOException;
import java.util.UUID;

public class EventualReplica
        extends Replica
{
    protected EventualReplica(String configPath, String id, String multicastType, int W, int R)
            throws IOException
    {
        super(configPath, id, multicastType, W, R);
    }

    private static class Event
    {
        public Event(String uuid)
        {
            this.uuid = uuid;
            this.value = new Value(0, 0);
        }

        String uuid;

        Value value;

        int count;
    }

    // ack:clientId:uuid:varName:value:timestamp:put/get
    @Override
    public synchronized void onDelivery(String id, String data)
    {
        String[] results = data.split(":");
        String ackString;
        switch (results[0]) {
            case "put":
                setValue(results[3], Integer.parseInt(results[4]), Long.parseLong(results[5]));
                ackString = String.format("ack:%s:%s:%s:%s:0:put", results[1], results[2], results[3], results[4]);
                unicastData(id, ackString);
                break;
            case "get":
                Value value = getValue(results[3]);
                ackString = String.format("ack:%s:%s:%s:%d:%d:get", results[1], results[2], results[3],
                        value != null ? value.value : 0, value != null ? value.timestamp : 0);
                unicastData(id, ackString);
                break;
            case "ack":
                Event event = (Event) clientMap.get(results[1]).privateData;
                String type = results[6];
                if (event != null && event.uuid.equals(results[2])) {
                    event.count++;
                    if (type.equals("put")) {
                        if (event.count == wCount) {
                            try {
                                clientMap.get(results[1]).objectOutputStream.writeObject("");
                                log(results[1], "put", "resp", results[3], Integer.parseInt(results[4]));
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if (type.equals("get")) {
                        int val = Integer.parseInt(results[4]);
                        long ts = Long.parseLong(results[5]);
                        if (event.value.timestamp < ts) {
                            event.value = new Value(val, ts);
                        }
                        if (event.count == rCount) {
                            try {
                                clientMap.get(results[1]).objectOutputStream.writeObject(String.format("%d", event.value.value));
                                log(results[1], "get", "resp", results[3], event.value.value);
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                break;
        }
    }

    // put/get:clientId:uuid:varName(:value:timestamp)
    @Override
    public void onRequest(String clientId, String cmd, String[] args)
    {
        String requestString;
        String uuid = UUID.randomUUID().toString();
        switch (cmd) {
            case "get":
                log(clientId, "get", "req", args[0], null);
                clientMap.get(clientId).privateData = new Event(uuid.toString());
                requestString = String.format("get:%s:%s:%s:0:%d", clientId, uuid, args[0], System.currentTimeMillis());
                multicastData(requestString);
                break;
            case "put":
                int val = Integer.parseInt(args[1]);
                log(clientId, "put", "req", args[0], val);
                clientMap.get(clientId).privateData = new Event(uuid.toString());
                requestString = String.format("put:%s:%s:%s:%d:%d", clientId, uuid, args[0], val, System.currentTimeMillis());
                multicastData(requestString);
                break;
        }
    }
}
