package shani.orders.templates;

import java.util.List;

import org.w3c.dom.Element;

import shani.ShaniString;

/**Basic class for parsing input queries.
 * @author TakMashido
 */
public abstract class Order {
	protected Element orderFile;
	
	/**Main method for initilaizating module.
	 * @param e XML Element represeting this order Object.
	 * @return If successfully initializeted.
	 */
	public final boolean init(Element e) {
		orderFile=e;
		return init();
	}
	/**Method for doing cuscom initalizations. Override if you want do do some.
	 * @return If successully initializeted.
	 */
	protected abstract boolean init();
	
	/**Prepares list of executables which are respond for given command query.
	 * @param command Command to interprete.
	 * @return Exectuable list matching given command or null.
	 */
	public abstract List<Executable> getExecutables(ShaniString command);
	
	/**Invoced before saving shani mainFile.*/
	public void save() {};
}