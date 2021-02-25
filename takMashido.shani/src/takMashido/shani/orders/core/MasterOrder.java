package takMashido.shani.orders.core;

import org.w3c.dom.Element;
import takMashido.shani.Engine;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.core.text.ShaniString.ShaniMatcher;
import takMashido.shani.orders.Action;
import takMashido.shani.orders.Executable;
import takMashido.shani.orders.TextOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class MasterOrder extends TextOrder {
	private ShaniString notGoodTimeMessage;
	
	private enum ActionType{exit,save,autosaveTime};
	
	private int autoSaveTime=5;				//In minutes
	
	private ShaniString exitKey;
	private ShaniString saveKey;
	private ShaniString autoSaveTimeKey;
	
	private AutoSaver autoSaver;
	
	public MasterOrder(Element e) {
		super(e);
		
		autoSaver=new AutoSaver();
		autoSaver.setDaemon(true);
		autoSaver.start();
		
		exitKey=ShaniString.loadString(e,"exit");
		autoSaveTimeKey=ShaniString.loadString(e,"autosave");
		saveKey=ShaniString.loadString(e,"save");
		
		notGoodTimeMessage=ShaniString.loadString(e,"notGoodTimeMessage");
	}
	
	private void invokeSave() {
		Engine.debug.println("save");
		Engine.saveMainFile();
		Engine.flushBuffers();
	}
	@SuppressWarnings("resource")
	private void setAutosaveTime(String time) {
		Engine.debug.println("autosaveTimeChange");
		try {
			autoSaveTime=new Scanner(time).nextInt();
		} catch(NoSuchElementException ex) {
			System.out.println(notGoodTimeMessage);
		}
	}
	private void exit() {
		Engine.exit();
	}
	
	private class AutoSaver extends Thread{
		private AutoSaver() {
			setName("autoSaver");
		}
		public void run() {
			long lastSaved=Engine.lastExecutionTime;
			while(true) {
				try {
					Thread.sleep(30000);
					if(Engine.lastExecutionTime>lastSaved&&Engine.lastExecutionTime>System.currentTimeMillis()+autoSaveTime*60*1000) {
						Engine.saveMainFile();
						Engine.debug.flush();
						Engine.info.flush();
						System.gc();
					}
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	@Override
	public List<Executable> getExecutables(ShaniString command) {
		ArrayList<Executable> executables=new ArrayList<>();
		
		ShaniMatcher matcher;
		if((matcher=command.getMatcher()).apply(exitKey).isEqual()) {
			executables.add(new Executable(new MasterAction(ActionType.exit,matcher.getUnmatched().toString()),matcher.getCost()));
		} else if((matcher=command.getMatcher()).apply(saveKey).isEqual()) {
			executables.add(new Executable(new MasterAction(ActionType.save,matcher.getUnmatched().toString()),matcher.getCost()));
		} else if((matcher=command.getMatcher()).apply(autoSaveTimeKey).isEqual()) {
			executables.add(new Executable(new MasterAction(ActionType.autosaveTime,matcher.getUnmatched().toString()),matcher.getCost()));
		}
		
		return executables;
	}
	
	private class MasterAction extends Action{
		private ActionType actionType;
		private String addData;

		private MasterAction(ActionType actionType, String addData) {
			this.actionType=actionType;
			this.addData=addData;
		}
		
		public boolean execute() {
			switch(actionType) {
			case save:
				invokeSave();
				return true;
			case autosaveTime:
				setAutosaveTime(addData);
				return true;
			case exit:
				exit();
				return true;
			default:
				return false;
			}
		}

		public boolean connectAction(String action) {
			assert false:"Actions connecting for shani.orders.MasterOrder.MasterAction is not supported";
			System.err.println("Actions connecting for shani.orders.MasterOrder.MasterAction is not supported");
			return false;
		}
	}
}