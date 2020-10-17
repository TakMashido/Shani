package shani.orders;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import shani.Config;
import shani.Engine;
import shani.ShaniString;
import shani.orders.templates.Action;
import shani.orders.templates.Executable;
import shani.orders.templates.Order;

public class MergeOrder extends Order{
	protected static ShaniString connectSuccessfulMessage;
	public static ShaniString cantConnectMessage;
	
	private ShaniString connectKey;
	
	@Override
	public boolean init(Element e) {
		connectKey=ShaniString.loadString(e, "key");
		
		connectSuccessfulMessage=ShaniString.loadString(e,"connectSuccessfulMessage");
		cantConnectMessage=ShaniString.loadString(e,"connectSuccessfulMessage");
		
		return true;
	}
	
	@Override
	public List<Executable> getExecutables(ShaniString command) {
		var matcher=command.getMatcher().apply(connectKey);
		//System.out.println("a");
		short cost=matcher.getMatchedCost();
		if(cost<Config.sentenseCompareTreshold) {
			var unMatched=matcher.getUnmatched();
			var executable=Engine.getExecutable(unMatched);
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