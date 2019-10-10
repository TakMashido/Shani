package shani.orders;

import java.io.IOException;
import java.util.ArrayList;

import shani.Engine;
import shani.ShaniString;
import shani.orders.templates.KeywordOrder;

public class CMDOrder extends KeywordOrder{
	private static final ShaniString executeMessage=ShaniString.loadString("orders.CMDOrder.executeMessage");
	private static final ShaniString runCMDSessionMessage=ShaniString.loadString("orders.CMDOrder.runCMDSessionMessage");
	private static final ShaniString endCMDSessionMessage=ShaniString.loadString("orders.CMDOrder.endCMDSessionMessage");
	
	public UnmatchedAction getUnmatchedAction() {
		return new CMDAction();
	}
	
	private class CMDAction extends UnmatchedAction{
		@Override
		public boolean execute() {
			if(unmatched.isEmpty()) {
				launchCMD();
			} else {
				executeCommand(unmatched.toFullString());
			}
			return true;
		}
		private void executeCommand(String command) {
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
				Engine.debug.println(command+": "+proc.exitValue());
				return;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} catch (InterruptedException e) {
				return;
			}
		}
		private void launchCMD() {
			runCMDSessionMessage.printOut();
			try {
				ProcessBuilder builder=new ProcessBuilder("cmd");
				builder.inheritIO();
				Process proc=builder.start();
				
				proc.waitFor();
				endCMDSessionMessage.printOut();
			} catch (IOException e) {							//Shoudn't occur
				assert false:"Failed to launch cmd. It's propably missing.";
				System.err.println("Failed to launch cmd.  It's propably missing.");
				System.out.println("Failed to launch cmd. Shani can't find it");
				e.printStackTrace();
			} catch (InterruptedException e) {}
		}
	}
}