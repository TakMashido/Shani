package shani.orders.templates;

import java.util.List;

import org.w3c.dom.Element;

import shani.ShaniString;

/**Basic class for parsing input queries.
 * @author TakMashido
 */
public abstract class Order {
	/**Source of data for shani.*/
	@Deprecated
	protected Element orderFile;
	
	/**Main method for initilaizating module.
	 * @param e XML Element represeting this order Object.
	 * @return If successfully initializeted.
	 */
	public final boolean init(Element e) {
		orderFile=e;
		return init();
	}
	/**Method for doing custom initalizations. Override if you want do do some.
	 * @return If successfully initializeted.
	 */
	protected abstract boolean init();
	
	/**Prepares list of executables which are respond for given command query.
	 * @param command Command to interpret.
	 * @return Executable list matching given command or null.
	 */
	public abstract List<Executable> getExecutables(ShaniString command);
	
	/**Invoked before saving shani mainFile.*/
	public void save() {};
}