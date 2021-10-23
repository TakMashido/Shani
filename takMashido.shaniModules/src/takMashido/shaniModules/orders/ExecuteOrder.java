package takMashido.shaniModules.orders;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.Storage;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.libraries.Pair;
import takMashido.shani.orders.Action;
import takMashido.shani.orders.IntendParserOrder;
import takMashido.shani.orders.targetAction.KeywordTarget;
import takMashido.shani.orders.targetAction.Target;
import takMashido.shani.orders.targetAction.TargetAction;
import takMashido.shani.orders.targetAction.TargetActionManager;
import takMashido.shani.tools.InputCleaners;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExecuteOrder extends IntendParserOrder{
	private static ShaniString successfulMessage;
	private static ShaniString notKnowMessage;
	private static ShaniString unrecognizedMessage;
	
	private TargetActionManager manager;
	
	public ExecuteOrder(Element e) {
		super(e);
		
		successfulMessage=ShaniString.loadString(e, "successfulMessage");
		notKnowMessage=ShaniString.loadString(e, "notKnowMessage");
		unrecognizedMessage=ShaniString.loadString(e, "unrecognizedMessage");
		
		Node dataNode=Storage.getOrderData(this);
		
		Element executableElement=(Element)Storage.getNode(dataNode,"executables");
		if(executableElement==null){
			executableElement=dataNode.getOwnerDocument().createElement("executables");
			dataNode.appendChild(executableElement);
		}
		
		manager=new TargetActionManager(executableElement, ExecuteAction::new,ExecuteTarget::new);
	}
	
	@Override
	public Action getAction(){
		return manager.getAction();
	}
	
	private static final Pattern UriPattern=Pattern.compile("\"?[\\w\\.]+://[\\w/\\\\\\?=& ]+\"?");
	private static final Pattern StartDirPattern =Pattern.compile("^\"?(\\w:[\\\\/][\\w\\\\/!@#\\$%^&\\(\\)';,-\\[\\]\\{\\} ]+)[\\\\/]([\\w\\\\/!@#$%^&\\(\\)';,-\\[\\]\\{\\} ]+\\.[\\w]+)\"?$");				//group 1- Home dir, 2- fileName
	private static final Pattern StartDirPattern2=Pattern.compile("^\"(\\w:[\\\\/][\\w\\\\/!@#\\$%^&\\(\\)';,-\\[\\]\\{\\} ]+)[\\\\/]([\\w\\\\/!@#$%^&\\(\\)';,-\\[\\]\\{\\} ]+\\.[\\w]+)\" ?(.*)$");			//SAME							, 3- command line arguments
	private static final Pattern PathPattern=Pattern.compile("\"?\\w:\\\\[\\w\\d \\\\()']+\"?");
	protected boolean isUri(String com) {
		return UriPattern.matcher(InputCleaners.removeNational(com)).matches();
	}
	protected boolean isExecutable(String com) {
		String path=InputCleaners.removeNational(com);
		return StartDirPattern.matcher(path).matches()||StartDirPattern2.matcher(path).matches();
	}
	protected boolean isPath(String com) {
		return PathPattern.matcher(InputCleaners.removeNational(com)).matches();
	}
	
	private class ExecuteAction extends TargetAction{
		@Override
		protected boolean execute(Target target){
			return ((ExecuteTarget)target).execute();
		}
		@Override
		protected boolean executeNoTarget(){
			System.out.println(notKnowMessage);
			
			String newCom=ShaniCore.getIntend(ShaniString.class).value.toString();
			ShaniCore.debug.println("ExecuteOrderAddTargetInput: \""+newCom+'"');
			
			ShaniString unmatched=(ShaniString)parameters.get("unmatched");
			
			Boolean positive=ShaniCore.isInputPositive(new ShaniString(ShaniString.ParseMode.raw,newCom));
			if(positive!=null&&!positive)
				return false;
			
			if(isPath(newCom)) {
				return createTarget(unmatched,"dir",new String[] {InputCleaners.clear(newCom)});
			}
			if(isUri(newCom)) {
				return createTarget(unmatched,"start",new String[] {InputCleaners.clear(newCom)});
			}
			if(isExecutable(newCom)) {
				Matcher mat=StartDirPattern.matcher(InputCleaners.clear(newCom));
				if(!mat.matches()) {
					mat=StartDirPattern2.matcher(newCom);
					mat.matches();
					return createTarget(unmatched,"startdir",new String[] {mat.group(1),mat.group(2),mat.group(3)});
				}
				mat.group();
				
				return createTarget(unmatched,"startdir",new String[] {mat.group(1),mat.group(2)});
			}
			
			Pair<Pair<Short,Short>,Target> exec=manager.getTarget("execute", Map.of("unmatched",newCom));				//Input is not valid program/file/URL. Check if it's one of already existing keys.
			if(exec!=null) {
				((KeywordTarget)exec.second).keyword.add(unmatched);
				return execute(exec.second);
			}
			unrecognizedMessage.printOut();
			return false;
		}
		private boolean createTarget(ShaniString key, String targetType, String[] target) {
			ExecuteTarget newTarget=new ExecuteTarget(key,targetType,target);
			manager.registerNewTarget(newTarget);
			
			execute(newTarget);
			return true;
		}
		
		@Override
		public boolean connectAction(String action){
			ShaniCore.errorOccurred("Connecting not available now.");
			return false;
		}
	}
	private class ExecuteTarget extends KeywordTarget{
		private String targetType;
		private String[] target;
		
		ExecuteTarget(Element e){
			super(e);
			
			targetType=e.getElementsByTagName("type").item(0).getTextContent();
			target=new ShaniString(e.getElementsByTagName("todo").item(0).getTextContent()).getArray();
		}
		ExecuteTarget(ShaniString key,String targetType,String[] target) {
			super(key);
			
			this.targetType=targetType;
			this.target=target;
		}
		
		@Override
		public void setSaveElement(Element e){
			super.setSaveElement(e);
			
			Document doc=e.getOwnerDocument();
			Element e2=doc.createElement("type");
			e2.appendChild(doc.createTextNode(targetType));
			e.appendChild(e2);
			
			e2=doc.createElement("todo");
			e2.appendChild(doc.createTextNode(new ShaniString(target).toFullString()));
			e.appendChild(e2);
		}
		
		boolean execute() {
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
		private boolean execute(String command, int successExitVal) {
			try {
				Process proc=Runtime.getRuntime().exec(command);
				proc.waitFor();
				ShaniCore.debug.println(command+": "+proc.exitValue());
				return proc.exitValue()==successExitVal;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} catch (InterruptedException e) {
				return true;
			}
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