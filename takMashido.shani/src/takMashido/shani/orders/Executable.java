package takMashido.shani.orders;

import takMashido.shani.core.Cost;

/**
 *Ready to execute Action with cost between execute pattern and input ShaniString.
 * @author TakMashido*/
public final class Executable{
	/**Cost between this executable Action and Intend it's response to.*/
	public final Cost cost;

	/**{@link Action} which this Executable represent*/
	public final Action action;
	/**Says if execution of stored action was successful. It's value returned by Action::execute()*/
	private boolean successful =false;
	
	/**Creates Executable.
	 * @param action Action to be stored.
	 * @param cost Cost between input and action invoke pattern.
	 */
	public Executable(Action action, short cost){
		this(action,cost,(short)0);
	}
	/**Creates Executable.
	 * @param action Action to be stored.
	 * @param cost Cost between input and action invoke pattern.
	 * @param importanceBias Executables with bigger importance bias are more likely to execute, even if they sentences comparison cost is bigger than one of other sentence.
	 */
	public Executable(Action action, short cost, short importanceBias) {
		this.action=action;
		this.cost=new Cost((short)(cost+action.getCost()), (short)(importanceBias+action.getImportanceBias()));
	}
	public Executable(Action action, Cost cost) {
		this.action=action;
		this.cost=cost;
	}

	/**
	 * Executes stored action.
	 * @see Action#execute()
	 */
	public void execute() {
		successful =action.execute();
	}
	
	/**Check if stored action was successfully executed.
	 * @return If action successfully executed or false if not executed.
	 */
	public boolean isSuccessful() {
		return successful;
	}
	
	@Override
	public String toString(){
		return action.toString()+" "+cost.toString();
	}
}