package shani.orders.templates;

/**
 *Ready to execute Action with cost beetwen execute pattern and input ShaniString.
 * @author TakMashido
 */
public final class Executable{
	/**Cost beetwen input and action invoke pattern.*/
	public final short cost;
	/**{@link Action} which this Executable represent*/
	public final Action action;
	private boolean succesful=false;
	
	/**Creates Exetuable.
	 * @param action Action to be stored.
	 * @param cost Cost beetwen input and action invoke pattern.
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