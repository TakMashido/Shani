package takMashido.shani.core;

public class Intend {
    public IntendBase rawValue;
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
