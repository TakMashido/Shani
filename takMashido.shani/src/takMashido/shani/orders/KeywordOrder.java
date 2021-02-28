package takMashido.shani.orders;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import takMashido.shani.Config;
import takMashido.shani.Engine;
import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.Storage;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.core.text.ShaniString.ShaniMatcher;

import java.util.ArrayList;
import java.util.List;

/**Represents order activated by a single keyword.
 * Supports creating multiple targets with it's own keywords represented by {@code KeywordAction} class.
 * Creation of KeywordAction registers it in inner targets List and mainFile Document object.
 * <p>
 * To execute Actions do not having specified keywords override {@link UnmatchedAction} class and return it's instance from {@link #getUnmatchedAction(ShaniString)} or {@link #getUnmatchedAction()} method. 
 * <p>
 * Loading of targets stored in MainFile is done in {@link #actionFactory(Element)}.
 * 
 * @author TakMashido
 */
public abstract class KeywordOrder extends TextOrder {
	/**Keyword indicating user intent is for this Order instance.*/
	protected ShaniString keyword;
	/**List of possible to execute actions initialized with their specific keywords.*/
	protected ArrayList<KeywordAction> actions=new ArrayList<>();
	
	/**If true then exact matching of targets keywords({@link ShaniMatcher#exactApply(ShaniString...)}) is applied, otherwise standard fuzzy ShaniString matching({@link ShaniMatcher#apply(ShaniString...)})*/
	protected boolean targetExactMatch=false;
	
	protected Node targetDataNode;
	
	/**Where to search for targets. Each subnode under given path is treated as differed target, and {@link KeywordACtion} is created for it.
	 * @return String[] containing paths to use.*/
	protected abstract String getDataLocation();
	
	public KeywordOrder(Element e) {
		super(e);
		
		keyword=new ShaniString(e.getElementsByTagName("keywords").item(0));
		
		String dataLocation=getDataLocation();
		if(dataLocation!=null) {
			NodeList list=Storage.getNodes(getDataLocation());
			targetDataNode=list.item(0);
			list=targetDataNode.getChildNodes();
			
			for(int i=0;i<list.getLength();i++) {
				if(list.item(i).getNodeType()!=Node.ELEMENT_NODE)
					continue;
				Element elem=(Element)list.item(i);
				if(elem==null)continue;
				actions.add(actionFactory(elem));
			}
		}
	}
	
	/**Override if you want do some initializations to your module.
	 * @param e Xml element containing initialization data for this order.
	 * @return If successfully initialized
	 */
	protected boolean initialize(Element e) {return true;}
	
	/**Creates Action object from specific xml Element
	 * @param element Element containing Action data
	 * @return Ready to use Action
	 */
	public abstract KeywordAction actionFactory(Element element);
	
	@Override
	public List<Executable> getExecutables(ShaniString command) {
		ArrayList<Executable> Return=new ArrayList<Executable>();
		
		ShaniMatcher matcher=command.getMatcher().apply(keyword);
		
		short minCost=Short.MAX_VALUE;
		KeywordAction minAction=null;
		
		Engine.info.printf("KeywordOrderNG: %s:\n",keyword.toFullString());
		for(KeywordAction action:actions) {
			ShaniMatcher actionMatcher=matcher.clone();
			if(targetExactMatch) 
				actionMatcher.exactApply(action.actionKeyword);
			else
				actionMatcher.apply(action.actionKeyword);
			
			short cost=actionMatcher.getCost();
			if(cost<minCost) {
				minCost=cost;
				minAction=action;
			}
			
			if(cost<Config.sentenseCompareTreshold*1.5f)
				Engine.info.println(action.actionKeyword.toFullString()+"= "+cost);
		}
		Engine.info.println();
		
		if(minCost<Config.sentenseCompareTreshold) {
			Return.add(new Executable(minAction,minCost));
		} else if(matcher.isSemiEqual()) {
			ShaniString unmatched=matcher.getUnmatched();
			var add=getUnmatchedAction(unmatched);
			if(add!=null) {
				add.setUnmatched(unmatched);
				Return.add(add.getExecutable(Config.sentenseCompareTreshold-1));
			}
		}
		
		List<Executable> additional=createExecutables(command,matcher);
		if(additional!=null)Return.addAll(additional);
		
		return Return;
	}
	protected KeywordAction getAction(ShaniString command) {
		short minCost=Short.MAX_VALUE;
		KeywordAction minAction=null;
		
		for(var action:actions) {
			short cost=action.actionKeyword.getCompareCost(command);
			
			if(cost<minCost) {
				minCost=cost;
				minAction=action;
			}
		}
		
		if(minCost<Config.sentenseCompareTreshold)
			return minAction;
		return null;
	}
	/**Method for creation of custom Executables.
	 * @param command Command from user
	 * @param matcher ShaniMatcher created from command with applied keyword
	 * @return List of additional executables for specified command.
	 */
	protected List<Executable> createExecutables(ShaniString command, ShaniMatcher matcher){
		return null;
	}
	
	/**Use to handle inputs matching key of order, but not matching any target
	 * @param unmatched Input command without key of this Order.
	 * @return Custom order for handling unmatched data.
	 */
	protected UnmatchedAction getUnmatchedAction(ShaniString unmatched) {return getUnmatchedAction();}
	/**Use to handle inputs matching key of order, but not matching any target
	 * @return Custom order for handling unmatched data.
	 */
	protected UnmatchedAction getUnmatchedAction() {return null;}
	
	public abstract class UnmatchedAction extends Action {
		/**Value corresponding to this action. It's original intend except used keyword of Order.*/
		protected ShaniString unmatched;
		
		@Override
		public boolean connectAction(String action) {
			assert false:"Connecting actions to "+this.getClass().getName()+"is not possible";
			System.err.println("Connecting actions to "+this.getClass().getName()+"is not possible");
			return false;
		}
		
		/**Set value corresponding to this action. It's original intend except used keyword of Order.*/
		protected final void setUnmatched(ShaniString newUnmatched) {
			unmatched=newUnmatched;
		}
	}
	/**Keyword based action.
	 * Override {@link KeywordAction#keywordExecute()} to define behavior.
	 * @author TakMashido
	 */
	public abstract class KeywordAction extends Action{
		protected ShaniString actionKeyword;
		protected ShaniString mergedActions;
		
		/**Super constructor for loading Action.
		 * @param elem Element representing this Action.
		 */
		protected KeywordAction(Element elem) {
			actionFile=elem;
			actionKeyword=new ShaniString(elem.getElementsByTagName("key").item(0));
			mergedActions=new ShaniString(elem.getElementsByTagName("merged").item(0));
		}
		/**SuperConstructor for creating new Action.
		 * @param keyword Keyword for new Action.
		 */
		protected KeywordAction(ShaniString keyword) {
			var doc=targetDataNode.getOwnerDocument();
			
			actionFile=doc.createElement("executable");
			targetDataNode.appendChild(actionFile);
			
			Element e=doc.createElement("key");
			actionFile.appendChild(e);
			//e.appendChild(Engine.doc.createTextNode(keyword.toFullString()));
			actionKeyword=keyword.copy();
			actionKeyword.setNode(e);
			
			e=doc.createElement("merged");
			actionFile.appendChild(e);
			mergedActions=new ShaniString(e);
			
			actions.add(this);
		}
		
		/**
		 * Classes overriding {@link KeywordAction} shouldn't override this method. Use {@link #keywordExecute()} instead.
		 * @see takMashido.shani.orders.Action#execute()
		 */
		@Override
		public boolean execute() {
			boolean Return=keywordExecute();
			if(!Return)return false;
			for(var command:mergedActions.getArray()) {
				ShaniCore.interpret(command);
			}
			return true;
		}
		/**Basic method for executing KeywordAction.
		 * @return If action was successfully executed.
		 */
		public abstract boolean keywordExecute();
		
		@Override
		public boolean connectAction(String action) {
			mergedActions.add(action);
			return true;
		}
		
		public void addKey(ShaniString key) {
			actionKeyword.add(key);
		}
		
		public boolean isEqual(ShaniString com) {
			return com.getMatcher().apply(actionKeyword).isEqual();
		}
	}
}