package takMashido.shani.filters;

import org.w3c.dom.Element;
import takMashido.shani.core.Intend;
import takMashido.shani.core.text.ShaniString;

public abstract class TextFilter extends IntendFilter {
    public TextFilter(Element e) {
        super(e);
    }

    @Override
    public Intend filter(Intend intend) {
        if(intend.value instanceof ShaniString)
            intend.value=filter((ShaniString) intend.value);
        return intend;
    }

    public abstract ShaniString filter(ShaniString input);
}