package takMashido.shani.orders;

/**
 *Ready to execute Action with cost between execute pattern and input ShaniString.
 * @author TakMashido
 */
public final class Executable{
	/**Cost between input and action invoke pattern.*/
	public final short cost;
	/***/
	public final short importanceBias;
	/**{@link Action} which this Executable represent*/
	public final Action action;
	private boolean succesful=false;
	
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
		this.cost=(short)(cost+action.getCost());
		this.importanceBias=(short)(importanceBias+action.getImportanceBias());
	}
	
	/**
	 * Executes stored action.
	 * @see Action#execute()
	 */
	public void execute() {
		succesful=action.execute();
	}
	
	/**Check if stored action was successfully executed.
	 * @return If action successfully executed or false if not executed.
	 */
	public boolean isSuccesful() {
		return succesful;
	}
	
	@Override
	public String toString(){
		return action.toString()+" costs:"+cost+":"+importanceBias;
	}
}