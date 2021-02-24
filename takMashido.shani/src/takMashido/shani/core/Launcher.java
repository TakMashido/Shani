package takMashido.shani.core;

import takMashido.shani.Engine;

public class Launcher {
    public static void run(String[] args){
        Engine.initialize(args);
        Engine.start();
    }
}
