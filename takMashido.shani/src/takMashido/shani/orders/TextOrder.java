package takMashido.shani.orders;

import org.w3c.dom.Element;
import takMashido.shani.core.Intend;
import takMashido.shani.core.text.ShaniString;

import java.util.List;

public abstract class TextOrder extends Order{
    public TextOrder(Element e){
        super(e);
    }

    @Override
    public List<Executable> getExecutables(Intend intend) {
        if(intend.value instanceof ShaniString)
            return getExecutables((ShaniString) intend.value);
        return null;
    }

    public abstract List<Executable> getExecutables(ShaniString string);
}
