package takMashido.shani.filters;

import org.w3c.dom.Element;
import takMashido.shani.core.Intend;

/**Shani module to filter input before trying to match it with Order.
 * @author TakMashido
 */
public abstract class IntendFilter {
	
	public IntendFilter(Element e) {}

	public abstract Intend filter(Intend intend);
}