package takMashido.shani.filters;

import org.w3c.dom.Element;
import takMashido.shani.core.Intent;
import takMashido.shani.core.text.ShaniString;

public abstract class TextFilter extends IntentFilter{
    public TextFilter(Element e) {
        super(e);
    }

    @Override
    public Intent filter(Intent intent) {
        if(intent.value instanceof ShaniString)
            intent.value=filter((ShaniString)intent.value);
        return intent;
    }

    public abstract ShaniString filter(ShaniString input);
}