package takMashido.shani.orders.targetAction;

import takMashido.shani.core.Config;
import takMashido.shani.libraries.Pair;
import takMashido.shani.orders.IntendParserAction;

import java.util.Map;

/**Action designed to be invoked on one of inner targets.
 * Performs automatic target selection and cost, importanceBias calculation based on distance and existence of target matching user input.*/
public abstract class TargetAction<T> extends IntendParserAction<T> {
	private TargetActionManager manager;
	
	private Target bestTarget;
	private Pair<Short,Short> bestCost;
	
	/**Set TargetActionManager used by this TargetAction. It's used to provide list of targets.
	 * @param manager Look above.
	 */
	void setManager(TargetActionManager manager){
		this.manager=manager;
	}
	
	@Override
	public void init(String name, Map<String,? extends T> parameters){
		super.init(name,parameters);
		
		Pair<Pair<Short,Short>,Target> best=manager.getTarget(name,parameters);
		if(best==null){
			bestCost=null;
			bestTarget=null;
		} else {
			bestCost=best.first;
			bestTarget=best.second;
		}
	}
	
	@Override
	public boolean execute(){
		if(bestTarget==null)
			return executeNoTarget();
		
		return execute(bestTarget);
	}
	
	/**Perform execution of Action on given Target
	 * @param target Target best matching to input given by user.
	 * @return If successfully executed.
	 */
	protected abstract boolean execute(Target target);
	/**Perform execution of Action when no Target was matched.
	 * @return If successfully executed.
	 */
	protected abstract boolean executeNoTarget();
	
	@Override
	public short getCost(){
		return (short)(super.getCost()+(bestTarget!=null? bestCost.first : 0));
	}
	@Override
	public short getImportanceBias(){
		return (short)(super.getImportanceBias()+(bestTarget!=null?bestCost.second:Config.targetActionNoTargetImportanceBias));
	}
}
