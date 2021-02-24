package takMashido.shani.orders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Element;

import takMashido.shani.core.text.SentenceMatcher;
import takMashido.shani.core.text.ShaniString;

public abstract class SentenceMatcherOrder extends Order{
	protected SentenceMatcher matcher;
	
	public SentenceMatcherOrder(Element e) {
		super(e);
		matcher=new SentenceMatcher(e.getElementsByTagName("sentence").item(0));
	}
	
	/**Override to do some initializations in your module.
	 * @return If successfully initialized.
	 */
	protected boolean initialize(Element e) {return true;}
	
	/**Override to do some initializations in your module.
	 * @return If successfully initialized.
	 * @deprecated Use initialize(Element) instead.
	 */
	@Deprecated
	protected boolean initialize() {return true;}
	
	@Override
	public List<Executable> getExecutables(ShaniString command) {
		var resoults=matcher.process(command);
		ArrayList<Executable> Return=new ArrayList<>();
		
		for(var resoult:resoults) {
			for(var action:actionFactoryList(resoult.getName(),resoult.data)) {
				action.init(resoult.getName(),resoult.data);
				Return.add(action.getExecutable(resoult.getCost(),resoult.getImportanceBias()));
			}
		}
		
		return Return;
	}
	
	/**Return Action object used by your Order. It's next initialized by SentenceMatherOrder.
	 * <code>return new YourActionClass();</code> should be sufficient in most cases.
	 * @param sentenceName name of matched sentence.
	 * @param returnValues list of values under return nodes from {@link SentenceMatcher}.
	 */
	protected SentenceMatcherAction actionFactory(String sentenceName, HashMap<String,String> returnValues) {return null;}
	/**Use if you want to create list of Actions to execute.
	 * @param sentenceName name of matched sentence.
	 * @param returnValues list of values under return nodes from {@link SentenceMatcher}.
	 */
	protected List<SentenceMatcherAction> actionFactoryList(String sentenceName, HashMap<String,String> returnValues){
		ArrayList<SentenceMatcherAction> ret=new ArrayList<>();
		ret.add(actionFactory(sentenceName, returnValues));
		return ret;
	}
	
	protected abstract class SentenceMatcherAction extends Action{
		private String sentenceName;
		private HashMap<String,String> returnValues;
		
		public short cost=0;							//Cost and importance bias calculated by child class, added to values determined by underlying SeteceMatcher
		public short importanceBias=0;
		
		private void init(String sentenceName, HashMap<String,String> returnValues) {
			this.sentenceName=sentenceName;
			this.returnValues=returnValues;
		}
		
		@Override
		public boolean connectAction(String action) {
			System.err.println("Can't connect action to SentenceMatcherOrder.SentenceAction");
			assert false:"Can't connect action to SentenceMatcherOrder.SentenceAction";
			return false;
		}

		@Override
		public boolean execute() {
			return execute(sentenceName,returnValues);
		}
		
		protected abstract boolean execute(String sentenceName,HashMap<String,String> returnValues);
		
		@Override
		public Executable getExecutable(short cost,short importanceBias) {
			return super.getExecutable(cost+this.cost, importanceBias+this.importanceBias);
		}
	}
}