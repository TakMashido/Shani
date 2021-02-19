package shani.orders.templates;

import java.util.List;

import org.w3c.dom.Element;

import shani.ShaniString;

/**Basic class for parsing input queries.
 * @author TakMashido
 */
public abstract class Order {
	/**Xml element containing Order data. Description, key words responses.*/
	protected Element templateFile;
	
	/**Constructor loading order from xml node*/
	public Order(Element e) {}
	
	/**Prepares list of executables which are respond for given command query.
	 * @param command Command to interpret.
	 * @return Executable list matching given command or null.
	 */
	public abstract List<Executable> getExecutables(ShaniString command);
	
	/**Invoked before saving shani mainFile.*/
	public void save() {};
}