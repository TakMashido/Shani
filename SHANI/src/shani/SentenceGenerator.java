package shani;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SentenceGenerator {
	private SentenceTemplate[] sentences;
	private HashMap<String,ShaniString> mainParams=new HashMap<>();
	
	private static final Random random=new Random();
	
	public SentenceGenerator(Node node) {
		var nodes=node.getChildNodes();
		for(int i=0;i<nodes.getLength();i++) {
			if(nodes.item(i).getNodeName().equals("template")||nodes.item(i).getNodeType()==Node.TEXT_NODE)continue;
			mainParams.put(nodes.item(i).getNodeName(), new ShaniString(nodes.item(i).getTextContent()));
		}
		
		nodes=((Element)node).getElementsByTagName("template");
		sentences=new SentenceTemplate[nodes.getLength()];
		for(int i=0;i<nodes.getLength();i++) {
			Node nod=nodes.item(i);
			sentences[i]=new SentenceTemplate(((Element)(nod)).getAttribute("name"),nod.getTextContent());
		}
	}
	
	public boolean printOut(Map<String,? extends Object> params) {
		return printStream(System.out,params);
	}
	public boolean printStream(PrintStream stream,Map<String,? extends Object> params) {					//Can be more efficient if prints data directly do stream, not get string and then print it.
		var str=getString(params);
		if(str==null)return false;
		stream.println(str);
		return true;
	}
	public String getString(Map<String,? extends Object> params) {
		assert params!=null;
		
		String Return = null;
		int length=sentences.length;
		boolean[] checked=new boolean[length];
		int checkedNumber=0;
		while(checkedNumber<length) {							//On big arrays it is not effective. Prepared for small ones.
			int toCheck=random.nextInt(length);
			if(checked[toCheck])continue;
			if((Return=sentences[toCheck].getString(params))!=null) return Return;
			checked[toCheck]=true;
			checkedNumber++;
		}
		return Return;
	}
	
	public boolean printOut(String name,Map<String,? extends Object> params) {
		return printStream(name,System.out,params);
	}
	public boolean printStream(String name,PrintStream stream,Map<String,? extends Object> params) {
		var str=getString(name,params);
		if(str==null)return false;
		stream.println(str);
		return true;
	}
	public String getString(String name,Map<String,? extends Object> params) {
		assert params!=null;
		
		String Return=null;
		int length=sentences.length;
		boolean[] checked=new boolean[length];
		int checkedNumber=0;
		while(checkedNumber<length) {							//On big arrays it is not effective. Prepared for small ones.
			int toCheck=random.nextInt(length);
			if(checked[toCheck])continue;
			if(sentences[toCheck].name.equals(name)&&(Return=sentences[toCheck].getString(params))!=null) return Return;
			checked[toCheck]=true;
			checkedNumber++;
		}
		return Return;
	}
	
	private class SentenceTemplate {
		private final String[] elementNames;
//		private boolean[] isPresent;					//If corresponding key is presented in parts Map
		
		private final String name;
		
		private SentenceTemplate(String nodeName,String template) {
			name=nodeName;
			
			@SuppressWarnings("resource")
			Scanner in=new Scanner(template);
			
			var data=new ArrayList<String>();
			while(in.hasNext())data.add(in.next());
			elementNames=data.toArray(new String[data.size()]);
		}
		
		/*private void createPresentTable(){						//Not load all of them during initialization. Decrease start time and average memory usage(not all of instances will be used)
			if(isPresent!=null)return;
			isPresent=new boolean[elementNames.length];
			for(int i=0;i<elementNames.length;i++)isPresent[i]=mainParams.containsKey(elementNames[i]);
		}*/
		/*private boolean canPrint(Map<String,ShaniString> params) {
			createPresentTable();
			for(int i=0;i<elementNames.length;i++)if(!params.containsKey(elementNames[i]))return false;
			return true;
		}*/
		private String getString(Map<String,? extends Object> params) {
			StringBuffer Return=new StringBuffer();
			
			for(int i=0;i<elementNames.length;i++) {
				Object toAdd;
				if((toAdd=params.get(elementNames[i]))!=null||(toAdd=mainParams.get(elementNames[i]))!=null) {
					Return.append(toAdd.toString());
				} else return null;
			}
			
			Return.setCharAt(0, Character.toUpperCase(Return.charAt(0)));
			return Return.toString();
		}
	}
}