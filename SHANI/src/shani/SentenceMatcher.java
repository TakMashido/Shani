package shani;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**More powerfull matching engine than ShaniMatcher.
 * <pre>
 * 
 * Match whole sentence and get data out of it.
 * 
 * It gets data directly from one xml node.
 * 
 * Subnodes named "sentence" are sentence templates.
 * You can specifi "name" attribute in it for further recognizing which sentence was matched by matcher.
 * It's text content contain words representing Sentence Elements with additional special characters at from of each word to choose their type. Following word is name of sentence element.
 * If it's start with '*' character, would be not used durnig sentence mathing.
 * $ means ShaniString. It'll perform normal ShaniStrig Matching with this element.
 * ^ means regex. It'll try to match content with regex
 * ? means return Value. It'll store corresponding value from processed String in HashMap under given keyword.
 * 
 * Subnodes with other names are used to provide addional data for sentence elements.
 * E.g. element "$foo" will compare input value with ShaniString stored under "foo" subnode. This also apply for regex matching.
 * </pre>
 * 
 * @author TakMashido
 */
public class SentenceMatcher {
	private Sentence[] sentences;
	
	/**Creates new Senetnce Matcher Object based on data from given node.
	 * @param node XML Node which contain Senetnce templates and it's elements information.
	 */
	public SentenceMatcher(Node node) {
		var nodes=node.getChildNodes();
		HashMap<String,String> parts=new HashMap<String,String>();
		for(int i=0;i<nodes.getLength();i++) {
			if(nodes.item(i).getNodeName().equals("template")||nodes.item(i).getNodeType()==Node.TEXT_NODE)continue;
			parts.put(nodes.item(i).getNodeName(), nodes.item(i).getTextContent());
		}
		
		nodes=((Element)node).getElementsByTagName("template");
		sentences=new Sentence[nodes.getLength()];
		for(int i=0;i<nodes.getLength();i++) {
			Node nod=nodes.item(i);
			sentences[i]=new Sentence(parts,nod.getTextContent(),((Element)nod).getAttribute("name"));
		}
	}
	
	/**Process given String.
	 * @param string String in which engine search for matches.
	 * @return Array of {@code SentenceResoult} object representing resoults of successful matching.
	 */
	public SentenceResoult[] process(String string) {
		return process(new ShaniString(string,false));
	}
	/**Process given ShaniString.
	 * @param string {@code ShaniString} in which engine search for matches.
	 * @return Array of {@code SentenceResoult} object representing resoults of successful matching.
	 */
	public SentenceResoult[] process(ShaniString string) {
		var str=string.split(false);
		
		ArrayList<SentenceResoult> Return=new ArrayList<>();
		for(Sentence sen:sentences) {
			SentenceResoult sr=sen.process(str);
			if(sr.cost<Config.sentenseCompareTreshold)Return.add(sr);
		}
		
		return Return.toArray(new SentenceResoult[0]);
	}
	
	private class Sentence{
		private SentenceElement[] sentence;
		private String sentenceName;
		
		private Sentence(HashMap<String,String> parts,String content,String name) {
			@SuppressWarnings("resource")
			Scanner in=new Scanner(content);
			this.sentenceName=name;
			
			ArrayList<SentenceElement> sentenceTemp=new ArrayList<>();
			while(in.hasNext())sentenceTemp.add(createNewSentenceElement(parts,in.next()));
			sentence=sentenceTemp.toArray(new SentenceElement[0]);
		}
		
		private SentenceResoult process(ShaniString[][] str){
			var Return=new SentenceResoult[str.length];
			short[] costs=new short[str.length];
			
			for(int i=0;i<str.length;i++)
				costs[i]=mainProcess((Return[i]=new SentenceResoult(sentenceName)),str[i],0,0);
			
			int minIndex=0;
			for(int i=1;i<str.length;i++)
				if(costs[minIndex]>costs[i])
					minIndex=i;
			
			Return[minIndex].cost=costs[minIndex];
			return Return[minIndex];
		}
		
		private SentenceElement createNewSentenceElement(HashMap<String,String> parts,String data) {
			String name=data.substring(1);
			switch(data.charAt(0)) {
			case '*':return new OptionalElement(parts, name);
			case '?':return new DataReturnElement(parts, name);
			case '$':return new ShaniStringElement(parts, name);
			case '^':return new RegexElement(parts, name);
			}
			System.err.println("Unrecognized SenetnceMatcher elemet: "+data);
			return null;
		}
		private final short mainProcess(SentenceResoult resoult,ShaniString[]str,int strIndex,int sentenceIndex) {
//			System.out.println(strIndex+" "+sentenceIndex);
//			System.out.println(resoult.data);
			if(strIndex>=str.length&&sentenceIndex>=sentence.length)
				return 0;
			if(sentenceIndex>=sentence.length)
				return (short) (Config.wordDeletionCost*(str.length-strIndex));
			
			
			var ret=sentence[sentenceIndex].preProcess(resoult, str, strIndex, sentenceIndex);
//			System.out.println(resoult.data);
//			System.out.println("--------------: "+strIndex+" "+sentenceIndex+" "+ret);
			return ret;
		}
		
		private abstract class SentenceElement{
			protected short preProcess(SentenceResoult resoult,ShaniString[]str,int strIndex,int sentenceIndex) {
				if(strIndex>=str.length)
					return (short) (Config.wordInsertionCost*(sentence.length-sentenceIndex));
				return process(resoult,str,strIndex,sentenceIndex);
			}
			protected abstract short process(SentenceResoult resoult,ShaniString[]str,int strIndex,int sentenceIndex);
		}
		private class OptionalElement extends SentenceElement{
			private SentenceElement element;
			private OptionalElement(HashMap<String,String> parts,String data) {
				element=createNewSentenceElement(parts, data);
			}
			@Override
			protected short preProcess(SentenceResoult resoult, ShaniString[] str, int strIndex, int sentenceIndex) {
				if(strIndex>=str.length) {
					if(sentenceIndex==sentence.length-1) return 0;
					else return (short) (Config.wordInsertionCost*(sentence.length-sentenceIndex));
				}
				
				var resCopy=new SentenceResoult(sentenceName);
				
				short normalCost=element.process(resCopy, str, strIndex, sentenceIndex);
				if(normalCost<Config.optionalMatchTreshold)return normalCost;
				
				short skippedCost=mainProcess(resoult, str, strIndex, sentenceIndex+1);
				
				if(normalCost-skippedCost>Config.optionalMatchTreshold) {
					resoult.add(resCopy);
					return skippedCost;
				}
				return normalCost;
			}
			@Override
			protected short process(SentenceResoult resoult, ShaniString[] str, int strIndex, int sentenceIndex) {
				assert false:"Method process in OptionalSentenceElement shouldn't be invoced";
				return Short.MAX_VALUE;
			}
		}
		private class DataReturnElement extends SentenceElement{
			private String returnKey;
			private DataReturnElement(HashMap<String,String> parts,String data) {
				returnKey=data;
			}
			@Override
			protected short process(SentenceResoult resoult, ShaniString[] str, int strIndex, int sentenceIndex) {
				StringBuffer strBuf=new StringBuffer();
				if(sentenceIndex+1>=sentence.length) {			//last elem of sentence
					System.out.println("ret range2: "+strIndex+" "+str.length);
					strBuf.append(str[strIndex++]);
					for(;strIndex<str.length;strIndex++)strBuf.append(' ').append(str[strIndex]);
					resoult.data.put(returnKey, strBuf.toString());
					return 0;
				}
				assert !(sentence[sentenceIndex+1] instanceof DataReturnElement):"Two data elements shouldn't apper next to each other.";
				
				SentenceResoult retResoult=resoult.makeCopy();
				int minIndex=strIndex;
				short minCost=(short) (mainProcess(retResoult,str,strIndex,sentenceIndex+1)+Config.sentenseCompareTreshold);				//Add Config.sentenceCompareTreshold or Config.wordInsertionCost??
				for(int i=strIndex+1;i<=str.length;i++) {
					var tempResoult=resoult.makeCopy();
					short tempCost=mainProcess(tempResoult,str,i,sentenceIndex+1);
					if(tempCost<minCost) {
						minCost=tempCost;
						minIndex=i;
						retResoult=tempResoult;
					}
				}
				
				if(minIndex>strIndex) {
					strBuf.append(str[strIndex]);
					for(int i=strIndex+1;i<minIndex;i++) {
						strBuf.append(' ').append(str[i]);
					}
					resoult.set(retResoult);
					resoult.data.put(returnKey, strBuf.toString());
				} else resoult.set(retResoult);
				System.out.println("ret range: "+strIndex+" "+minIndex);
				return minCost;
			}
		}
		private class ShaniStringElement extends SentenceElement{
			private ShaniString[][] value;
			private ShaniStringElement(HashMap<String,String> parts,String data) {
				value=new ShaniString(data).split();
			}
			@Override
			protected short process(SentenceResoult resoult, ShaniString[] str, int strIndex, int sentenceIndex) {
				short[] subCosts=new short[value.length];
				var sr=new SentenceResoult[subCosts.length];
				
				for(int i=0;i<subCosts.length;i++) {
					var ret=ShaniString.getMatchingIndex(str, strIndex, value[i]);
					if(ret.cost<Config.wordCompareTreshold);
					subCosts[i]=(short) (mainProcess((sr[i]=new SentenceResoult(sentenceName)),str,ret.endIndex,sentenceIndex+1)+ret.cost);
				}
				
				int minIndex=0;
				for(int i=1;i<subCosts.length;i++) {
					if(subCosts[i]<subCosts[minIndex]) minIndex=i;
				}
				
				resoult.data.putAll(sr[minIndex].data);
				return subCosts[minIndex];
			}
		}
		private class RegexElement extends SentenceElement{
			private Pattern pattern;
			private RegexElement(HashMap<String,String> parts,String data) {
				pattern=Pattern.compile(parts.get(data));
			}
			@Override
			protected short process(SentenceResoult resoult, ShaniString[] str, int strIndex, int sentenceIndex) {
				int tempStrIndex;
				short deleteCost=0;
				for(tempStrIndex=strIndex;tempStrIndex<str.length;tempStrIndex++) {
					if(str[tempStrIndex].isEquals(pattern)) {
						if(deleteCost<Config.wordInsertionCost) {
							return (short)(mainProcess(resoult,str,tempStrIndex+1,sentenceIndex+1)+deleteCost);
						} else {
							return (short)(mainProcess(resoult,str,strIndex+1,sentenceIndex+1)+Config.wordInsertionCost);
						}
					}
					deleteCost+=Config.wordDeletionCost;
				}
				return (short)(mainProcess(resoult,str,strIndex,sentenceIndex+1)+Config.wordInsertionCost);
			}
			
		}
	}
	
	/**Object containing resoult of matching ShaniString by SenetenceMenager.
	 * @author TakMashido
	 */
	public class SentenceResoult{
		/**Map contating words appered in return node position.
		 */
		public final HashMap<String,String> data=new HashMap<String,String>();
		private short cost;
		/**Name of matched sentence. Specyfied by name attribute in representing xml node.
		 */
		public final String name;
		
		private SentenceResoult() {name=null;}
		private SentenceResoult(String name) {
			this.name=name;
		}
		
		/**Performs deep copy of this object. It not make copy of underlaying String, but there are immutale so no sense to doing it.
		 * @return Deep copy of this object.
		 */
		public SentenceResoult makeCopy() {
			var copy=new SentenceResoult(name);
			copy.data.putAll(data);
			copy.cost=cost;
			
			return copy;
		}
		private void set(SentenceResoult sr) {
			assert name==null?sr.name==null:name.equals(sr.name):"Propably trying to set values from very diffrend element";
			this.cost=sr.cost;
			data.clear();
			data.putAll(sr.data);
		}
		private void add(SentenceResoult sr) {
			assert name==null?sr.name==null:name.equals(sr.name):"Propably trying to set values from very diffrend element";
			this.cost+=sr.cost;
			data.putAll(sr.data);
		}
		
		/**Return's {@link SentenceResoult#data data} Map.
		 * @return {@link SentenceResoult#data data} Map.
		 */
		public HashMap<String,String> getData(){
			return data;
		}
		/**Return's {@link SentenceResoult#name} of matched sentence resoult.
		 * @return {@link SentenceResoult#name} of matched sentence resoult.
		 */
		public String getName() {
			return name;
		}
		public short getCost() {
			return cost;
		}
	}
	
	public static void main(String[]args) throws IOException, SAXException, ParserConfigurationException{
		SentenceMatcher mat=new SentenceMatcher(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("test.xml")).getElementsByTagName("sentence").item(0));
		
		var res=mat.process("test asdfghd test2222222 test22fghjk2");
		System.out.println("\nmatches number: "+res.length);
		for(int i=0;i<res.length;i++)
			System.out.println(res[i].cost+" "+res[i].data);
	}
}