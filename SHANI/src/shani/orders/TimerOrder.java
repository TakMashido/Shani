package shani.orders;

import org.w3c.dom.Element;

import shani.ShaniString;
import shani.orders.templates.MultipleKeywordOrder;

public class TimerOrder extends MultipleKeywordOrder{
//	private ShaniString startMessage=new ShaniString("Timer %s wystartowa³.*Zegar w³¹czony.*Timer %s dzia³a.*W³¹czy³am timer %s.");
//	private ShaniString alreadyRunningMessage=new ShaniString("Timer %s ju¿ dzia³a*Zegar %s ju¿ chodzi.*Zegar %s ca³y czas dzia³a.*Nie mogê w³¹czyæ timera %s. Ju¿ dzia³a.");
//	private ShaniString stopMessage=new ShaniString("Zatrzyma³am timer %s.*Timer %s zosta³ zatrzymany");
//	private ShaniString alreadyStoppedMessage=new ShaniString("Timer %s ju¿ stoi.");
//	private ShaniString resetMessage=new ShaniString("Resetujê timer %s.*Ju¿ zerujê timer %s.Zegar %s ustawiony na 0.");
//	private ShaniString printTimeMessage=new ShaniString("Aktualny czas to %s.*Zliczono %s.*Timer pracowa³ przez %s.");
//	
//	private ShaniString nonExistStartMessage=new ShaniString("Tworzê timer %s.*Nowy timer %s wystartowa³.*Zegar w³¹czony.*Timer %s dzia³a.*W³¹czy³am timer %s.");
//	private ShaniString nonExistStopMessage=new ShaniString("Nie mogê zatrzymaæ timera %s. Nie istnieje.*Timer %s nie istnieje.*Brak timera %s");
//	private ShaniString nonExistResetMessage=new ShaniString("Timer %s nie intnieje.*Nie mogê zresetowaæ timera %s. Takowy nie istnieje.");
//	private ShaniString nonExistPrintTimeMessage=new ShaniString("Brak timera %s.*Jak mam wyœwietliæ czas z nieistniej¹cego timera?");
	
	private ShaniString startMessage=ShaniString.loadString("orders.TimerOrder.startMessage");                                          
	private ShaniString alreadyRunningMessage=ShaniString.loadString("orders.TimerOrder.alreadyRunningMessage");
	private ShaniString stopMessage=ShaniString.loadString("orders.TimerOrder.stopMessage");                                                                     
	private ShaniString alreadyStoppedMessage=ShaniString.loadString("orders.TimerOrder.alreadyStoppedMessage");                                                                                         
	private ShaniString resetMessage=ShaniString.loadString("orders.TimerOrder.resetMessage");                                                     
	private ShaniString printTimeMessage=ShaniString.loadString("orders.TimerOrder.printTimeMessage");                                                      
	                                                                                                                                                                         
	private ShaniString nonExistStartMessage=ShaniString.loadString("orders.TimerOrder.nonExistStartMessage");            
	private ShaniString nonExistStopMessage=ShaniString.loadString("orders.TimerOrder.nonExistStopMessage");                            
	private ShaniString nonExistResetMessage=ShaniString.loadString("orders.TimerOrder.nonExistResetMessage");                                  
	private ShaniString nonExistPrintTimeMessage=ShaniString.loadString("orders.TimerOrder.nonExistPrintTimeMessage");                                       
	
	@Override
	protected OrderTarget targetFactory(Element e) {
		return new TimerTarget(e);
	}
	
	@Override
	protected NotMatchedTarget getUnmatchedTarget() {
		return new NoTimerTarget();
	}
	
	@Override
	public void save() {
		for(var timer:orderTargets) {
			if(timer instanceof TimerTarget)((TimerTarget)timer).save();
		}
	}
	
	private class TimerTarget extends OrderTarget {
		private int timeCounted;						//In sec
		private boolean isRunning;
		private long lastTimeMeansure;					//currentTimeMillis					
		
		private TimerTarget(Element e) {
			super(e);
			timeCounted=Integer.parseInt(targetFile.getElementsByTagName("time").item(0).getTextContent());
		}
		private TimerTarget(ShaniString keyword) {
			super(keyword);
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
			System.out.printf(printTimeMessage.toString(), getTimeString());
			System.out.println();
		}
		
		private final String getTimeString() {
			String Return="";
			int time=timeCounted;
			if(time>60) {					//min
				if(time>3600) {				//h
					int h=time/3600;
					if(h==1)Return+=" godzina ";
					else Return+=h+" godzin ";
					time%=3600;
				}
				int m=time/60;
				if(m==1)Return+=" minuta ";
				else Return+=m+" minut ";
				time%=60;
			}
			Return+=time+" sekund";
			Return=Return.trim();
			return Return;
		}
		
		private void updateTime() {
			if(!isRunning)return;
			long time=System.currentTimeMillis();
			timeCounted+=(time-lastTimeMeansure)/1000;
			lastTimeMeansure=time;
		}
		private void save() {
			updateTime();
			targetFile.getElementsByTagName("time").item(0).setTextContent(Integer.toString(timeCounted));
		}
	}
	private class NoTimerTarget extends NotMatchedTarget{
		@SuppressWarnings("unused")						//Used using java reflecion API
		private void start() {
			System.out.printf(nonExistStartMessage.toString(),unmatched.toString());
			System.out.println();
			TimerTarget timer=new TimerTarget(unmatched);
			timer.start(false);
		}
		@SuppressWarnings("unused")						//Used using java reflecion API
		private void stop() {
			System.out.printf(nonExistStopMessage.toString(),unmatched.toString());
			System.out.println();
		}
		@SuppressWarnings("unused")						//Used using java reflecion API
		private void reset() {
			System.out.printf(nonExistResetMessage.toString(),unmatched.toString());
			System.out.println();
		}
		@SuppressWarnings("unused")						//Used using java reflecion API
		private void show() {
			System.out.printf(nonExistPrintTimeMessage.toString(),unmatched.toString());
			System.out.println();
		}
	}
}