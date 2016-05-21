package com.company;

import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Replica
        extends Console
{

    public static void main(String[] args)
    {
        try {
            // config-path id model-type W R
            createReplica(args[0], args[1], args[2],
                    args.length > 3 ? Integer.parseInt(args[3]) : -1,
                    args.length > 4 ? Integer.parseInt(args[4]) : -1);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected int wCount;
    protected int rCount;

    public static Replica createReplica(String configPath, String id, String modelType, int W, int R)
            throws IOException
    {
        switch (modelType) {
            case "linear":
                return new LinearReplica(configPath, id, "total", W, R);
            case "eventual":
                return new EventualReplica(configPath, id, "", W, R);
            default:
                throw new IllegalArgumentException("unkown model type: " + modelType);
        }
    }

    private FileWriter fileWriter;

    protected int replicaCount;

    protected Replica(String configPath, String id, String multicastType, int W, int R)
            throws IOException
    {
        super(configPath, id, multicastType);
        wCount = W;
        rCount = R;
        fileWriter = new FileWriter("log_" + id + ".txt", false);
        replicaCount = basicSystem.getNodeSet().size();
    }

    protected class ClientNode
    {
        public ClientNode(ObjectOutputStream objectOutputStream)
        {
            this.objectOutputStream = objectOutputStream;
        }

        public ObjectOutputStream objectOutputStream;
        public Object privateData;
    }

    protected Map<String, ClientNode> clientMap = new HashMap<>();

    protected static class Value {
        public Value(int value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
        int value;
        long timestamp;
    }

    private Map<String, Value> dataMap = new HashMap<>();

    protected Value getValue(String varName) {
        return dataMap.get(varName);
    }

    protected void setValue(String varName, int value, long timestamp) {
        dataMap.put(varName, new Value(value, timestamp));
    }

    public void onClientData(String clientId, String data, ObjectOutputStream objectOutputStream)
    {
        if (!clientMap.containsKey(clientId)) {
            clientMap.put(clientId, new ClientNode(objectOutputStream));
        } else {
            clientMap.get(clientId).objectOutputStream = objectOutputStream;
        }
        String[] results = data.split(":", 2);
        String[] args = null;
        if (results.length == 2) {
            args = results[1].split(":");
        }
        if(results[0].equals("dump")) {
            dumpData();
            try {
                objectOutputStream.writeObject("");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        onRequest(clientId, results[0], args);
    }

    public void onRequest(String clientId, String cmd, String[] args)
    {
        throw new UnsupportedOperationException("basic replica not implement onRequest");
    }

    @Override
    public synchronized void onDelivery(String id, String data)
    {
        throw new UnsupportedOperationException("basic class not implement onDelivery");
    }

    protected void dumpData()
    {
        Iterator iterator = dataMap.entrySet().iterator();
        System.out.println("DUMP DATA:");
        while (iterator.hasNext()) {
            Map.Entry<String, Value> entry = (Map.Entry<String, Value>) iterator.next();
            System.out.println(entry.getKey() + " => " + entry.getValue().value);
        }
    }

    protected void log(String clientId, String op, String stat, String varName, Integer value) {
        String val = value == null ? "" : value.toString();
        try {
            fileWriter.write(String.format("2323,%s,%s,%s,%d,%s,%s\n", clientId, op, varName, System.currentTimeMillis(), stat, val));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fileWriter.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
