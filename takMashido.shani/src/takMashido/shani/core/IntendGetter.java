package takMashido.shani.core;

import org.w3c.dom.Element;

/**Responsible for collecting user intents and giving them to the {@link takMashido.shani.Engine}.*/
public abstract class IntendGetter implements Runnable{
    /**Name of this getter.*/
    public final String getterName;
    
    /**Create new instance of IntendGetter and initialize it with data from XML node.
     * @param e XML Element containing this IntendGetter data.
     */
    public IntendGetter(Element e){
        getterName =e.getAttribute("classname");
    }
}
