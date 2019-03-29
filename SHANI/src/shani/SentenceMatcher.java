package shani;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**More powerfull matching engine than ShaniMatcher.
 * Tries to match whole sentence and get data out of it, not only apply some keywords.
 * @author TakMashido
 */
public class SentenceMatcher {
	private Sentence[] sentences;
	
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
			Node nod=nodes.item(0);
			sentences[i]=new Sentence(parts,nod.getTextContent(),((Element)nod).getAttribute("name"));
		}
		
	}
	private enum Type{
		data,string,regex,none;
		
		private static Type get(String val) {
			switch(val.charAt(0)) {
			case '?': return data;
			case '$': return string;
			case '^': return regex;
			default: return none;
			}
		}
	};
	
	/**Process given ShaniString.
	 * @param string {@code ShaniString} in which engine search for matches.
	 * @return Array of {@code SentenceResoult} object representing resoults of successful matching.
	 */
	public SentenceResoult[] process(ShaniString string) {
		var str=string.split();
		
		ArrayList<SentenceResoult> Return=new ArrayList<>();
		for(Sentence sen:sentences) {
			SentenceResoult sr=sen.process(str);
			if(sr!=null)Return.add(sr);
		}
		
		return Return.toArray(new SentenceResoult[0]);
	}
	
	private class SentenceElement{
		private Type type;
		private String string;
		private ShaniString[][] shaniStringArray;
		private Pattern regex;
		
		private SentenceElement(HashMap<String,String> parts,String data) {
			type=Type.get(data);
			String realData=data.substring(1);
			switch(type) {
			case data:string=realData;break;
			case string:
				String temp=parts.get(realData);
				if(temp==null)System.err.println("Can't find identifier "+data+" during processing shani.SentenceMather");
				else shaniStringArray=new ShaniString(temp).split();
				break;
			case regex:
				temp=parts.get(realData);
				if(temp==null)System.err.println("Can't find identifier "+data+" during processing shani.SentenceMather");
				regex=Pattern.compile(temp);
				break;
			default:System.err.println(realData+" in not valid sentence element of shani.SentenceMatcher");
			}
		}
	}
	private class Sentence{
		private SentenceElement[] sentence;
		private String name;
		
		private Sentence(HashMap<String,String> parts,String content,String name) {
			@SuppressWarnings("resource")
			Scanner in=new Scanner(content);
			this.name=name;
			
			ArrayList<SentenceElement> sentenceTemp=new ArrayList<>();
			SentenceElement elem;
			while(in.hasNext()&&((elem=new SentenceElement(parts,in.next()))!=null))sentenceTemp.add(elem);
			sentence=sentenceTemp.toArray(new SentenceElement[0]);
		}
		
		private SentenceResoult process(ShaniString[][] str){
			var Return=new SentenceResoult[str.length];
			short[] costs=new short[str.length];
			
			for(int i=0;i<str.length;i++)
				costs[i]=process((Return[i]=new SentenceResoult(name)),str[i],0,0);
			
			int minIndex=0;
			for(int i=1;i<str.length;i++)
				if(costs[minIndex]>costs[i])
					minIndex=i;
			
			//if(costs[minIndex]>=Config.sentenseCompareTreshold)return null;				//TODO uncomment this after dubugging
			Return[minIndex].cost=costs[minIndex];
			return Return[minIndex];
		}
		private short process(SentenceResoult Return,ShaniString[]str,int strIndex,int sentenceIndex) {			//Returns cost of compartition
			short cost=0;					//Check if can delete it.
			
			if(strIndex>=str.length&&sentenceIndex>=sentence.length) {
				return 0;
			}
			
			int tempStrIndex;
			var elem=sentence[sentenceIndex];
			switch(elem.type) {
			case data:
				assert sentence[sentenceIndex+1].type!=Type.data:"Two data elements shouldn't apper next to each other.";
				StringBuffer strBuf=new StringBuffer();
				if(sentenceIndex+1>=sentence.length) {			//end of sentence
					strBuf.append(str[strIndex++]);
					for(;strIndex<str.length;strIndex++)strBuf.append(' ').append(str[strIndex]);
					Return.data.put(elem.string, strBuf.toString());
					return 0;
				}
				int tempCost=0;
				tempStrIndex=strIndex;
				while(tempStrIndex<str.length&&(tempCost=process(Return,str,tempStrIndex++,sentenceIndex+1))>Config.sentenseCompareTreshold) {
					strBuf.append(str[tempStrIndex]).append(' ');
				}
				int length;
				if(!((length=strBuf.length())==0)) {				//Handle not matching sentence ending and direct matching beetwen two sentence elemnets surrounding this one(no data returned)
					strBuf.delete(length-1, length);
					Return.data.put(elem.string, strBuf.toString());
				} else {
					return (short)(cost+Config.sentenseCompareTreshold);				//which one? theoretically it is only one not matched element, but it is data return elem so can be no point in processing longer and return Config.sentenceSompareTreshold
//					return (short)(cost+Config.wordInsertionCost);
				}
			case string:
				short[] subCosts=new short[elem.shaniStringArray.length];
				var sr=new SentenceResoult[subCosts.length];
				
				for(int i=0;i<subCosts.length;i++) {
					var ret=ShaniString.getMatchingIndex(str, strIndex, sentence[sentenceIndex].shaniStringArray[i]);
					subCosts[i]=process((sr[i]=new SentenceResoult()),str,ret.endIndex,sentenceIndex+1);
				}
				
				int minIndex=0;
				for(int i=1;i<subCosts.length;i++) {
					if(subCosts[i]<subCosts[minIndex]) minIndex=i;
				}
				
				Return.data.putAll(sr[minIndex].data);
				return subCosts[minIndex];
			case regex:
				tempStrIndex=strIndex;
				short deleteCost=0;
				while(!str[tempStrIndex++].isEquals(elem.regex)) {
					deleteCost+=Config.wordDeletionCost;
				}
				
				if(deleteCost<Config.wordInsertionCost) {
					return (short)(process(Return,str,tempStrIndex,sentenceIndex+1)+deleteCost+cost);
				} else {
					return (short)(process(Return,str,strIndex+1,sentenceIndex+1)+Config.wordInsertionCost+cost);
				}
			default: 
				assert false:"Trying to process unrecognized content SentenceElement in SentenceMatcher.";
				return Config.wordInsertionCost;
			}
		}
	}
	
	/*private SentenceResoult process(ShaniString[]str) {				//copy of savenes
	var Return=new SentenceResoult();
	
	int sentenceIndex=0;
	int strIndex=-1;
	int matchedIndex=-1;
	int[] subIndexs = null;
	StringBuffer strBuf=new StringBuffer();
	while(++strIndex<str.length&&sentenceIndex<sentence.length) {
		var elem=sentence[sentenceIndex];
		switch(elem.type) {
		case data:
			subIndexs=null;
			if(sentenceIndex+1==sentence.length) {
				Return.data.put(elem.string, strBuf.toString());
			}
			if(sentenceIndex+1>=sentence.length) {			//end of sentence
				strBuf.append(str[strIndex++]);
				for(;strIndex<str.length;strIndex++)strBuf.append(' ').append(str[strIndex]);
				Return.data.put(elem.string, strBuf.toString());
				sentenceIndex++;
				break;
			}
			var elem2=sentence[sentenceIndex+1];
			switch(elem2.type) {
			case data:
				assert false:"Two data elements shouldn't apper next to each other.";
				sentenceIndex++;
				continue;
			case string:
				ShaniString sst=str[strIndex];
				boolean matched=false;
				for(int i=0;i<elem2.shaniStringArray.length;i++) {
					if(sst.equals(elem2.shaniStringArray[i][0])) {
						int length=strBuf.length()-1;
						if(length>0) {
							Return.data.put(elem.string, strBuf.delete(length, length+1).toString());
							strBuf.delete(0, length);
						}
						sentenceIndex++;
						strIndex--;
						matched=true;
						break;
					}
				}
				if(!matched)strBuf.append(sst).append(' ');
				break;
			case regex:
				if(!str[strIndex].isEquals(elem2.regex)) {
					strBuf.append(str[strIndex]).append(' ');
				} else {
					Return.data.put(elem.string, strBuf.toString());
					strBuf.delete(0, strBuf.length());
					sentenceIndex+=2;			//Skip already matched regex pattern.
					strIndex++;			
				}
				break;
			default:
				assert false:"Trying to process unrecognized content SentenceElement in SentenceMatcher.";
			}
			break;
		case string:
			if(subIndexs==null)subIndexs=new int[elem.shaniStringArray.length];
			int processed=0;
			boolean positive=false;
			for(int i=0;i<subIndexs.length;i++) {
				switch(subIndexs[i]) {				//-2 -> not matched, -1 -> matched, >=0 during checking
				case -1:
					positive=true;
				case -2:
					processed++;
					continue;
				}
				if(!str[strIndex].equals(elem.shaniStringArray[i][subIndexs[i]])) {
					subIndexs[i]=-2;
					continue;
				}
				subIndexs[i]++;
				if(subIndexs[i]==elem.shaniStringArray[i].length) {
					processed++;
					positive=true;
					subIndexs[i]=-1;
					matchedIndex=strIndex;
				}
			}
			if(processed==subIndexs.length) {				//All of sub ShaniStrings processed
				if(positive) {								//and one or more of them matched
					strIndex=matchedIndex;
					sentenceIndex++;
				} else {
					return null;
				}
			}
			break;
		case regex:
			subIndexs=null;
			if(!str[strIndex].isEquals(elem.regex)) {
				return null;
			}
			sentenceIndex++;
			break;
		default: 
			assert false:"Trying to process unrecognized content SentenceElement in SentenceMatcher.";
			//System.err.println("Trying to process unrecognized content SentenceElement in SentenceMatcher.");
		}
	}
	if(strIndex>=str.length&&sentenceIndex>=sentence.length)return Return;
	return null;
}*/
	
	/**Object containing resoult of matching ShaniString by SenetnceMenager
	 * @author TakMashido
	 */
	public class SentenceResoult{
		public final HashMap<String,String> data=new HashMap<String,String>();
		private short cost;
		private final String name;
		
		private SentenceResoult() {name=null;}
		private SentenceResoult(String name) {
			this.name=name;
		}
		
		public short getCost() {
			return cost;
		}
	}
	public static void main(String[]args) throws IOException, ParserConfigurationException, SAXException {
		Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("temp.xml"));
		var parser=new SentenceMatcher(doc.getElementsByTagName("node").item(0));
		var resoults=parser.process(new ShaniString("word is stefan idzie idzie gdzieœ królik"));
		for(var res:resoults)
			System.out.println(res.name+" "+res.data.keySet()+" "+res.data.values());
		System.out.println("End of matching");
	}
}