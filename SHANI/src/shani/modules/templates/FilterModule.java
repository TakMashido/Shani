package shani.modules.templates;

import org.w3c.dom.Element;

import shani.ShaniString;

/**Shani module to filter input before trying to match it with Order.
 * @author TakMashido
 */
public abstract class FilterModule extends ShaniModule{
	
	public FilterModule(Element e) {
		super(e);
	}

	public abstract ShaniString filter(ShaniString orginalRespond);
}