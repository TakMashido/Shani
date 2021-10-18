package takMashido.shani.orders;

import org.w3c.dom.Element;
import takMashido.shani.core.Cost;
import takMashido.shani.core.ShaniCore;

/**
 *Contain specyfic action to execute.
 * @author TakMashido
 */
public abstract class Action{
	@Deprecated			//Used only in KeywordOrder, find better way to store and parse target based Actions
	protected Element actionFile;

	/**Connects this Action to another.
	 * Connected actions will be invoced after invocing master one.
	 * @param action String representing action being connected.
	 * @return If merge was succesfull.
	 * @deprecated Merging now uses {@link #hashCode()} to recognize action. It's not necessary to do connecting in Action code.
	 */
	@Deprecated
	public boolean connectAction(String action){
		ShaniCore.errorOccurred("New action connecting is coming so this is not implemented.");
		return false;
	}

	/**Execute this action.
	 * @return If succesfully executed
	 */
	public abstract boolean execute();
	
	/**Get additional cost of executing this Action calculated by Action itself, not IntendParsers.
	 * @return Look above.
	 * @deprecated Use getCostBias instead.
	 */
	@Deprecated
	public short getCost(){
		return 0;
	}
	/**Get additional importanceBias of executing this Action calculated by Action itself, not IntendParsers.
	 * @return Look above.
	 * @deprecated Use getCostBias instead.
	 */
	@Deprecated
	public short getImportanceBias(){
		return 0;
	}

	/**Get additional cost of executing this Action.
	 * @return Cost bias of execution calculated by Action itself, not being part of IntendBase matching cost.
	 */
	public Cost getCostBias(){
		return Cost.FREE;
	}

	/**Return if Action can participate in MergeOrder create chains.
	 * By defaults it's returning true. Merge order uses {@link #hashString()} value to check merges, and if it returns default value it assumes merging is not supported.
	 * @return If this Action can connect to another.
	 */
	public boolean canMerge(){
		return true;
	}

	/**Create string representing this Action.
	 * Should contain all important parameters of Action.
	 * By default it returns empty string.
	 * Do not use it as regular hash. Use {@link #hashStringCode()} instead, it handles default value of this function.
	 * @return String based hash of Action.
	 */
	public String hashString(){
		return "";
	}
	/**HashString of this Action.
	 * It returns {@link #hashString()} or String representation og {@link #hashCode()} if first one is empty.
	 * @return Look above.
	 */
	public String hashStringCode(){
		String hash=hashString();

		if(hash.isEmpty())
			return Integer.toString(hashCode());

		return hash;
	}

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

	 /**Creates executable from this action. Equivalent to {@link Executable#Executable(Action, Cost) new Executable(thisAction,Cost)}.
	 * @param cost Cost of this Action.
	 * @return Executable connected to this action with given cost.
	 */
	public final Executable getExecutable(Cost cost){
		return new Executable(this, cost);
	}
}