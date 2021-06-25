package takMashido.shani.core;

import takMashido.shani.Config;
import takMashido.shani.Engine;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.libraries.Logger;

import java.io.PrintStream;

/**Core shani methods accessible from outside of module.
 * Also provides extended implementation of functions from {@link Engine} class.
 * @author TakMashido
 */
public class ShaniCore {
    /**Default stream for debug text*/
    public static final PrintStream debug= Logger.getStream("debug");
    /**Default stream low level debug messages*/
    public static final PrintStream info= Logger.getStream("info");
    
    /**Error message to print if something bad happened to inform user about it.*/
    public static final ShaniString errorMessage=Engine.errorMessage;

    /**Current value of SentenceCompareThreshold.*/
    public static short getSentenceCompareThreshold(){
        return Config.sentenceCompareThreshold;
    }
    /**Current value of WordCompareThreshold.*/
    public static short getWordCompareThreshold(){
        return Config.wordCompareThreshold;
    }

    /** @see Engine#isInputPositive(ShaniString)*/
    public static Boolean isInputPositive(ShaniString input) {
        return Engine.isInputPositive(input);
    }

    /**Checks if user confirmed given license. If not confirmed ask user to do it and prints message if user refuses to do it.
     * @param name Name of license which confirmation are being checked.
     * @return If license confirmed.
     */
    public static boolean getLicenseConfirmation(String name) {
        return getLicenseConfirmation(name,true);
    }
    /**Checks if user confirmed given license. If it's not confirmed abort message is printed to user.
     * @param name Name of license which confirmation are being checked.
     * @param printConfirmationMessage If ask user to get confirmation.
     * @return If license confirmed.
     */
    public static boolean getLicenseConfirmation(String name, boolean printConfirmationMessage) {
        return getLicenseConfirmation(name,printConfirmationMessage,true);
    }
    /**Checks if user confirmed given license. If it's not confirmed abort message is printed to user.
     * @param name Name of license which confirmation are being checked.
     * @param printConfirmationMessage If ask user to get confirmation.
     * @param informNotConfirmed if print "Not able to execute because not all license confirmed" to user.
     * @return If license confirmed.
     */
    public static boolean getLicenseConfirmation(String name, boolean printConfirmationMessage, boolean informNotConfirmed) {
        var ret= Engine.getLicenseConfirmation(name,printConfirmationMessage);

        if(!ret&&informNotConfirmed)
            Engine.licensesNotConfirmedMessage.printOut();

        return ret;
    }

    /**Add new intend to interpretation queue.
     * @param str String to interpret.
     */
    public static void interpret(String str){
        interpret(new ShaniString(str));
    }
    /**Add new intend to interpretation queue.
     * @param intend Intend to interpret.
     */
    public static void interpret(IntendBase intend){
        Engine.registerIntend(new Intend(intend));
    }

    /**Get intend provided by user.
     * @param type Class of same type you want Type of intent, you do not want Gesture intend when asking for name of something.
     * @return {@link Intend} containing user input of specified type.
     */
    public static Intend getIntend(Class<? extends IntendBase> type){
        try {
            while(true){
                Intend intend=Engine.getIntend();

                if(type.isInstance(intend.value)){
                    return intend;
                }
            }
        } catch (InterruptedException ignored) {}

        return null;
    }
    
    /**Call if any error encountered during loading shani. Sets up LOADING_ERROR flag. If true at the end of loading message informing user about loading errors become send to System.out*/
    public static void registerLoadException() {
        Engine.registerLoadException();
    }
}