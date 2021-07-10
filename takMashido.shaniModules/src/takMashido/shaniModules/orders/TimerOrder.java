package takMashido.shaniModules.orders;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import takMashido.shani.core.Config;
import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.Storage;
import takMashido.shani.core.Tests;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.orders.Action;
import takMashido.shani.orders.IntendParserOrder;
import takMashido.shani.tools.parsers.TimeParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
	
	private short noTimerImportanceBias;
	
	private Node dataElement;			//node inside ordersData in shaniData
	
	private ArrayList<TimerAction> timers=new ArrayList<>();
	
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
		
		noTimerImportanceBias=(short)Storage.getInt(e, "config.noTimerImportanceBias");
		
		NodeList subnodes=dataElement.getChildNodes();
		for(int i=0;i<subnodes.getLength();i++) {
			if(subnodes.item(i) instanceof Element)
				new TimerAction((Element)subnodes.item(i));
		}
	}
	
	@Override
	public Action getAction(){
		return new SelectTimerAction();
	}
	
	@Override
	public void save() {
		for(var timer:timers) {
			timer.save();
		}
	}
	
	private class SelectTimerAction extends Action{
		private TimerAction bestTimer;
		private short bestTimerCost;
		
		@Override
		public boolean execute(){
			if(bestTimer!=null)
				return bestTimer.execute(name, (HashMap<String,String>)parameters);
			
			return new NoTimerAction().execute(name, (HashMap<String,String>)parameters);
		}
		
		@Override
		public void init(String name, Map<String,? extends Object> params){
			super.init(name,params);
			
			//Set best timer for this Action.
			ShaniString keyword=new ShaniString((String)parameters.get("name"));
			
			if(timers.size()>0){
				short minCost=timers.get(0).keyword.getCompareCost(keyword);
				int minIndex=0;
				for(int i=0; i<timers.size(); i++){
					short cost=timers.get(i).keyword.getCompareCost(keyword);
					
					if(cost<minCost){
						minCost=cost;
						minIndex=i;
					}
				}
				
				bestTimer=timers.get(minIndex);
				bestTimerCost=minCost;
			}
		}
		
		@Override
		public short getImportanceBias(){
			if(bestTimer!=null)
				return bestTimerCost;
			
			return Config.wordInsertionCost;
		}
		@Override
		public short getCost(){
			if(bestTimer!=null)
				return 0;
			
			return noTimerImportanceBias;
		}
		
		@Override
		public boolean connectAction(String action){
			assert false:"Better Action connecting implementation soon, so this will stay unimplemented until then.";
			System.err.print("Action connecting not supported.");
			return false;
		}
	}
	private class TimerAction{
		protected Element targetFile;
		protected ShaniString keyword;
		
		private int timeCounted;						//In sec
		private boolean isRunning;
		private long lastTimeMeasure;					//currentTimeMillis
		
		private TimerAction(Element e) {
			targetFile=e;
			keyword=new ShaniString(targetFile.getElementsByTagName("key").item(0));
			
			timeCounted=Integer.parseInt(targetFile.getElementsByTagName("time").item(0).getTextContent());
			
			timers.add(this);
		}
		private TimerAction(String keyword) {
			this.keyword=new ShaniString(keyword);
			targetFile=dataElement.getOwnerDocument().createElement("target");
			dataElement.appendChild(targetFile);
			
			Element e=targetFile.getOwnerDocument().createElement("key");
			targetFile.appendChild(e);
			timers.add(this);
			this.keyword.setNode(e);
		}
		
		protected boolean execute(String sentenceName, HashMap<String, String> returnValues) {
			Tests.addResults("operation",sentenceName);
			
			switch (sentenceName) {
			case "start":
				start(true);
				break;
			case "stop":
				stop();
				break;
			case "show":
				show();
				break;
			case "reset":
				reset();
				break;
			default:
				ShaniCore.errorMessage.printOut();
				System.err.println();
				return false;
			}
			return true;
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
			Tests.addResults("time",timeCounted);
			
			updateTime();
			System.out.printf(printTimeMessage.toString(), TimeParser.parseTime(timeCounted));
			System.out.println();
		}
		
		private void updateTime() {
			if(!isRunning)return;
			long time=System.currentTimeMillis();
			timeCounted+=(time- lastTimeMeasure)/1000;
			lastTimeMeasure =time;
		}
		private void save() {
			updateTime();
			var timeNode=targetFile.getElementsByTagName("time").item(0);
			if(timeNode==null) {
				timeNode=targetFile.getOwnerDocument().createElement("time");
				targetFile.appendChild(timeNode);
			}
			timeNode.setTextContent(Integer.toString(timeCounted));
		}
	}
	private class NoTimerAction{
		protected boolean execute(String sentenceName, HashMap<String, String> returnValues) {
			String timerName=returnValues.get("name");
			
			Tests.addResults("operation",sentenceName);
			Tests.addResults("timerName",timerName);
			
			switch(sentenceName) {
			case "start":
				System.out.printf(nonExistStartMessage.toString(),timerName);
				System.out.println();
				TimerAction timer=new TimerAction(timerName);
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
				System.err.println("Unknown sentence name in NoTimerAction: "+sentenceName);
				ShaniCore.errorMessage.printOut();
				return false;
			}
			return true;
		}
	}
}