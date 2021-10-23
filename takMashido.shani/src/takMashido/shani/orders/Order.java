package takMashido.shani.orders;

import org.w3c.dom.Element;
import takMashido.shani.core.Intend;

import java.util.List;

/**Basic class for parsing input queries.
 * @author TakMashido
 */
public abstract class Order {
	/**Xml element containing Order data. Description, key words responses.*/
	protected Element templateFile;
	
	/**Constructor loading order from xml node
	 * @param e XML node containing data for order creation.*/
	public Order(Element e) {}
	
	/**Prepares list of executables which are respond for given command query.
	 * @param intend Command to interpret.
	 * @return Executable list matching given command or null.
	 */
	public abstract List<Executable> getExecutables(Intend intend);
	
	/**Invoked before saving shani mainFile.*/
	public void save() {};
}