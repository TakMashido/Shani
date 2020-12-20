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
	 * @return If merge was succesfull.
	 */
	public abstract boolean connectAction(String action);
	/**Execute this action.
	 * @return If succesfully executed
	 */
	public abstract boolean execute();
	
	/**Creates executable from this action. Equivalent to {@link Executable#Executable(Action, short) new Executable(thisAction,cost)}.
	 * @param cost Cost of executing this action.
	 * @return Executable connected to this action with given cost.
	 */
	public final Executable getExecutable(short cost) {
		return new Executable(this,cost);
	}
	/**Creates executable from this action. Equivalent to {@link Executable#Executable(Action, short, short) new Executable(thisAction,cost,importanceBias)}.
	 * @param cost Cost of executing this action.
	 * @param importanceBias importance bias of this action.
	 * @return Executable connected to this action with given cost and importance bias.
	 */
	public Executable getExecutable(short cost, short importanceBias) {
		return new Executable(this,cost,importanceBias);
	}
	
	/**Creates executable from this action. Equivalent to {@link #getExecutable(short) getExecutable((short)cost)}.
	 * @param cost Cost of executing this action.
	 * @return Executable connected to this action with given cost.
	 */
	public final Executable getExecutable(int cost) {
		return getExecutable((short)cost);
	}
	/**Creates executable from this action. Equivalent to {@link Executable#Executable(Action, short, short) new Executable(thisAction,(short)cost,(short)importanceBias)}.
	 * @param cost Cost of executing this action.
	 * @param importanceBias importance bias of this action.
	 * @return Executable connected to this action with given cost and importance bias.
	 */
	public final Executable getExecutable(int cost, int importanceBias) {
		return new Executable(this,(short)cost,(short)importanceBias);
	}
}