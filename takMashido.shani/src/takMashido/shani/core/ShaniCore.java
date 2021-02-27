package takMashido.shani.core;

import takMashido.shani.Config;
import takMashido.shani.Engine;
import takMashido.shani.core.text.ShaniString;

import java.io.PrintStream;

/**Core shani methods accessible from outside of module.
 * Also provides extended implementation of functions from {@link Engine} class.
 * @author TakMashido
 */
public class ShaniCore {
    /**Default stream for debug text*/
    public static final PrintStream debug=Engine.debug;

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
        var ret= Engine.getLicenseConfirmation(name,true);

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
}