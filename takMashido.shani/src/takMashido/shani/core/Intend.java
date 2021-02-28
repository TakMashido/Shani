package takMashido.shani.core;

/**Representes user intend. It's interpreted by shani to get {@link takMashido.shani.orders.Action} to execute.
 * @author TakMashido
 */
public class Intend {
    /**Raw user input.*/
    public final IntendBase rawValue;
    /**User input after applying all {@link takMashido.shani.filters.IntendFilter input filters}.*/
    public IntendBase value;

    public Intend(IntendBase value){
        this.rawValue=this.value=value;
    }
    public Intend(IntendBase rawValue, IntendBase value){
        this.rawValue=rawValue;
        this.value=value;
    }

    public Intend copy(){
        return new Intend(rawValue.copy(), value.copy());
    }

    @Override
    public String toString(){
        return "intent: '"+rawValue+"':'"+value+'\'';
    }
}
