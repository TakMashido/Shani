package takMashido.shani.filters;

import org.w3c.dom.Element;
import takMashido.shani.core.Intend;
import takMashido.shani.core.text.ShaniString;

/**Filter for text based intents.
 * @author TakMashido
 */
public abstract class TextFilter extends IntendFilter {
    public TextFilter(Element e) {
        super(e);
    }

    @Override
    public Intend filter(Intend intend) {
        if(intend.value instanceof ShaniString) {
            var newValue=(ShaniString) intend.value;
            if(newValue!=null)
                intend.value = newValue;
        }
        return intend;
    }

    /**Filter intent text.
     * @param input Text to filter.
     * @return filtered text.
     */
    public abstract ShaniString filter(ShaniString input);
}