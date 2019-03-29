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
	public static final ShaniString cantConnectMessage=new ShaniString("Wybacz nie wiem co zrobiæ*Nie mogê po³¹czyæ tych akcji");
	
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
	
	private static final Pattern UriPattern=Pattern.compile("[\\w\\.]+://[\\w/\\\\\\?=& ]+");
	private static final Pattern StartDirPattern=Pattern.compile("^(\\w:[\\\\/][\\w\\\\/!@#\\$%^&\\(\\)';,-\\[\\]\\{\\} ]+)[\\\\/]([\\w\\\\/!@#$%^&\\(\\)';,-\\[\\]\\{\\} ]+\\.[\\w]+)$");				//group 1- Home dir, 2- fileName
	private static final Pattern PathPattern=Pattern.compile("\\w:\\\\[\\w\\d \\\\()']+");
	private boolean isUri(String com) {
		return UriPattern.matcher(Tools.removeNational(com)).matches();
	}
	private boolean isExecutable(String com) {
		return StartDirPattern.matcher(Tools.removeNational(com)).matches();
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
				Return=execute("cmd /c start \"\" /D \""+target[0]+"\" \""+target[1]+'"',0);
				break;
			case "dir":
				Return=execute("cmd /c explorer "+target[0],1);
				break;
			default:
				System.out.println(targetType+" is not supported execute type");
				Return=false;
			}
			if(Return)System.out.println(successfulMessage);
			return Return;
		}
		private boolean execute(String command, int succesfulExitVal) {
			try {
				Process proc=Runtime.getRuntime().exec(command);
				proc.waitFor();
				Engine.debug.println(command+": "+proc.exitValue());
				return proc.exitValue()==succesfulExitVal;
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
		
		private AddExecuteAction(ShaniMatcher matcher) {
			unmatched=matcher.getUnmatched().toString();
		}
		
		public boolean execute() {
			System.out.println(notKnowMessage);
			
			String newCom=Tools.clear(Engine.in.nextLine());
			if(Tools.isNegativeAnswer(newCom))
				return false;
			if(isPath(newCom)) {
				createAction(unmatched,"dir",new String[] {newCom});
				return true;
			}
			if(isUri(newCom)) {
				return createAction(unmatched,"start",new String[] {newCom});
			}
			if(isExecutable(newCom)) {
				if(newCom.endsWith(".bat")) {
					createAction(unmatched,"call",new String[] {newCom});
				} else {
					Matcher mat=StartDirPattern.matcher(newCom);
					mat.matches();
					mat.group();
					
					createAction(unmatched,"startdir",new String[] {mat.group(1),mat.group(2)});
				}
				return true;
			} else {
				KeywordAction exec=getAction(newCom);
				if(exec!=null) {
					exec.addKey(new ShaniString(unmatched));
					exec.execute();
					return true;
				}
				return false;
			}
		}
		private boolean createAction(String key, String targetType, String[] target) {
			KeywordAction action=new ExecuteAction(new ShaniString(key),targetType,target);
			action.execute();
			
			return true;
		}
		public boolean connectAction(String action) {
			System.err.println("Can't connect ExecutableOrder.AddExecuteAction action to another");
			return false;
		}
	}
}
/* regex data:
 * uri:
 * com.epicgames.launcher://apps/Jaguar?action=launch&silent=true
 * steam://rungameid/219740
 * executable:
 * C:\Program Files (x86)\StarCraft II\StarCraft II.exe
 * C:\Program Files (x86)\GOG Galaxy\Games\The Witcher 3 Wild Hunt GOTY\bin\x64\Witcher3.exe
 * C:\Users\Przemek\Documents\Gry\game launchers\Gaming Mode.bat
 * directory:
 * C:\Users\Przemek\Desktop\con
 */