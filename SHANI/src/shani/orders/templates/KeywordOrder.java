package shani.orders.templates;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import shani.Config;
import shani.Engine;
import shani.ShaniString;
import shani.ShaniString.ShaniMatcher;

public abstract class KeywordOrder extends Order {
	protected ShaniString keyword;
	protected ArrayList<KeywordAction> actions=new ArrayList<KeywordAction>();
	
	protected boolean init() {
		keyword=new ShaniString(orderFile.getElementsByTagName("keywords").item(0));
		
		NodeList list=orderFile.getElementsByTagName("target");
		for(int i=0;i<list.getLength();i++) {
			Element elem=(Element)list.item(i);
			actions.add(actionFactory(elem));
		}
		return initialize();
	}
	
	/**Overide if you want do some initializations to your module
	 * @return If succesfuly initializated
	 */
	protected boolean initialize() {return false;}
	
	/**Creates Action object from specyfic xml Element
	 * @param element Element containg Action data
	 * @return Ready to use Action
	 */
	public abstract KeywordAction actionFactory(Element element);
	public KeywordAction createAction(Element element){
		KeywordAction action=actionFactory(element);
		
		if(action.actionKeyword==null)action.actionKeyword=new ShaniString(element.getElementsByTagName("key").item(0));
		
		return action;
	}
	
	
	public List<Executable> getExecutables(ShaniString command) {
		ArrayList<Executable> Return=new ArrayList<Executable>();
		
		ShaniMatcher matcher=command.getMatcher().apply(keyword);
		
		Engine.info.println('\n'+keyword.toFullString()+"-> "+command.toFullString()+":");
		for(KeywordAction action:actions) {
			ShaniMatcher actionMatcher=matcher.clone().apply(action.actionKeyword);
			short cost=actionMatcher.getCost();
			if(cost<Config.sentenseCompareTreshold) {
				Return.add(new Executable(action,cost));
			}
			if(cost<Config.sentenseCompareTreshold*2)
				Engine.info.println(action.actionKeyword.toFullString()+"= "+cost);
		}
		Engine.info.println();
		
		if(Return.size()==0&&matcher.isSemiEqual()) {
			var add=getUnmatchedAction();
			if(add!=null) {
				add.setUnmatched(matcher.getUnmatched());
				Return.add(add.getExecutable(Config.sentenseCompareTreshold-1));
			}
		}
		
		List<Executable> additional=createExecutables(command,matcher);
		if(additional!=null)Return.addAll(additional);
		
		return Return;
	}
	/**Method for creation of cuscom Executables
	 * @param command Command from user
	 * @param matcher ShaniMatcher created from command with applied keyword
	 * @return List of addition executables for specyfied command.
	 */
	public List<Executable> createExecutables(ShaniString command, ShaniMatcher matcher){
		return null;
	}
	
	public UnmatchedAction getUnmatchedAction() {return null;}
	
	public abstract class UnmatchedAction extends Action {
		protected ShaniString unmatched;
		
		private void setUnmatched(ShaniString newUnmatched) {
			unmatched=newUnmatched;
		}
	}
	/**Keyword based action.
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
			actionFile=Engine.doc.createElement("action");
			orderFile.appendChild(actionFile);
			
			Element e=Engine.doc.createElement("key");
			actionFile.appendChild(e);
			//e.appendChild(Engine.doc.createTextNode(keyword.toFullString()));
			actionKeyword=keyword.copy();
			actionKeyword.setNode(e);
			
			e=Engine.doc.createElement("merged");
			actionFile.appendChild(e);
			mergedActions=new ShaniString(e);
			
			actions.add(this);
		}
		
		/* (non-Javadoc)
		 * Classes overiding {@link shani.orders.KeywordOrder.KeywordAction} shouldn't override this method. Use {@link #keywordExecute()} instead.
		 * @see shani.orders.Order.Action#execute()
		 */
		public boolean execute() {
			boolean Return=keywordExecute();
			if(!Return)return false;
			for(var command:mergedActions.getArray()) {
				Engine.interprete(command);
			}
			return true;
		}
		/**Basic method for executing KeywordAction.
		 * @return If action was successfuly exeuted.
		 */
		public abstract boolean keywordExecute();
		
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