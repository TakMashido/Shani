package takMashido.shani.orders.core;

import org.w3c.dom.Element;
import takMashido.shani.Config;
import takMashido.shani.Engine;
import takMashido.shani.core.Intend;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.orders.Action;
import takMashido.shani.orders.Executable;
import takMashido.shani.orders.TextOrder;

import java.util.ArrayList;
import java.util.List;

public class MergeOrder extends TextOrder {
	protected static ShaniString connectSuccessfulMessage;
	public static ShaniString cantConnectMessage;
	
	private ShaniString connectKey;
	
	public MergeOrder(Element e) {
		super(e);
		
		connectKey=ShaniString.loadString(e, "key");
		
		connectSuccessfulMessage=ShaniString.loadString(e,"connectSuccessfulMessage");
		cantConnectMessage=ShaniString.loadString(e,"connectSuccessfulMessage");
	}
	
	@Override
	public List<Executable> getExecutables(ShaniString command) {
		var matcher=command.getMatcher().apply(connectKey);
		//System.out.println("a");
		short cost=matcher.getMatchedCost();
		if(cost< Config.sentenseCompareTreshold) {
			var unMatched=matcher.getUnmatched();
			var executable=Engine.getExecutable(new Intend(unMatched));
			if(Engine.getLastExecuted()!=null&&executable!=null) {
				var list=new ArrayList<Executable>();
				list.add(new MergeAction(unMatched,executable,Engine.getLastExecuted()).getExecutable((short)(cost+executable.cost)));
				return list;
			}
		}
		
		return null;
	}
	
	protected class MergeAction extends Action{
		private Executable old;
		private Executable newO;
		private String command;
		
		protected MergeAction(ShaniString data, Executable forConnect, Executable whereConnect) {
			command=data.toString();
			newO=forConnect;
			old=whereConnect;
		}
		
		@Override
		public boolean connectAction(String action) {
			assert false:"Can't connect action to shani.orders.MergeOrder.MergeAction";
			System.err.println("Can't connect action to shani.orders.MergeOrder.MergeAction");
			return false;
		}
		
		@Override
		public boolean execute() {
			if(old.action.connectAction(command))
				System.out.println(connectSuccessfulMessage);
			else System.out.println(cantConnectMessage);
			newO.execute();
			
			return false;				//Always returns false to prevent from connectiong order to this order
		}
	}
}