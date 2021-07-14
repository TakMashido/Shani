package takMashido.shani.orders.targetAction;

import takMashido.shani.core.Config;
import takMashido.shani.libraries.Pair;
import takMashido.shani.orders.Action;

import java.util.Map;

/**Action designed to be invoked on one of inner targets.
 * Performs automatic target selection and cost, importanceBias calculation based on distance and existence of target matching user input.*/
public abstract class TargetAction extends Action{
	private TargetActionManager maneger;
	
	private Target bestTarget;
	private Pair<Short,Short> bestCost;
	
	/**Set TargetActionManager used by this TargetAction. It's used to provide list of targets.
	 * @param manager Look above.
	 */
	void setManager(TargetActionManager manager){
		this.maneger=manager;
	}
	
	@Override
	public void init(String name, Map<String,?> parameters){
		super.init(name,parameters);
		
		if(!maneger.targets.isEmpty()){
			int minIndex=0;
			Pair<Short,Short> minCost=maneger.targets.get(0).getSimilarity(name,parameters);
			short minCompoundCost=(short)(minCost.first+Config.importanceBiasMultiplier*minCost.second);
			
			for(int i=0; i<maneger.targets.size(); i++){
				Pair<Short,Short> cost=maneger.targets.get(i).getSimilarity(name,parameters);
				short compoundCost=(short)(cost.first+Config.importanceBiasMultiplier*cost.second);
				
				if(compoundCost<minCompoundCost){
					minIndex=i;
					minCost=cost;
					minCompoundCost=compoundCost;
				}
			}
			
			if(minCost.first<Config.sentenceCompareThreshold){
				bestTarget=maneger.targets.get(minIndex);
				bestCost=minCost;
				
				return;
			}
		}
		
		bestTarget=null;
		bestCost=null;
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
