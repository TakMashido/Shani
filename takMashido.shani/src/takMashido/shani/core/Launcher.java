package takMashido.shani.core;

import takMashido.shani.Engine;

/**Contain methods for launching shani.*/
public class Launcher {
    /**Runs shani with given args on current Thread.
     * @param args Arguments used to launch shani.
     */
    public static void run(String[] args){
        Engine.initialize(args);
        Engine.start();
    }
}
