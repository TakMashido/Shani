package takMashido.shaniModules.orders;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.Storage;
import takMashido.shani.core.Tests;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.orders.Action;
import takMashido.shani.orders.IntendParserOrder;
import takMashido.shani.orders.targetAction.KeywordTarget;
import takMashido.shani.orders.targetAction.Target;
import takMashido.shani.orders.targetAction.TargetAction;
import takMashido.shani.orders.targetAction.TargetActionManager;
import takMashido.shani.tools.parsers.TimeParser;

public class TimerOrder extends IntendParserOrder{
	private ShaniString startMessage;
	private ShaniString alreadyRunningMessage;
	private ShaniString stopMessage;
	private ShaniString alreadyStoppedMessage;
	private ShaniString resetMessage;
	private ShaniString printTimeMessage;
	
	private ShaniString nonExistStartMessage;
	private ShaniString nonExistStopMessage;
	private ShaniString nonExistResetMessage;
	private ShaniString nonExistPrintTimeMessage;
	
	private TargetActionManager manager;
	
	private Node dataElement;			//node inside ordersData in shaniData
	
	public TimerOrder(Element e) {
		super(e);
		
		startMessage=ShaniString.loadString(e,"startMessage");
		alreadyRunningMessage=ShaniString.loadString(e,"alreadyRunningMessage");
		stopMessage=ShaniString.loadString(e,"stopMessage");
		alreadyStoppedMessage=ShaniString.loadString(e,"alreadyStoppedMessage");
		resetMessage=ShaniString.loadString(e,"resetMessage");
		printTimeMessage=ShaniString.loadString(e,"printTimeMessage");
		
		nonExistStartMessage=ShaniString.loadString(e,"nonExistStartMessage");
		nonExistStopMessage=ShaniString.loadString(e,"nonExistStopMessage");
		nonExistResetMessage=ShaniString.loadString(e,"nonExistResetMessage");
		nonExistPrintTimeMessage=ShaniString.loadString(e,"nonExistPrintTimeMessage");
		
		dataElement=Storage.getOrderData(this);
		
		Node timersNode=Storage.getNode(dataElement, "timers");
		if(timersNode==null){
			timersNode=dataElement.getOwnerDocument().createElement("timers");
			dataElement.appendChild(timersNode);
		}
		
		manager=new TargetActionManager((Element)timersNode, TimerAction::new, Timer::new);
	}
	
	@Override
	public Action getAction(){
		return manager.getAction();
	}
	
	private class TimerAction extends TargetAction{
		@Override
		protected boolean execute(Target target){
			if(!(target instanceof Timer)){
				ShaniCore.errorOccurred("Invalid target class detected in TimerAction.execute(Target). It has to be Timer instance.");
				return false;
			}
			Timer timer=(Timer)target;
			
			Tests.addResults("operation",name);
			Tests.addResults("timerName",timer.keyword.toString());
			
			switch (name) {
				case "start":
					timer.start(true);
					break;
				case "stop":
					timer.stop();
					break;
				case "show":
					timer.show();
					break;
				case "reset":
					timer.reset();
					break;
				default:
					ShaniCore.errorOccurred("Invalid action name in TimerAction: "+name);
					return false;
			}
			return true;
		}
		@Override
		protected boolean executeNoTarget(){
			Tests.addResults("newTimer",true);
			
			String timerName=(String)parameters.get("name");
			
			Tests.addResults("operation",name);
			Tests.addResults("timerName",timerName);
			
			switch(name) {
				case "start":
					System.out.printf(nonExistStartMessage.toString(),timerName);
					System.out.println();
					
					Timer timer=new Timer(timerName);
					manager.registerNewTarget(timer);
					
					timer.start(false);
					break;
				case "stop":
					System.out.printf(nonExistStopMessage.toString(),timerName);
					System.out.println();
					break;
				case "show":
					System.out.printf(nonExistPrintTimeMessage.toString(),timerName);
					System.out.println();
					break;
				case "reset":
					System.out.printf(nonExistResetMessage.toString(),timerName);
					System.out.println();
					break;
				default:
					ShaniCore.errorOccurred("Invalid action name in TimerAction: "+name);
					return false;
			}
			return true;
		}
		
		@Override
		public boolean connectAction(String action){
			assert false:"Better Action connecting implementation soon, so this will stay unimplemented until then.";
			System.err.print("Action connecting not supported.");
			return false;
		}
	}
	private class Timer extends KeywordTarget{
		
		private int timeCounted;						//In sec
		private boolean isRunning;
		private long lastTimeMeasure;					//currentTimeMillis
		
		public Timer(String keyword){
			super("name",new ShaniString(ShaniString.ParseMode.raw,keyword));
		}
		private Timer(Element e) {
			super("name",e);
			
			timeCounted=Storage.getInt(saveLocation,"time");
		}
		
		private void start(boolean echo) {
			if(!isRunning) {
				isRunning=true;
				lastTimeMeasure =System.currentTimeMillis();
				if(echo)System.out.printf(startMessage.toString(),keyword.toString());
			}else if(echo)System.out.printf(alreadyRunningMessage.toString(),keyword.toString());
			if(echo)System.out.println();
		}
		private void stop() {
			if(isRunning) {
				updateTime();
				isRunning=false;
				System.out.printf(stopMessage.toString(),keyword.toString());
			} else System.out.printf(alreadyStoppedMessage.toString(),keyword.toString());
			System.out.println();
		}
		private void reset() {
			timeCounted=0;
			isRunning=false;
			System.out.printf(resetMessage.toString(),keyword.toString());
			System.out.println();
		}
		private void show() {
			updateTime();
			
			Tests.addResults("time",timeCounted);
			System.out.printf(printTimeMessage.toString(), TimeParser.parseTime(timeCounted));
			System.out.println();
		}
		
		private void updateTime() {
			if(!isRunning)return;
			long time=System.currentTimeMillis();
			timeCounted+=(time- lastTimeMeasure)/1000;
			lastTimeMeasure =time;
		}
		
		@Override
		public void save() {
			super.save();
			
			updateTime();
			var timeNode=saveLocation.getElementsByTagName("time").item(0);
			if(timeNode==null) {
				timeNode=saveLocation.getOwnerDocument().createElement("time");
				saveLocation.appendChild(timeNode);
			}
			timeNode.setTextContent(Integer.toString(timeCounted));
		}
	}
	
	@Override
	public void save(){
		super.save();
		
		manager.saveTargets();
	}
}