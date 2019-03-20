package shani.orders;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import shani.Engine;
import shani.ShaniString;
import shani.ShaniString.ShaniMatcher;
import shani.orders.templates.Action;
import shani.orders.templates.Executable;
import shani.orders.templates.Order;

public class MasterOrder extends Order {
	private static final ShaniString notGoodTimeMessage=ShaniString.loadString("orders.MasterOrder.notGoodTimeMessage");
	private static final ShaniString closeMessage=ShaniString.loadString("orders.MasterOrder.closeMessage");
	
	private enum ActionType{exit,save,autosaveTime};
	
	private int autoSaveTime=10;			//In minutes
	
	private ShaniString exitKey;
	private ShaniString saveKey;
	private ShaniString autoSaveTimeKey;
	
	private AutoSaver autoSaver;
	
	protected boolean init() {
		autoSaver=new AutoSaver();
		autoSaver.setDaemon(true);
		autoSaver.start();
		
		exitKey=new ShaniString(orderFile.getElementsByTagName("exit").item(0));
		autoSaveTimeKey=new ShaniString(orderFile.getElementsByTagName("autosave").item(0));
		saveKey=new ShaniString(orderFile.getElementsByTagName("save").item(0));
		
		return true;
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
		Engine.debug.println("exit\n");
		System.out.println(closeMessage);
		try {
			Thread.sleep(700);
		} catch (InterruptedException e) {}
		System.exit(0);
	}
	
	private class AutoSaver extends Thread{
		private int lastSave=0;
		private AutoSaver() {
			setName("autoSaver");
		}
		public void run() {
			while(true) {
				try {
					Thread.sleep(60000);
					if(++lastSave>=autoSaveTime) {
						lastSave=0;
						Engine.saveMainFile();
						Engine.debug.flush();
						Engine.info.flush();
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