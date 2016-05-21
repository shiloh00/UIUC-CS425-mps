package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Console
{
    protected BasicSystem basicSystem;
    private Multicaster multicaster;
    private SimpleDateFormat dateFormat;
    public Console(String config, String id, String multicasterType)
            throws IOException
    {
        basicSystem = new BasicSystem();
        basicSystem.loadConfiguration(config);
        basicSystem.init(id);
        switch (multicasterType) {
            case "":
                this.multicaster = new Multicaster(basicSystem, this);
                break;
            case "causal":
                this.multicaster = new CausalOrderingMulticaster(basicSystem, this);
                break;
            case "total":
                this.multicaster = new TotalOrderingMulticaster(basicSystem, this);
                break;
            default:
                throw new IllegalArgumentException("Unknown argument multicast type");
        }
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }

    private String getCurrentTime() {
        return dateFormat.format(new Date());
    }

    public void mainLoop()
            throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while((line = br.readLine()) != null) {
            //System.out.println("hehe "+ line);
            executeCommand(line.trim());
        }
    }

    private void executeCommand(String cmd) {
        String[] results = cmd.split("\\s+", 2);
        if(results.length < 1)
            return;
        if(results[0].equals("msend")) {
            multicastData(results.length > 1 ? results[1] : "");
        } else if(results[0].equals("send")) {
            results = cmd.split("\\s+", 3);
            if(results.length >= 2)
                unicastData(results[1], results.length > 2 ? results[2] : "");
        }
    }

    protected void unicastData(String id, String data) {
        /*
        if(multicaster.singleSend(id, data))
            System.out.println("Sent \"" + data + "\" process " + id + ", system time is " + getCurrentTime());
            */
        multicaster.singleSend(id, data);
    }

    protected void multicastData(String data) {
        multicaster.multicast(data);
    }

    public void onClientData(String clientId, String data, ObjectOutputStream objectOutputStream) {
        throw new UnsupportedOperationException("Console not implement onRequest");
    }

    public synchronized void onDelivery(String id, String data) {
        System.out.println("Received \"" + data + "\" from process " + id + ", system time is " + getCurrentTime());
    }
}
