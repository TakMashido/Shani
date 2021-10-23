package takMashido.shani.filters;

import org.w3c.dom.Element;
import takMashido.shani.core.Intend;

/**Shani module to filter input before trying to match it with Order.
 * @author TakMashido
 */
public abstract class IntendFilter {
	/**Load intend filter data from XML node.*/
	public IntendFilter(Element e) {}
	
	/**Filter given intend. Clear unnecessary data, change to more accessible for, or whatever else child class wants to do.
	 * @param intend Intend to filter.
	 * @return Filtered intend.
	 */
	public abstract Intend filter(Intend intend);
}