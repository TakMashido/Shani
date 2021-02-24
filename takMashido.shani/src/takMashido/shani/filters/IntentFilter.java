package takMashido.shani.filters;

import org.w3c.dom.Element;

import takMashido.shani.core.text.ShaniString;

/**Shani module to filter input before trying to match it with Order.
 * @author TakMashido
 */
public abstract class IntentFilter{
	
	public IntentFilter(Element e) {}

	public abstract ShaniString filter(ShaniString orginalRespond);
}