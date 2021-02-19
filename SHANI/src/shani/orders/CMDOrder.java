package shani.orders;

import java.io.IOException;
import java.util.ArrayList;

import org.w3c.dom.Element;

import shani.Engine;
import shani.ShaniString;
import shani.orders.templates.KeywordOrderNG;
import shani.orders.templates.KeywordOrderNG.KeywordActionNG;
import shani.orders.templates.KeywordOrderNG.UnmatchedActionNG;

public class CMDOrder extends KeywordOrderNG{
	private ShaniString executeMessage;
	private ShaniString runCMDSessionMessage;
	private ShaniString endCMDSessionMessage;
	
	@Override
	public boolean initialize(Element e) {
		executeMessage=ShaniString.loadString(e,"executeMessage");
		runCMDSessionMessage=ShaniString.loadString(e,"runCMDSessionMessage");
		endCMDSessionMessage=ShaniString.loadString(e,"endCMDSessionMessage");
		
		targetExactMatch=true;
		return true;
	}
	
	@Override
	public KeywordActionNG actionFactory(Element element) {
		return new MergedCMDAction(element);
	}
	@Override
	public UnmatchedActionNG getUnmatchedAction() {
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
			Engine.debug.println("cmd /c "+command+": "+proc.exitValue());
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
	
	private class MergedCMDAction extends KeywordActionNG{
		private final String command;
		
		protected MergedCMDAction(Element elem) {
			super(elem);
			command=elem.getElementsByTagName("command").item(0).getTextContent();
		}
		protected MergedCMDAction(ShaniString command) {
			super(command);									//Delete fuzzy string matching from this somehow
			this.command=command.toFullString();
			
			Element elem=Engine.doc.createElement("command");
			elem.setTextContent(this.command);
			actionFile.appendChild(elem);
		}
		
		@Override
		public boolean keywordExecute() {
			processCommand(command);
			return true;
		}
	}
	private class CMDAction extends UnmatchedActionNG{
		private MergedCMDAction mergedAction=null;  
		
		@Override
		public boolean execute() {
			processCommand(unmatched.toFullString());
			return true;
		}
		public boolean connectAction(String action) {
			if(mergedAction==null) {
				mergedAction=new MergedCMDAction(unmatched);
			}
			return mergedAction.connectAction(action);
		}
	}
	
	@Override
	protected String getDataLocation() {
		return null;
	}
}