package shani.orders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import shani.Config;
import shani.Engine;
import shani.ShaniString;
import shani.ShaniString.ShaniMatcher;
import shani.Tools;
import shani.orders.templates.Action;
import shani.orders.templates.Executable;
import shani.orders.templates.KeywordOrder.KeywordAction;
import shani.orders.templates.KeywordOrderNG;
import shani.orders.templates.KeywordOrderNG.KeywordActionNG;

public class ExecuteOrder extends KeywordOrderNG {
	private static ShaniString successfulMessage;
	private static ShaniString notKnowMessage;
	private static ShaniString unrecognizedMessage;
	
	@Override
	protected boolean initialize(Element e) {
		successfulMessage=ShaniString.loadString(e, "successfulMessage");
		notKnowMessage=ShaniString.loadString(e, "notKnowMessage");
		unrecognizedMessage=ShaniString.loadString(e, "unrecognizedMessage");
		
		return true;
	}
	
	@Override
	protected String getDataLocation() {
		return "fileSystem.executables";
	}
	
	@Override
	public KeywordActionNG actionFactory(Element element) {
		return new ExecuteAction(element);
	}
	@Override
	public UnmatchedActionNG getUnmatchedAction() {
		return new AddExecuteAction();
	}
	
	private static final Pattern UriPattern=Pattern.compile("\"?[\\w\\.]+://[\\w/\\\\\\?=& ]+\"?");
	private static final Pattern StartDirPattern =Pattern.compile("^\"?(\\w:[\\\\/][\\w\\\\/!@#\\$%^&\\(\\)';,-\\[\\]\\{\\} ]+)[\\\\/]([\\w\\\\/!@#$%^&\\(\\)';,-\\[\\]\\{\\} ]+\\.[\\w]+)\"?$");				//group 1- Home dir, 2- fileName
	private static final Pattern StartDirPattern2=Pattern.compile("^\"(\\w:[\\\\/][\\w\\\\/!@#\\$%^&\\(\\)';,-\\[\\]\\{\\} ]+)[\\\\/]([\\w\\\\/!@#$%^&\\(\\)';,-\\[\\]\\{\\} ]+\\.[\\w]+)\" ?(.*)$");			//SAME							, 3- command line arguments
	private static final Pattern PathPattern=Pattern.compile("\"?\\w:\\\\[\\w\\d \\\\()']+\"?");
	protected boolean isUri(String com) {
		return UriPattern.matcher(Tools.removeNational(com)).matches();
	}
	protected boolean isExecutable(String com) {
		String path=Tools.removeNational(com);
		return StartDirPattern.matcher(path).matches()||StartDirPattern2.matcher(path).matches();
	}
	protected boolean isPath(String com) {
		return PathPattern.matcher(Tools.removeNational(com)).matches();
	}
 	
	protected class ExecuteAction extends KeywordActionNG{
		private String targetType;
		private String[] target;
		
		protected ExecuteAction(Element e) {
			super(e);
			actionFile=e;
			targetType=e.getElementsByTagName("type").item(0).getTextContent();
			target=new ShaniString(e.getElementsByTagName("todo").item(0).getTextContent()).getArray();
		}
		protected ExecuteAction(ShaniString key,String targetType,String[] target) {
			super(key);
			
			var doc=targetDataNode.getOwnerDocument();
			Element e2=doc.createElement("type");
			e2.appendChild(doc.createTextNode(targetType));
			actionFile.appendChild(e2);
			this.targetType=targetType;
			
			e2=doc.createElement("todo");
			e2.appendChild(doc.createTextNode(new ShaniString(target).toFullString()));
			actionFile.appendChild(e2);
			this.target=target;
		}
		
		@Override
		public boolean keywordExecute() {
			boolean Return=false;
			switch(targetType) {
			case "print":
				System.out.println(target[0]);
				Return=true;
				break;
			case "call":
				Return=execute("cmd /c call \""+target[0]+'"',0);
				break;
			case "start":
				Return=execute("cmd /c start \"\" \""+target[0]+'"',0);
				break;
			case "startdir":
				if(target.length==2) {
					Return=execute("cmd /c start \"\" /D \""+target[0]+"\" \""+target[1]+'"',0);
				} else if(target.length==3) {
					Return=execute("cmd /c start \"\" /D \""+target[0]+"\" \""+target[1]+"\" "+target[2],0);
				} else {
					Return=false;
					System.out.println("Failed to execute: You've just found shani bug so congrats and report on github.com/takmashido/shani.");
					assert false:"Execute order suports executing only startdir targets with length 2 or 3. Length "+target.length+" sneaked somehow. FIXIT!!!!!!!!!";
					System.err.println("Execute order suports executing only startdir targets with length 2 or 3. Length "+target.length+" sneaked somehow. FIXIT!!!!!!!!!");
				}
				break;
			case "dir":
				Return=execute("cmd /c explorer "+target[0],1);
				break;
			default:
				System.err.println(targetType+" is not supported execute type");
				System.out.println(targetType+" is not supported execute type");
				Return=false;
			}
			if(Return)System.out.println(successfulMessage);
			return Return;
		}
		private boolean execute(String command, int succesfullExitVal) {
			try {
				Process proc=Runtime.getRuntime().exec(command);
				proc.waitFor();
				Engine.debug.println(command+": "+proc.exitValue());
				return proc.exitValue()==succesfullExitVal;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} catch (InterruptedException e) {
				return true;
			}
		}
	}
	protected class AddExecuteAction extends UnmatchedActionNG{
		private KeywordActionNG readyAction;
		
		@Override
		public boolean execute() {
			System.out.println(notKnowMessage);
			
			String newCom=Engine.in.nextLine().trim();
			Boolean positive=Engine.isInputPositive(new ShaniString(newCom,false));
			if(positive!=null&&!positive)
				return false;
			if(isPath(newCom)) {
				return createAction(unmatched,"dir",new String[] {Tools.clear(newCom)});
			}
			if(isUri(newCom)) {
				return createAction(unmatched,"start",new String[] {Tools.clear(newCom)});
			}
			if(isExecutable(newCom)) {
				Matcher mat=StartDirPattern.matcher(Tools.clear(newCom));
				if(!mat.matches()) {
					mat=StartDirPattern2.matcher(newCom);
					mat.matches();
					return createAction(unmatched,"startdir",new String[] {mat.group(1),mat.group(2),mat.group(3)});
				}
				mat.group();
				
				return createAction(unmatched,"startdir",new String[] {mat.group(1),mat.group(2)});
			}
			
			KeywordActionNG exec=getAction(new ShaniString(newCom,false));				//Input is not valid program/file/URL. Check if it's one of already existing keys.
			if(exec!=null) {
				exec.addKey(unmatched);
				exec.execute();
				readyAction=exec;
				return true;
			}
			unrecognizedMessage.printOut();
			return false;
		}
		private boolean createAction(ShaniString key, String targetType, String[] target) {
			KeywordActionNG action=new ExecuteAction(key,targetType,target);
			action.execute();
			readyAction=action;
			return true;
		}
		@Override
		public boolean connectAction(String action) {
			if(readyAction!=null) {
				return readyAction.connectAction(action);
			}
			System.err.println("Can't connect ExecuteOrder.AddExecuteAction action to another if it wasn't created KeywordAction");
			return false;
		}
	}
}
/* regex data:
 * uri:
 * com.epicgames.launcher://apps/Jaguar?action=launch&silent=true
 * steam://rungameid/219740
 * 
 * executable:
 * C:\Program Files (x86)\StarCraft II\StarCraft II.exe
 * C:\Program Files (x86)\GOG Galaxy\Games\The Witcher 3 Wild Hunt GOTY\bin\x64\Witcher3.exe
 * C:\Users\TakMashido\Documents\Gry\game launchers\Gaming Mode.bat
 * 
 * executableWithArgs:
 * "C:\Program Files (x86)\GOG Galaxy\GalaxyClient.exe" /command=runGame /gameId=1238653230 /path="C:\Program Files (x86)\GOG Galaxy\Games\Factorio"
 * 
 * directory:
 * C:\Users\Przemek\Desktop\con
 */