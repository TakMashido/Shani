package shani.orders.templates;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import shani.Config;
import shani.Engine;
import shani.ShaniString;

public abstract class MultipleKeywordOrder extends Order{
	protected ShaniString keyword;
	protected ArrayList<OrderTarget> orderTargets;
	protected ArrayList<OrderAction> orders;
	
	@Override
	protected boolean init() {
		keyword=new ShaniString(orderFile.getElementsByTagName("keyword").item(0));
		
		var orderList=orderFile.getElementsByTagName("suborder");
		orders=new ArrayList<OrderAction>(orderList.getLength());
		for(int i=0;i<orderList.getLength();i++) {
			orders.add(new OrderAction((Element)orderList.item(i)));
		}
		
		orderList=orderFile.getElementsByTagName("target");
		orderTargets=new ArrayList<OrderTarget>(orderList.getLength());
		for(int i=0;i<orderList.getLength();i++) {
			orderTargets.add(targetFactory((Element)orderList.item(i)));
		}
		var addTargets=additionalTargets();
		if(addTargets!=null) {
			orderTargets.addAll(addTargets);
		}
		
		return initialize();
	}
	
	/**Ovride if you want do some initializations to your module
	 * @return If succesfuly initializated
	 */
	protected boolean initialize() {return false;}
	
	/**Override if you want to creted handler for not specyfied target.
	 * @return
	 */
	protected NotMatchedTarget getUnmatchedTarget() {return null;}
	@Override
	public List<Executable> getExecutables(ShaniString command) {
		var matcher=command.getMatcher().apply(keyword);
		if(!matcher.isSemiEqual())return null;
		
		boolean anyMatched=false;
		boolean[] targetMatched=new boolean[orderTargets.size()];
		for(int i=0;i<targetMatched.length;i++) {
			boolean matched=matcher.clone().apply(orderTargets.get(i).targetKeyword).isSemiEqual();
			targetMatched[i]=matched;
			anyMatched=matched;
		}
		
		boolean[] ordersMatched=new boolean[orders.size()];
		for(int i=0;i<ordersMatched.length;i++) {
			ordersMatched[i]=matcher.clone().apply(orders.get(i).keyword).isSemiEqual();
		}
		if(!anyMatched) {
			var Return=new ArrayList<Executable>();
			for(int i=0;i<ordersMatched.length;i++) {
				if(!ordersMatched[i])continue;
				var notMatchedTarget=getUnmatchedTarget();
				if (notMatchedTarget==null)continue;
				notMatchedTarget.setUnmatched(matcher.clone().apply(orders.get(i).keyword).getUnmatched());
				Return.add(new MultipleKeywordAction(notMatchedTarget,orders.get(i)).getExecutable(Config.sentenseCompareTreshold-2));				//Create executable have cost Config.sentenceCompareTreshold-1, making it -2 there make sure i'll be executed if timerkeyword exist in command
			}
			return Return;
		}
		
		short cost;
		var Return=new ArrayList<Executable>();
		for(int i=0;i<targetMatched.length;i++) {
			if(!targetMatched[i])continue;
			for(int j=0;j<ordersMatched.length;j++) {
				if(!ordersMatched[j])continue;
				if((cost=matcher.clone().apply(orderTargets.get(i).targetKeyword,orders.get(j).keyword).getCost())<Config.sentenseCompareTreshold) {
					Return.add(new MultipleKeywordAction(orderTargets.get(i),orders.get(j)).getExecutable(cost));
				}
			}
		}
		
		return Return;
	}
	
	protected abstract OrderTarget targetFactory(Element e);
	
	/**Override if you want to aditional targets during initializng module.
	 * @return List of additional Targets.
	 */
	protected List<OrderTarget> additionalTargets(){return null;}
	
	protected final class OrderAction{
		protected Element orderFile;
		private ShaniString keyword;
		private String methodName;
		
		protected OrderAction(Element e) {
			orderFile=e;
			keyword=new ShaniString(e.getElementsByTagName("keyword").item(0));
			methodName=e.getElementsByTagName("method").item(0).getTextContent();
		}
		
		protected void execute(OrderTarget target) {
			target.invoke(methodName);
		}
	}
	protected abstract class OrderTarget{
		protected Element targetFile;
		protected ShaniString targetKeyword;
		
		private OrderTarget() {}
		/**Loads target from given Element.
		 * @param e Element in mainFile containing this target description
		 */
		protected OrderTarget(Element e) {
			targetFile=e;
			targetKeyword=new ShaniString(targetFile.getElementsByTagName("keyword").item(0));
		}
		/**Creates new target.
		 * @param keyword Keyword which will be used to represent this target.
		 */
		protected OrderTarget(ShaniString keyword) {
			targetKeyword=keyword.copy();
			targetFile=Engine.doc.createElement("target");
			orderFile.appendChild(targetFile);
			
			Element e=Engine.doc.createElement("key");
			targetFile.appendChild(e);
			orderTargets.add(this);
			targetKeyword.setNode(e);
		}
		
		private final void invoke(String name) {
			try {
				var met=getClass().getDeclaredMethod(name);
				Engine.debug.println("debug: "+name);
				met.setAccessible(true);
				met.invoke(this);
			} catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
				e.printStackTrace();
			}
		}
	}
	protected abstract class NotMatchedTarget extends OrderTarget{
		protected ShaniString unmatched;
		
		private void setUnmatched(ShaniString newUnmatched) {
			unmatched=newUnmatched;
		}
	}
	private final class MultipleKeywordAction extends Action{
		private final OrderTarget target;
		private final OrderAction action;
		
		private MultipleKeywordAction(OrderTarget target, OrderAction action) {
			this.target=target;
			this.action=action;
		}
		
		@Override
		public boolean connectAction(String action) {
			System.err.println("Conecting commands to shani.orders.tempaltes.MultipleKeywordOrder.MultipleKeywordAction is not supported");
			return false;
		}
		@Override
		public boolean execute() {
			action.execute(target);
			return false;
		}
	}
}