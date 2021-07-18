package takMashido.shani.orders;

import org.w3c.dom.Element;
import takMashido.shani.core.ShaniCore;

import java.util.Map;

/**
 *Contain specyfic action to execute.
 * @author TakMashido
 */
public abstract class Action{
	@Deprecated			//Used only in KeywordOrder, find better way to store and parse target based Actions
	protected Element actionFile;
	
	/**Name of Intend match, used to determine what exactly has to be done.*/
	protected String name;
	/**Contain parameters for execution isolated by ExecutableGetter or it's underlying interpreter.*/
	protected Map<String,? extends Object> parameters;
	
	/**Connects this Action to another.
	 * Connected actions will be invoced after invocing master one.
	 * @param action String representing action being connected.
	 * @return If merge was succesfull.
	 */
	public boolean connectAction(String action){
		ShaniCore.errorOccurred("New action connecting is coming so this is not implemented.");
		return false;
	}
	/**Execute this action.
	 * @return If succesfully executed
	 */
	public abstract boolean execute();
	
	/**Used by ExecutableGetters to inject information provided by user in Intend.
	 * @param name Name of Intend match, used to determine what exactly has to be done.
	 * @param parameters Parameters with additional information about what to execute.
	 */
	public void init(String name, Map<String,? extends Object> parameters){
		this.name=name;
		this.parameters=parameters;
	}
	
	/**Get additional cost of executing this Action calculated by Action itself, not IntendParsers.
	 * @return Look above.
	 */
	public short getCost(){
		return 0;
	}
	/**Get additional importanceBias of executing this Action calculated by Action itself, not IntendParsers.
	 * @return Look above.
	 */
	public short getImportanceBias(){
		return 0;
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
}