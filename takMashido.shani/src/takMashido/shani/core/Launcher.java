package takMashido.shani.core;

import takMashido.shani.Config;
import takMashido.shani.Engine;
import takMashido.shani.libraries.ArgsDecoder;
import takMashido.shani.libraries.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**Contain methods for launching shani.*/
public class Launcher {
    static HashMap<String,String> argsOverride;
    
    public static void main(String[]args){
        run(args);
    }
    
    /**Runs shani with given args on new Thread.
     * @param args Arguments used to launch shani.
     */
    public static void run(String[] args){
        ArgsDecoder argsDec=new ArgsDecoder(args);
    
        if(argsDec.containFlag("--custom")) {		//custom args
            System.out.println("Please give args.");
            @SuppressWarnings("resource")
            Scanner in=new Scanner(new Scanner(System.in).nextLine());
            ArrayList<String> argsList=new ArrayList<>();
            while(in.hasNextLine())argsList.add(in.next());
            args=argsList.toArray(new String[0]);
            argsDec=new ArgsDecoder(args);
            System.out.println("New args set. Running shani with it.");
        }
        
        try {                               //Setup Logger
            Logger.setDefaultLogDirectory(Config.logsDirectory);
        } catch (IOException e) {
            ShaniCore.registerLoadException();
            System.err.println("Failed to setup logging directory.");
            e.printStackTrace();
        }
        Logger.setStreamOpenMessage("<<<<<<<<<<<<<<<<<<shani>>>>>>>>>>>>>>>>>>");
    
        if(argsDec.containFlag("-d","--debug")) {		//debug
            Logger.addStream(System.out,"debug");
        } else if(argsDec.containFlag("-dd")){
            Logger.addStream(System.out,"commands");
            Logger.addStream(System.out,"debug");
        } else {
            System.setErr(Logger.getStream("error",false));
        }
    
        Integer integer;
        if((integer=argsDec.getInt("-cl","--close"))!=null) {
            Thread closingThread=new Thread("CloserThread") {
                @Override
                public void run() {
                    try {
                        Thread.sleep(60*1000*integer);
                        Engine.exit();
                    } catch (InterruptedException ignored) {}
                }
            };
            closingThread.setDaemon(true);
            closingThread.start();
        }
    
        if(!argsDec.isProcesed()) {														//End of args processing
            System.out.println("Program input contain unrecognized parameters:");
            var unmatched=argsDec.getUnprocesed();
            for(var un:unmatched)System.out.println(un);
            System.exit(0);
        }
    
        if(Config.socksProxyHost!=null) {											//Set up before initializing orders
            if(Config.socksProxyPort!=0) {
                System.setProperty("socksProxyHost", Config.socksProxyHost);
                System.setProperty("socksProxyPort", Integer.toString(Config.socksProxyPort));
            } else {
                ShaniCore.registerLoadException();
                System.err.println("Please specify port for socks proxy using \"socksProxyPort\" entry in config file.");
            }
        }
        if(Config.HTTPProxyHost!=null) {
            if(Config.HTTPProxyPort!=0) {
                System.setProperty("http.proxyHost", Config.HTTPProxyHost);
                System.setProperty("http.proxyPort", Integer.toString(Config.HTTPProxyPort));
            } else {
                ShaniCore.registerLoadException();
                System.err.println("Please specify port for http proxy using \"HTTPProxyPort\" entry in config file.");
            }
        }
        
        Engine.initialize();
        Engine.start();
    }
}
