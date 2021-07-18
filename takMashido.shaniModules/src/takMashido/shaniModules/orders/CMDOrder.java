package takMashido.shaniModules.orders;

import org.w3c.dom.Element;
import takMashido.shani.core.Config;
import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.Tests;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.orders.Action;
import takMashido.shani.orders.IntendParserOrder;

import java.io.IOException;
import java.util.ArrayList;

public class CMDOrder extends IntendParserOrder{
	private ShaniString executeMessage;
	private ShaniString runCMDSessionMessage;
	private ShaniString endCMDSessionMessage;
	
	public CMDOrder(Element e) {
		super(e);
		
		executeMessage=ShaniString.loadString(e,"executeMessage");
		runCMDSessionMessage=ShaniString.loadString(e,"runCMDSessionMessage");
		endCMDSessionMessage=ShaniString.loadString(e,"endCMDSessionMessage");
	}
	
	@Override
	public Action getAction(){
		return new CMDAction();
	}
	
	private void processCommand(String command) {
		if(command.isEmpty()) {
			launchCMD();
		} else {
			executeCommand(command);
		}
	}
	private void executeCommand(String command) {
		if(Config.testMode){
			Tests.addResults("operation",command);
			return;
		}
		
		executeMessage.printOut();
		try {
			ArrayList<String> com=new ArrayList<>();
			com.add("cmd");
			com.add("/c");
			com.add(command);
			
			ProcessBuilder builder=new ProcessBuilder(com);
			builder.inheritIO();
			Process proc=builder.start();
			
			proc.waitFor();
			ShaniCore.debug.println("cmd /c "+command+": "+proc.exitValue());
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (InterruptedException e) {
			return;
		}
	}
	private void launchCMD() {
		if(Config.testMode){
			Tests.addResults("openCMD",true);
			return;
		}
		
		runCMDSessionMessage.printOut();
		try {
			ProcessBuilder builder=new ProcessBuilder("cmd");
			builder.inheritIO();
			Process proc=builder.start();
			
			proc.waitFor();
			endCMDSessionMessage.printOut();
		} catch (IOException e) {							//Shouldn't occur
			assert false:"Failed to launch cmd. It's probably missing.";
			System.err.println("Failed to launch cmd.  It's probably missing.");
			System.out.println("Failed to launch cmd. Shani can't find it");
			e.printStackTrace();
		} catch (InterruptedException e) {}
	}
	
	private class CMDAction extends Action {
		@Override
		public boolean execute(){
			ShaniString unmatched=(ShaniString)parameters.get("unmatched");
			
			if(unmatched.isEmpty())
				launchCMD();
			else
				executeCommand(unmatched.toString());
			
			return true;
		}
	}
}