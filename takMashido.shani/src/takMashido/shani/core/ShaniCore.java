package takMashido.shani.core;

import takMashido.shani.Config;
import takMashido.shani.Engine;
import takMashido.shani.core.text.ShaniString;

import java.io.PrintStream;
import java.util.Scanner;

/**Core shani methods accessible from out side of module.
 * @author TakMashido
 */
public class ShaniCore {
    /**Default stream for debug text*/
    public static final PrintStream debug=Engine.debug;

    /**Scanner connected into shani text input stream(stdin by default)*/
    public static final Scanner in=Engine.in;

    /**Error message to print if something bad happened to inform user about it.*/
    public static final ShaniString errorMessage=Engine.errorMessage;

    /**Current value of SentenceCompareThreshold.*/
    public static short getSentenceCompareThreshold(){
        return Config.sentenseCompareTreshold;
    }
    /**Current value of WordCompareThreshold.*/
    public static short getWordCompareThreshold(){
        return Config.wordCompareTreshold;
    }

    /** @see Engine#isInputPositive(String)*/
    public static Boolean isInputPositive(String input) {
        return isInputPositive(new ShaniString(input,false));
    }
    /** @see Engine#isInputPositive(ShaniString)*/
    public static Boolean isInputPositive(ShaniString input) {
        return Engine.isInputPositive(input);
    }

    /** @see Engine#getLicenseConfirmation(String).
     * Also prints "Not able to execute because not all license corfirmed*/
    public static boolean getLicenseConfirmation(String name) {
        return getLicenseConfirmation(name,true);
    }
    /** @see Engine#getLicenseConfirmation(String, boolean).
     * Also prints "Not able to execute because not all license corfirmed"*/
    public static boolean getLicenseConfirmation(String name, boolean printConfirmationMessage) {
        return getLicenseConfirmation(name,printConfirmationMessage,true);
    }

    /** @see Engine#getLicenseConfirmation(String, boolean).
     * @param informNotConfirmed if print "Not able to execute because not all license corfirmed" to user.*/
    public static boolean getLicenseConfirmation(String name, boolean printConfirmationMessage, boolean informNotConfirmed) {
        var ret= Engine.getLicenseConfirmation(name,true);

        if(!ret&&informNotConfirmed)
            Engine.licensesNotConfirmedMessage.printOut();

        return ret;
    }
}