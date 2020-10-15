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
	 * @deprecated {@link #initialize(Element)} taken it's place.
	 */
	public final boolean init(Element e) {
		orderFile=e;
		return init();
	}
	/**Method for doing custom initializations. Override if you want do do some.
	 * @return If successfully initialized.
	 */
	protected abstract boolean init();
	
	/**Main method for initializing object.
	 * @param e XML element being root of this Order template.*/
	public void initialize(Element e) {};					//TODO make abstract when dadta storage refactored 
	
	/**Prepares list of executables which are respond for given command query.
	 * @param command Command to interpret.
	 * @return Executable list matching given command or null.
	 */
	public abstract List<Executable> getExecutables(ShaniString command);
	
	/**Invoked before saving shani mainFile.*/
	public void save() {};
}