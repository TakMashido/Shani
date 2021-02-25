package takMashido.shani.core;

public class Intent {
    public IntentBase rawValue;
    public IntentBase value;

    public Intent(IntentBase value){
        this.rawValue=this.value=value;
    }
    public Intent(IntentBase rawValue, IntentBase value){
        this.rawValue=rawValue;
        this.value=value;
    }

    public Intent copy(){
        return new Intent(rawValue.copy(), value.copy());
    }

    @Override
    public String toString(){
        return "intent: '"+rawValue+"':'"+value+'\'';
    }
}
