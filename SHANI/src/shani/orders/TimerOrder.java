package shani.orders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shani.Config;
import shani.Engine;
import shani.ShaniString;
import shani.Storage;
import shani.orders.templates.SentenceMatcherOrder;
import shani.tools.Parsers;

public class TimerOrder extends SentenceMatcherOrder{
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
	protected List<SentenceMatcherAction> actionFactoryList(String sentenceName, HashMap<String, String> returnValues) {
		ArrayList<SentenceMatcherAction> ret=new ArrayList<>(timers.size());
		
		ShaniString keyword=new ShaniString(returnValues.get("name"));
		
		for(var timer:timers) {
			timer.cost=timer.keyword.getCompareCost(keyword);
			
			if(timer.cost<Config.wordCompareTreshold)
				ret.add(timer);
		}
		
		if(ret.isEmpty())
			ret.add(new NoTimerAction());
		
		return ret;
	}
	
	@Override
	public void save() {
		for(var timer:timers) {
			timer.save();
		}
	}
	
	private class TimerAction extends SentenceMatcherAction {
		protected Element targetFile;
		protected ShaniString keyword;
		
		private int timeCounted;						//In sec
		private boolean isRunning;
		private long lastTimeMeansure;					//currentTimeMillis					
		
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
		
		@Override
		protected boolean execute(String sentenceName, HashMap<String, String> returnValues) {
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
				Engine.errorMessage.printOut();
				System.err.println();
				return false;
			}
			return true;
		}
		
		@SuppressWarnings("unused")						//Used using java reflecion API
		private void start() {
			start(true);
		}
		private void start(boolean echo) {
			if(!isRunning) {
				isRunning=true;
				lastTimeMeansure=System.currentTimeMillis();
				if(echo)System.out.printf(startMessage.toString(),keyword.toString());
			}else if(echo)System.out.printf(alreadyRunningMessage.toString(),keyword.toString());
			if(echo)System.out.println();
		}
		@SuppressWarnings("unused")						//Used using java reflecion API
		private void stop() {
			if(isRunning) {
				updateTime();
				isRunning=false;
				System.out.printf(stopMessage.toString(),keyword.toString());
			} else System.out.printf(alreadyStoppedMessage.toString(),keyword.toString());
			System.out.println();
		}
		@SuppressWarnings("unused")						//Used using java reflecion API
		private void reset() {
			timeCounted=0;
			isRunning=false;
			System.out.printf(resetMessage.toString(),keyword.toString());
			System.out.println();
		}
		@SuppressWarnings("unused")						//Used using java reflecion API
		private void show() {
			updateTime();
			System.out.printf(printTimeMessage.toString(), Parsers.parseTime(timeCounted));
			System.out.println();
		}
		
		private void updateTime() {
			if(!isRunning)return;
			long time=System.currentTimeMillis();
			timeCounted+=(time-lastTimeMeansure)/1000;
			lastTimeMeansure=time;
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
	private class NoTimerAction extends SentenceMatcherAction{
		
		private NoTimerAction() {
			importanceBias=noTimerImportanceBias;
		}
		
		@Override
		protected boolean execute(String sentenceName, HashMap<String, String> returnValues) {
			String timerName=returnValues.get("name");
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
				System.err.println("Unknow sentence name in NoTimerAction: "+sentenceName);
				Engine.errorMessage.printOut();
				return false;
			}
			return true;
		}
	}
}