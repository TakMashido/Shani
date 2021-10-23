package takMashido.shani.orders;

import org.w3c.dom.Element;
import takMashido.shani.core.text.SentenceMatcher;
import takMashido.shani.core.text.ShaniString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**@deprecated Use IntendParserOrder with SentenceIntendParser instead.*/
@Deprecated
public abstract class SentenceMatcherOrder extends TextOrder{
	/**{@link SentenceMatcher} used by this SentenceMatcherOrder for matching input sentences.
	 */
	protected SentenceMatcher matcher;
	
	public SentenceMatcherOrder(Element e) {
		super(e);
		matcher=new SentenceMatcher(e.getElementsByTagName("sentence").item(0));
	}
	
	@Override
	public List<Executable> getExecutables(ShaniString command) {
		var results=matcher.process(command);
		ArrayList<Executable> Return=new ArrayList<>();
		
		for(var result:results) {
			for(var action:actionFactoryList(result.getName(),result.data)) {
				action.init(result.getName(),result.data);
				Return.add(action.getExecutable(result.getCost(),result.getImportanceBias()));
			}
		}
		
		return Return;
	}
	
	/**Return Action object used by your Order. It's next initialized by SentenceMatherOrder.
	 * <code>return new YourActionClass();</code> should be sufficient in most cases.
	 * @param sentenceName name of matched sentence.
	 * @param returnValues list of values under return nodes from {@link SentenceMatcher}.
	 * @return SentenceMatcherAction instance matching into Matched sentence.
	 */
	protected SentenceMatcherAction actionFactory(String sentenceName, HashMap<String,String> returnValues) {return null;}
	/**Use if you want to create list of Actions to execute.
	 * @param sentenceName name of matched sentence.
	 * @param returnValues list of values under return nodes from {@link SentenceMatcher}.
	 * @return SentenceMatcherAction instance matching into Matched sentence packed into list.
	 */
	protected List<SentenceMatcherAction> actionFactoryList(String sentenceName, HashMap<String,String> returnValues){
		ArrayList<SentenceMatcherAction> ret=new ArrayList<>();
		ret.add(actionFactory(sentenceName, returnValues));
		return ret;
	}
	
	protected abstract class SentenceMatcherAction extends Action{
		private String sentenceName;
		private HashMap<String,String> returnValues;
		
		/**Cost calculated by child class, added to values determined by underlying SentenceMatcher*/
		public short cost=0;
		/**Importance bias calculated by child class, added to values determined by underlying SentenceMatcher*/
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

		/**Like {@link takMashido.shani.orders.Action#execute()} but with additional parameters from matched sentence. See {@link SentenceMatcher}.
		 * @param sentenceName Name of matched sentence.
		 * @param returnValues Components of matched sentence.
		 * @return If action executed successfully.
		 */
		protected abstract boolean execute(String sentenceName,HashMap<String,String> returnValues);
		
		@Override
		public Executable getExecutable(short cost,short importanceBias) {
			return super.getExecutable(cost+this.cost, importanceBias+this.importanceBias);
		}
	}
}