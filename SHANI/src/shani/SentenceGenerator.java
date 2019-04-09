package shani;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

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
			sentences[i]=new SentenceTemplate(nod.getTextContent());
		}
	}
	
	public boolean printOut(Map<String,ShaniString> params) {
		return printStream(System.out,params);
	}
	public boolean printStream(PrintStream stream,Map<String,ShaniString> params) {					//Can be more efficient if prints data directly do stream, not get string and then print it.
		var str=getString(params);
		if(str==null)return false;
		stream.println(str);
		return true;
	}
	public String getString(Map<String,ShaniString> params) {
		String Return;
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
		return null;
	}
	
	private class SentenceTemplate {
		private String[] elementNames;
//		private boolean[] isPresent;					//If corresponding key is presented in parts Map
		
		private SentenceTemplate(String template) {
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
		private String getString(Map<String,ShaniString> params) {
			StringBuffer Return=new StringBuffer();
			
			for(int i=0;i<elementNames.length;i++) {
				ShaniString toAdd;
				if((toAdd=params.get(elementNames[i]))!=null||(toAdd=mainParams.get(elementNames[i]))!=null) {
					Return.append(toAdd).append(' ');
				} else return null;
			}
			
			return Return.toString();
		}
	}
	
	public static void main(String[]args) throws SAXException, IOException, ParserConfigurationException {
		Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("test.xml"));
		
		var sg=new SentenceGenerator(doc.getElementsByTagName("sentence").item(0));
		var hm=new HashMap<String,ShaniString>();
		hm.put("b", new ShaniString("bb"));
		hm.put("a", new ShaniString("Dzia³a"));
		
		System.out.println(sg.printOut(hm));
		System.out.println("end");
	}
}