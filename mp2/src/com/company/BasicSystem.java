package com.company;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class BasicSystem {
    private String id;
    public String getId() {
        return id;
    }
    private ServerSocket serverSocket;
    private Multicaster multicaster;
    public void setMulticaster(Multicaster multicaster) {
        this.multicaster = multicaster;
    }

    public static class Message implements  Serializable {
        public String id;
        public String type;
        public Object privateData;
        public String data;
        public Message(String type, Object privateData, String data) {
            this.type = type;
            this.privateData = privateData;
            this.data = data;
        }
    }
    public void init(String id) throws IOException {
        this.id = id;
        serverSocket = new ServerSocket(nodeMap.get(id).port);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket;
                while(true) {
                    try {
                        socket = serverSocket.accept();
                        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                        String s = (String) objectInputStream.readObject();
                        if(nodeMap.containsKey(s)) {
                            nodeMap.get(s).socket = socket;
                            nodeMap.get(s).objectInputStream = objectInputStream;
                            if (!s.equals(id))
                                nodeMap.get(s).objectOutputStream = objectOutputStream;
                        }
                        startReceiveThread(s, objectInputStream, objectOutputStream);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        Iterator it = nodeMap.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, Node> pair = (Map.Entry)it.next();
            try {
                Socket socket = new Socket(pair.getValue().ip, pair.getValue().port);
                pair.getValue().socket = socket;
                pair.getValue().objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                pair.getValue().objectInputStream = new ObjectInputStream(socket.getInputStream());
                //System.out.println("hehe laile " + pair.getKey());
                if(!pair.getKey().equals(this.id)) {
                    //System.out.println("in hehe laile " + pair.getKey());
                    startReceiveThread(pair.getKey(), pair.getValue().objectInputStream, pair.getValue().objectOutputStream);
                }
                pair.getValue().objectOutputStream.writeObject(id);
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }

    private void startReceiveThread(final String id, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //ObjectInputStream objectInputStream = nodeMap.get(id).objectInputStream;
                //System.out.println("id => " + id);
                while(true) {
                    try {
                        Object receivedData = objectInputStream.readObject();
                        if(receivedData instanceof Message) {
                            Message message = (Message) receivedData;
                            //System.out.println("!!!!" + message.data);
                            if (multicaster != null)
                                multicaster.onMessage(message);
                        } else if(receivedData instanceof String) {
                            if(multicaster != null)
                                multicaster.onClientData(id, (String) receivedData, objectOutputStream);
                        }
                    }
                    catch (IOException e) {
                        //e.printStackTrace();
                        break;
                    }
                    catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                if(nodeMap.containsKey(id)) {
                    try {
                        nodeMap.get(id).objectInputStream.close();
                        nodeMap.get(id).objectOutputStream.close();
                        nodeMap.get(id).objectInputStream = null;
                        nodeMap.get(id).objectOutputStream = null;
                        nodeMap.get(id).socket.close();
                        nodeMap.get(id).socket = null;
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private synchronized void noDelaySend(String id, Message message) throws IOException {
        if(nodeMap.get(id).socket == null)
            return;
        message.id = this.id;
        nodeMap.get(id).objectOutputStream.writeObject(message);
    }

    public boolean existNode(String id) {
        return nodeMap.get(id) != null && nodeMap.get(id).socket != null;
    }

    public Set<String> getNodeSet() {
        return nodeMap.keySet();
    }

    private Random rand = new Random();
    public void unicastSend(final String id, final Message message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int randomNum = rand.nextInt((maxDelay - minDelay) + 1) + minDelay;
                    Thread.sleep(randomNum);
                    noDelaySend(id, message);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    /*
    public Message unicastReceive(String source) throws IOException, ClassNotFoundException {
        return (Message) nodeMap.get(source).objectInputStream.readObject();
    }
    */

    private class Node {
        public String ip;
        public int port;
        public Socket socket;
        public ObjectOutputStream objectOutputStream;
        public ObjectInputStream objectInputStream;
        public Node(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }
    private HashMap<String, Node> nodeMap = new HashMap<>();
    private int minDelay = 0;
    private int maxDelay = 0;
    public boolean loadConfiguration(String filename) throws IOException {
        File file = new File(filename);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            line = br.readLine();
            String[] delay = line.split("\\s+");
            minDelay = Integer.parseInt(delay[0]);
            maxDelay = Integer.parseInt(delay[1]);
            while ((line = br.readLine()) != null) {
                String[] stringList = line.split(" ");
                nodeMap.put(stringList[0], new Node(stringList[1], Integer.parseInt(stringList[2])));

            }
        }
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("minDelay = " + minDelay + "\n");
        sb.append("maxDelay = " + maxDelay + "\n");
        Iterator it = nodeMap.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, Node> pair = (Map.Entry) it.next();
            sb.append(pair.getKey() + " => " + pair.getValue().ip + ":" + pair.getValue().port + "\n");
        }
        return sb.toString();
    }
}

