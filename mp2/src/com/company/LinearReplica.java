package com.company;

import java.io.IOException;
import java.util.UUID;

public class LinearReplica
        extends Replica
{
    protected LinearReplica(String configPath, String id, String multicastType, int W, int R)
            throws IOException
    {
        super(configPath, id, multicastType, W, R);
    }

    private static class Event {
        public Event(String uuid) {
            this.uuid = uuid;
        }
        String uuid;
        int count;
    }

    // ack:clientId:uuid:varName:value
    @Override
    public synchronized void onDelivery(String id, String data)
    {
        System.out.println(id + " => " + data);
        String[] results = data.split(":");
        switch (results[0]) {
            case "req":
                setValue(results[3], Integer.parseInt(results[4]), 0);
                String ackString = String.format("ack:%s:%s:%s:%s", results[1], results[2], results[3], results[4]);
                unicastData(id, ackString);
                break;
            case "ack":
                Event event = (Event) clientMap.get(results[1]).privateData;
                if(event != null && event.uuid.equals(results[2])) {
                    event.count++;
                    if(event.count == replicaCount) {
                        clientMap.get(results[1]).privateData = null;
                        try {
                            clientMap.get(results[1]).objectOutputStream.writeObject("");
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                        log(results[1], "put", "resp", results[3], Integer.parseInt(results[4]));
                    }
                }
                break;
        }
    }

    // req:clientId:uuid:varName:value
    @Override
    public void onRequest(String clientId, String cmd, String[] args)
    {
        switch (cmd) {
            case "get":
                try {
                    log(clientId, "get", "req", args[0], null);
                    Value value = getValue(args[0]);
                    int realValue = value == null ? 0 : value.value;
                    clientMap.get(clientId).objectOutputStream.writeObject(String.format("%d", realValue));
                    log(clientId, "get", "resp", args[0], realValue);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "put":
                int val = Integer.parseInt(args[1]);
                //dataMap.put(args[0], val);
                log(clientId, "put", "req", args[0], val);
                String uuid = UUID.randomUUID().toString();
                clientMap.get(clientId).privateData = new Event(uuid.toString());
                String requestString = String.format("req:%s:%s:%s:%d", clientId, uuid, args[0], val);
                multicastData(requestString);
                break;
        }
    }
}
