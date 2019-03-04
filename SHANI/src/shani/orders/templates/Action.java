package shani.orders.templates;

import org.w3c.dom.Element;

/**
 *Contain specyfic action to execute.
 * @author TakMashido
 */
public abstract class Action{
	protected Element actionFile;
	/**Connects this Action to another.
	 * Connected actions will be invoced after invocing master one.
	 * @param action String representing action being connected.
	 * @return If marge was succesfull.
	 */
	public abstract boolean connectAction(String action);
	/**Execute specyfic action
	 * @return If succesful executed
	 */
	public abstract boolean execute();
	
	/**Creates executable from this action. Equvialent to new Executable(thisAction,cost);
	 * @param cost Cost of executing this action
	 * @return Executable connected to this action with given cost
	 */
	public final Executable getExecutable(short cost) {
		return new Executable(this,cost);
	}
	/**Creates executable from this action. Equvialent to getExecutable((short)cost);
	 * @param cost Cost of executing this action
	 * @return Executable connected to this action with given cost
	 */
	public final Executable getExecutable(int cost) {
		return getExecutable((short)cost);
	}
	
}