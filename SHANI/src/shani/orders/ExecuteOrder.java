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
import shani.orders.templates.KeywordOrder;

public class ExecuteOrder extends KeywordOrder {
	private static final ShaniString successfulMessage=ShaniString.loadString("orders.ExecuteOrder.successfulMessage");
	private static final ShaniString notKnowMessage=ShaniString.loadString("orders.ExecuteOrder.notKnowMessage");
	private static final ShaniString unrecognizedMessage=ShaniString.loadString("orders.ExecuteOrder.unrecognizedMessage");
	public static final ShaniString cantConnectMessage=ShaniString.loadString("orders.ExecuteOrder.cantConnectMessage");
	
	public KeywordAction actionFactory(Element element) {
		return new ExecuteAction(element);
	}
	
	public List<Executable> createExecutables(ShaniString command, ShaniMatcher matcher){
		if(matcher.getMatchedCost()>Config.sentenseCompareTreshold||matcher.getMatchedNumber()==0)return null;
		ArrayList<Executable> Return=new ArrayList<Executable>();
		Return.add(new Executable(new AddExecuteAction(matcher),(short)(Config.sentenseCompareTreshold-1)));
		return Return;
	}
	
	private ExecuteAction getAction(String com) {
		ShaniString com2=new ShaniString(com);
		int index=-1;
		for(int i=0;i<actions.size();i++) {
			KeywordAction action=actions.get(i);
			if(action.isEqual(com2)) {
				index=i;
				break;
			}
		}
		return index!=-1?(ExecuteAction)actions.get(index):null;
	}
	
	private static final Pattern UriPattern=Pattern.compile("\"?[\\w\\.]+://[\\w/\\\\\\?=& ]+\"?");
	private static final Pattern StartDirPattern =Pattern.compile("^\"?(\\w:[\\\\/][\\w\\\\/!@#\\$%^&\\(\\)';,-\\[\\]\\{\\} ]+)[\\\\/]([\\w\\\\/!@#$%^&\\(\\)';,-\\[\\]\\{\\} ]+\\.[\\w]+)\"?$");				//group 1- Home dir, 2- fileName
	private static final Pattern StartDirPattern2=Pattern.compile("^\"(\\w:[\\\\/][\\w\\\\/!@#\\$%^&\\(\\)';,-\\[\\]\\{\\} ]+)[\\\\/]([\\w\\\\/!@#$%^&\\(\\)';,-\\[\\]\\{\\} ]+\\.[\\w]+)\" ?(.*)$");			//SAME							, 3- command line arguments
	private static final Pattern PathPattern=Pattern.compile("\"?\\w:\\\\[\\w\\d \\\\()']+\"?");
	private boolean isUri(String com) {
		return UriPattern.matcher(Tools.removeNational(com)).matches();
	}
	private boolean isExecutable(String com) {
		String path=Tools.removeNational(com);
		return StartDirPattern.matcher(path).matches()||StartDirPattern2.matcher(path).matches();
	}
	private boolean isPath(String com) {
		return PathPattern.matcher(Tools.removeNational(com)).matches();
	}
	
	private class ExecuteAction extends KeywordAction{
		private String targetType;
		private String[] target;
		
		private ExecuteAction(Element e) {
			super(e);
			actionFile=e;
			targetType=e.getElementsByTagName("type").item(0).getTextContent();
			target=new ShaniString(e.getElementsByTagName("todo").item(0).getTextContent()).getArray();
		}
		private ExecuteAction(ShaniString key,String targetType,String[] target) {
			super(key);
			
			Element e2=Engine.doc.createElement("type");
			e2.appendChild(Engine.doc.createTextNode(targetType));
			actionFile.appendChild(e2);
			this.targetType=targetType;
			
			e2=Engine.doc.createElement("todo");
			e2.appendChild(Engine.doc.createTextNode(new ShaniString(target).toFullString()));
			actionFile.appendChild(e2);
			this.target=target;
		}
		
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
					System.out.println("Failed to execute: shani bug");
					assert false:"Execute order suports executing only of targets with length 2 or 3. Length "+target.length+" sneaked somehow. FIXIT!!!!!!!!!";
					System.err.println("Execute order suports executing only of targets with length 2 or 3. Length "+target.length+" sneaked somehow. FIXIT!!!!!!!!!");
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
	private class AddExecuteAction extends Action{
		private String unmatched;
		private KeywordAction readyAction;
		
		private AddExecuteAction(ShaniMatcher matcher) {
			unmatched=matcher.getUnmatched().toString();
		}
		
		public boolean execute() {
			System.out.println(notKnowMessage);
			
//			String newCom=Tools.clear(Engine.in.nextLine());
			String newCom=Engine.in.nextLine().trim();
			Boolean positive=Engine.isInputPositive(new ShaniString(newCom,false));
			if(positive!=null&&positive==false)
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
			} else {
				KeywordAction exec=getAction(newCom);
				if(exec!=null) {
					exec.addKey(new ShaniString(unmatched));
					exec.execute();
					readyAction=exec;
					return true;
				}
				unrecognizedMessage.printOut();
				return false;
			}
		}
		private boolean createAction(String key, String targetType, String[] target) {
			KeywordAction action=new ExecuteAction(new ShaniString(key),targetType,target);
			action.execute();
			readyAction=action;
			return true;
		}
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