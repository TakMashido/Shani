package takMashido.shani.core;

import takMashido.shani.Config;
import takMashido.shani.Engine;
import takMashido.shani.core.text.ShaniString;

import java.io.PrintStream;
import java.util.Scanner;

public class ShaniCore {
    public static final PrintStream debug=Engine.debug;

    public static final Scanner in=Engine.in;

    public static final ShaniString errorMessage=Engine.errorMessage;

    public static short getSentenceCompareThreshold(){
        return Config.sentenseCompareTreshold;
    }
    public static short getWordCompareThreshold(){
        return Config.wordCompareTreshold;
    }

    public static Boolean isInputPositive(String input) {
        return isInputPositive(new ShaniString(input,false));
    }
    public static Boolean isInputPositive(ShaniString input) {
        return Engine.isInputPositive(input);
    }

    public static boolean getLicenseConfirmation(String name) {
        return getLicenseConfirmation(name,true);
    }
    public static boolean getLicenseConfirmation(String name, boolean printConfirmationMessage) {
        return getLicenseConfirmation(name,printConfirmationMessage,true);
    }
    public static boolean getLicenseConfirmation(String name, boolean printConfirmationMessage, boolean informNotConfirmed) {
        var ret= Engine.getLicenseConfirmation(name,true);

        if(!ret&&informNotConfirmed)
            Engine.licensesNotConfirmedMessage.printOut();

        return ret;
    }
}