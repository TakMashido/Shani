package shani.orders.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import shani.SentenceMatcher;
import shani.ShaniString;

public abstract class SentenceMatcherOrder extends Order{
	protected SentenceMatcher matcher;
	
	@Override
	protected boolean init() {
		matcher=new SentenceMatcher(orderFile.getElementsByTagName("sentence").item(0));
		return initialize();
	}
	
	/**Override to do some initializations in your module.
	 * @return If successfully initializeted.
	 */
	protected boolean initialize() {return true;}
	
	@Override
	public List<Executable> getExecutables(ShaniString command) {
		var resoults=matcher.process(command);
		ArrayList<Executable> Return=new ArrayList<>();
		
		for(var resoult:resoults) {
			var action=actionFactory(resoult.getName(),resoult.data);
			action.init(resoult.getName(),resoult.data);
			Return.add(action.getExecutable(resoult.getCost(),resoult.getImportanceBias()));
		}
		
		return Return;
	}
	
	/**Has to return new Action object used by your Order. It's next initialized by SentenceMatherOrder.
	 * <code>return new YourActionClass();</code> should be sufficient in most cases.
	 */
	protected abstract SentenceMatcherAction actionFactory(String sentenceName, HashMap<String,String> returnValues);
	
	protected abstract class SentenceMatcherAction extends Action{
		private String sentenceName;
		private HashMap<String,String> returnValues;
		
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
	}
}