package shani.orders.templates;

/**
 *Ready to execute Action with cost beetwen execute pattern and input ShaniString.
 * @author TakMashido
 */
public final class Executable{
	public final short cost;
	public final Action action;
	private boolean succesful=false;
	
	/**Creates Exetuable.
	 * @param action Action to be stored.
	 * @param cost Cost beetwen input and action invoce pattern.
	 */
	public Executable(Action action, short cost){
		this.action=action;
		this.cost=cost;
	}
	
	/**
	 * Executes stored action.
	 * @see Action#execute()
	 */
	public void execute() {
		succesful=action.execute();
	}
	
	/**Check if stored action was succesully executed.
	 * @return If action succesfully executed or false if not executed.
	 */
	public boolean isSuccesful() {
		return succesful;
	}
}