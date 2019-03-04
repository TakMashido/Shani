package shani.orders.templates;

import java.util.List;

import org.w3c.dom.Element;

import shani.ShaniString;

/**Basic method for invocing, and acumulate actions.
 * @author TakMashido
 */
public abstract class Order {
	protected Element orderFile;
	
	public final boolean init(Element e) {
		orderFile=e;
		return init();
	}
	protected abstract boolean init();
	
	/**Prepares list of executables with given command can show.
	 * @param command Command pointing to action.
	 * @return Exectuable list match to given command or null.
	 */
	public abstract List<Executable> getExecutables(ShaniString command);
	
	/**Invoced before saving shani mainFile.*/
	public void save() {};
}