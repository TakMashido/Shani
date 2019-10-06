package shani;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**Generator of simple sentences.
 * <pre>
 * Creates sentence from given Object map based on templates from xml node.
 * 
 * Subnodes named "template" are templates for this generator.
 * Text content of this subnodes are words nameing parts of sentence.
 * You can also use name attribute to divide nodes into subgroups.
 * 
 * Main map of parts is created from rest of subnodes. Name become key, text content processed to {@link ShaniString} become value.
 * 
 * During procesing it try to create sentence from random template. If failed tryies to process another one until it successfully assembly sentence or run out of templates.
 * If specyfied name of sentence to process it uses only templates which same name String.
 * 
 * Template procesing is trying to find element name in Map&ltString,?&gt given to process, if it doesn't contain given keyword seach in main map of parts.
 * Note it doesn't insert spaces beewen words you have to mark you want them by putting them inside parts of map.
 * 
 * </pre>
 * @author TakMashido
 */
public class SentenceGenerator {
	private SentenceTemplate[] sentences;
	private HashMap<String,ShaniString> mainParams=new HashMap<>();
	
	private static final Random random=new Random();
	
	/**Creates Senetence Generator from given xml Node.
	 * @param node XML node containig necessary data.
	 */
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
	
	/**Asseble sentence and prints it to System.out.
	 * @param params Additional data for creator.
	 * @return If opertion sucessfull.
	 */
	public boolean printOut(Map<String,? extends Object> params) {
		return printStream(System.out,params);
	}
	/**Asseble sentence and prints it to given PrintStream.
	 * @param stream Stream in which data are printed.
	 * @param params Additional data for creator.
	 * @return If opertion sucessfull.
	 */
	public boolean printStream(PrintStream stream,Map<String,? extends Object> params) {					//Can be more efficient if prints data directly do stream, not get string and then print it.
		var str=getString(params);
		if(str==null)return false;
		stream.println(str);
		return true;
	}
	/**Asseble sentence.
	 * @param params Additional data for creator.
	 * @return Assebled senetence or null if failed doing it.
	 */
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
	
	 /**Asseble sentence and prints it to System.out.
	 * @param name Name of sentence to process.
	 * @param params Additional data for creator.
	 * @return If opertion sucessfull.
	 */
	public boolean printOut(String name,Map<String,? extends Object> params) {
		return printStream(name,System.out,params);
	}
	/**Asseble sentence and prints it to given PrintStream.
	 * @param name Name of sentence to process.
	 * @param stream Stream in which data are printed.
	 * @param params Additional data for creator.
	 * @return If opertion sucessfull.
	 */
	public boolean printStream(String name,PrintStream stream,Map<String,? extends Object> params) {
		var str=getString(name,params);
		if(str==null)return false;
		stream.println(str);
		return true;
	}
	/**Asseble sentence.
	 * @param name Name of sentence to process.
	 * @param params Additional data for creator.
	 * @return Assebled senetence or null if failed doing it.
	 */
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
		System.err.printf("Can't generate sentence with name %s and params %s",name,params.keySet().toString());
		return name+" "+params.toString();
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