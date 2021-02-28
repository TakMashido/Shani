package takMashido.shani.core.text;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**Generator of simple sentences.
 * <pre>
 * Creates sentence from given Object map based on templates from xml node.
 * 
 * Subnodes named "template" are templates for this generator.
 * Text content of this subnodes are interpreted in following way:
 * Any thing in curved brackets {} is taken as parameter to insert into sentence.
 * Tokens inside square brackets [] are parsed as {@link ShaniString} objects.
 * Everything outside the brackets is treated as normal string.
 *
 * You can store sentence template name as "name" attribute of this node.
 *
 * Every other subnode is treated as insertable sentence part and it's text content is parsed as {@link ShaniString}.
 * It's inserted in place of tokens in {} brackets with same name.
 *
 * Consider following initializing node:
 * {@code
 * <weathersentence>
 * 	<template name="weather">{weather}, od {tempMin} do {tempMax} {degree}.</template>
 * 	<template name="weather">{weather}, {tempMin} {degree}.</template>
 * 	<template name="weather">{weather}, {tempMax} {degree}.</template>
 * 	<template name="temperature">Od {tempMin} do {tempMax} {degree}.</template>
 * 	<template name="windSpeed">{windSpeed} [km na godzinę* km/h]</template>
 * 	<template name="negativeRespond">[tak*youp]</template>
 * 	<template name="positiveRespond">[nie*niet*nope]</template>
 * 	<degree>stopni*celcjuszów</degree>
 * </weathersentence>
 * }
 *
 * When invocating {@link #getString(String,Map)} with name="weather" and parts containing "weather"="Słonecznie" ,"tempMin"=10 and "tempMax"=15 it can randomly return couple of strings e.g.:
 * "Słonecznie, od 10 do 15 stopni." from first template,
 * "Słonecznie, 10 celcjuszów" from second template.
 *
 * Providing only "weather" and "tampMin" part forces it to build string only form second template.
 *
 * Invoicing with name="positiveRespond" returns "tak" or "youp" as standard {@link ShaniString} is doing.
 *
 * When name is not specified by using {@link #getString(Map)} it chooses one template without not given parts.
 * You can also use {@link #getString(String)} to choose sentece only by name, make sure it contain only non parts specified in initializing node.
 *
 * When valid sentence is not found it prints only templateName+" "+parts.toString(). And prints error message to System.err.
 *
 * </pre>
 * @author TakMashido
 */
public class SentenceGenerator {
	protected SentenceTemplate[] sentences;
	protected HashMap<String,ShaniString> mainParams=new HashMap<>();
	
	private static final Random random=new Random();
	
	/**Creates Sentence Generator from given xml Node.
	 * @param node XML node containing necessary data.
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
	
	/**Assemble sentence and prints it to System.out.
	 * @param parts Additional data for creator.
	 * @return If operation successful.
	 */
	public boolean printOut(Map<String,? extends Object> parts) {
		return printStream(System.out,parts);
	}
	/**Assemble sentence and prints it to given PrintStream.
	 * @param stream Stream in which data are printed.
	 * @param parts Additional data for creator.
	 * @return If operation successful.
	 */
	public boolean printStream(PrintStream stream,Map<String,? extends Object> parts) {					//Can be more efficient if prints data directly do stream, not get string and then print it.
		var str=getString(parts);
		if(str==null)return false;
		stream.println(str);
		return true;
	}
	/**Assemble sentence.
	 * @param parts Additional data for creator.
	 * @return Assembled sentence or null if failed doing it.
	 */
	public String getString(Map<String,? extends Object> parts) {
		@SuppressWarnings("unchecked")
		Map<String,Object> partsToPrint=(Map<String,Object>)mainParams.clone();
		if(parts!=null)
			partsToPrint.putAll(parts);
		
		String Return = null;
		int length=sentences.length;
		boolean[] checked=new boolean[length];
		int checkedNumber=0;
		while(checkedNumber<length) {							//On big arrays it is not effective. Prepared for small ones.
			int toCheck=random.nextInt(length);
			if(checked[toCheck])continue;
			
			if((Return=sentences[toCheck].getString(partsToPrint))!=null)
				return Return;
			
			checked[toCheck]=true;
			checkedNumber++;
		}
		
		return parts.toString();
	}
	
	 /**Assemble sentence and prints it to System.out.
	 * @param name Name of sentence to process.
	 * @param parts Additional data for creator.
	 */
	public void printOut(String name,Map<String,? extends Object> parts) {
		printStream(name,System.out,parts);
	}
	/**Assemble sentence and prints it to given PrintStream.
	 * @param name Name of sentence to process.
	 * @param stream Stream in which data are printed.
	 * @param parts Additional data for creator.
	 */
	public void printStream(String name,PrintStream stream,Map<String,? extends Object> parts) {
		var str=getString(name,parts);
		if(str!=null)
			stream.println(str);
	}
	/**Get sentence with specified name.
	 * @param name Name of sentence to assembly.
	 * @return Assembled sentence.
	 */
	public String getString(String name){
		return getString(name,null);
	}
	/**Assemble sentence.
	 * @param name Name of sentence to process.
	 * @param parts Additional data for creator.
	 * @return Assembled sentence or null if failed doing it.
	 */
	public String getString(String name,Map<String,? extends Object> parts) {
		@SuppressWarnings("unchecked")
		Map<String,Object> partsToPrint=(Map<String,Object>)mainParams.clone();
		if(parts!=null)
			partsToPrint.putAll(parts);
		
		String Return=null;
		int length=sentences.length;
		boolean[] checked=new boolean[length];
		int checkedNumber=0;
		while(checkedNumber<length) {							//On big arrays it is not effective. Prepared for small ones.
			int toCheck=random.nextInt(length);
			if(checked[toCheck])continue;
			
			if(sentences[toCheck].name.equals(name)&&(Return=sentences[toCheck].getString(partsToPrint))!=null)
				return Return;
			
			checked[toCheck]=true;
			checkedNumber++;
		}
		System.err.printf("Can't generate sentence with name %s and params %s",name,parts.keySet().toString());
		return name+" "+parts.toString();
	}
	
	protected static class SentenceTemplate {
		private final SentenceElement[] elements;
		
		protected final String name;
		
		protected SentenceTemplate(String nodeName,String sentenceTemplate) {
			name=nodeName;
			
			var ret=new ArrayList<SentenceElement>();

			char[] template=sentenceTemplate.toCharArray();
			int i=0;
			while(i<template.length){
				int context=0;				//What kind of sentence element is now processed.
											//0-String, 1-ShaniString, 2-parts
				if(template[i]=='[') {
					context = 1;
					i++;
				}
				if(template[i]=='{') {
					context = 2;
					i++;
				}
				
				int j=i+1;
				tokenEndSearch:
				while(j<template.length){
					switch (template[j]){
						case '{','}','[',']':
							break tokenEndSearch;
					}
					j++;
				}

				String token=new String(template,i,j-i);
				switch (context){
				case 0:
					ret.add(new StringSentenceElement(token));
					break;
				case 1:
					j++;							//closing bracket
					ret.add(new ShaniStringSentenceElement(token));
					break;
				case 2:
					j++;							//closing bracket
					ret.add(new PartsSentenceElement(token));
					break;
				}
				
				i=j;
			}
			
			elements=ret.toArray(new SentenceElement[ret.size()]);
		}
		
		protected String getString(Map<String,? extends Object> parts) {
			StringBuilder ret=new StringBuilder();
			
			for(int i=0;i<elements.length;i++) {
				if(!elements[i].append(ret,parts))
					return null;
			}
			
			return ret.toString();
		}

		protected static abstract class SentenceElement{
			/**Append sentence element value into final sentence.
			 * @param target Where to push this part of sentence.
			 * @param parts Parts of sentence.
			 * @return If successfully executed. If false is returned SentenceMatcher assumes Sentence using this Element is not applicable for provided data.
			 */
			protected abstract boolean append(StringBuilder target, Map<String,? extends Object> parts);
		}
		protected static class StringSentenceElement extends SentenceElement{
			private String value;
			
			protected StringSentenceElement(String value){
				this.value=value;
			}
			
			@Override
			protected boolean append(StringBuilder target, Map<String, ?> parts) {
				target.append(value);
				return true;
			}
		}
		protected static class ShaniStringSentenceElement extends SentenceElement{
			private ShaniString value;
			
			protected ShaniStringSentenceElement(String value){
				this.value=new ShaniString(value);
			}
			
			@Override
			protected boolean append(StringBuilder target, Map<String, ?> parts) {
				target.append(value);
				return true;
			}
		}
		protected static class PartsSentenceElement extends SentenceElement{
			private String elementName;
			
			protected PartsSentenceElement(String elementName){
				this.elementName=elementName;
			}
			
			@Override
			protected boolean append(StringBuilder target, Map<String, ?> parts) {
				if(parts.containsKey(elementName)) {
					target.append(parts.get(elementName));
					return true;
				}
				return false;
			}
		}
		
		@Override
		public String toString() {
			return "SentenceTemplate, name="+name;
		}
	}
}