package shani.orders.templates;

import java.util.List;

import org.w3c.dom.Element;

import shani.ShaniString;

/**Basic class for parsing input queries.
 * @author TakMashido
 */
public abstract class Order {
	/**Source of data for shani.
	 * use {@link #templateFile} instead.*/
	@Deprecated
	protected Element orderFile;
	
	/**Xml element containing Order data. Description, key words responses.*/
	protected Element templateFile;
	
	/**Main method for initializing module.
	 * @param e XML Element representing this order Object.
	 * @return If successfully initialized.
	 */
	public boolean init(Element e) {
		orderFile=e;
		return init();
	}
	/**Method for doing custom initializations. Override if you want do do some.
	 * @return If successfully initialized.
	 * @deprecated use {@link #init(Element)} instead.
	 */
	protected boolean init() {return false;}
	
	
	/**Prepares list of executables which are respond for given command query.
	 * @param command Command to interpret.
	 * @return Executable list matching given command or null.
	 */
	public abstract List<Executable> getExecutables(ShaniString command);
	
	/**Invoked before saving shani mainFile.*/
	public void save() {};
}