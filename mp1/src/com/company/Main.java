package com.company;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            Console console = new Console(args[0], args[1], args.length > 2 ? args[2] : "");
            console.mainLoop();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
