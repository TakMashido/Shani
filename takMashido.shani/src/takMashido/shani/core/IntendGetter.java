package takMashido.shani.core;

import org.w3c.dom.Element;

/**Responsible for collecting user intents and giving them to the {@link takMashido.shani.Engine}.*/
public abstract class IntendGetter implements Runnable{
    public final String getterName;

    public IntendGetter(Element e){
        getterName =e.getAttribute("classname");
    }
}
