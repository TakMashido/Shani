package takMashido.shani.orders.targetAction;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**Class responsible for managing TargetAction and Target's.
 * Automate creation of them and supplies TargetAction with necessary static data.*/
public final class TargetActionManager{
	/**List of targets to choose from.*/
	public final List<Target> targets=new ArrayList<>();
	
	/**Root node containing saved targets data*/
	private Element dataRootNode;
	private Supplier<TargetAction> actionFactory;
	
	/**Create new target action manager.
	 * @param dataRootNode Node being root of targets save location.
	 * @param actionFactory Factory of TargetAction's. YourTargetActionClass::new should do the work.
	 * @param targetLoader Factory of targets, Lambda used to load targets from XML node.*/
	public TargetActionManager(Element dataRootNode, Supplier<TargetAction> actionFactory, Function<Element,Target> targetLoader){
		Objects.requireNonNull(dataRootNode,"target data root node can't be null");
		
		this.dataRootNode=dataRootNode;
		
		this.actionFactory=actionFactory;
		
		//Load targets from dataRootNode
		NodeList nodes=dataRootNode.getChildNodes();
		for(int i=0;i<nodes.getLength();i++){
			if(nodes.item(i).getNodeType()!=Node.ELEMENT_NODE)
				continue;
			
			Target target=targetLoader.apply((Element)nodes.item(i));
			if(target!=null)
				targets.add(target);
		}
	}
	
	/**Register target to use it in this TargetActionManager. Call's alos setSaveLocation with new subnode of dataRootNode.
	 * @param target Look above.
	 */
	public void registerNewTarget(Target target){
		Element saveLocation=dataRootNode.getOwnerDocument().createElement("target");
		dataRootNode.appendChild(saveLocation);
		
		target.setSaveElement(saveLocation);
		
		targets.add(target);
	}
	/**Get new TargetAction using this TargetActionManager.
	 * @return Look above.
	 */
	public TargetAction getAction(){
		TargetAction ret=actionFactory.get();
		
		ret.setManager(this);
		
		return ret;
	}
	
	/**Invoke save method on all targets*/
	public void saveTargets(){
		for(Target tar:targets)
			tar.save();
	}
}