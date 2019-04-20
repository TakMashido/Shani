package shani;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
			Node nod=nodes.item(i);
			sentences[i]=new Sentence(parts,nod.getTextContent(),((Element)nod).getAttribute("name"));
		}
	}
	
	/**Process given String.
	 * @param string {@code ShaniString} in which engine search for matches.
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
			
			Return[minIndex].cost=costs[minIndex];
			return Return[minIndex];
		}
		private short process(SentenceResoult resoult,ShaniString[]str,int strIndex,int sentenceIndex) {			//Returns cost of compartition
//			System.out.println(strIndex+" "+sentenceIndex+" "+(sentenceIndex<sentence.length?sentence[sentenceIndex].type:"none"));
			short ReturnValue=Short.MAX_VALUE;									//useful for debuging					
			try {
			if(strIndex>=str.length&&sentenceIndex>=sentence.length) {
				ReturnValue= 0;
				return ReturnValue;
			}
			if(sentenceIndex>=sentence.length) {
				ReturnValue= (short) (Config.wordDeletionCost*(str.length-strIndex));
				return ReturnValue;
			}
			if(strIndex>=str.length) {
				ReturnValue= (short) (Config.wordInsertionCost*(sentence.length-sentenceIndex));
				return ReturnValue;
			}
			
			int minIndex;
			int tempStrIndex;
			var elem=sentence[sentenceIndex];
			switch(elem.type) {
			case data:										//TODO optymalize by decreasing number of self executions. If next sentence element is $ can predict start location of optimal match by ShaniString.getMatchingIndexMovable method.
				StringBuffer strBuf=new StringBuffer();
				if(sentenceIndex+1>=sentence.length) {			//end of sentence
					strBuf.append(str[strIndex++]);
					for(;strIndex<str.length;strIndex++)strBuf.append(' ').append(str[strIndex]);
					resoult.data.put(elem.string, strBuf.toString());
					ReturnValue= 0;
					return ReturnValue;
				}
				assert sentence[sentenceIndex+1].type!=Type.data:"Two data elements shouldn't apper next to each other.";
				short minCost=Short.MAX_VALUE;
				minIndex=0;
				SentenceResoult retResoult=null;
				for(int i=strIndex;i<str.length;i++) {
					var tempResoult=new SentenceResoult();
					short tempCost=process(tempResoult,str,i,sentenceIndex+1);
					if(tempCost<minCost) {
						minCost=tempCost;
						minIndex=i;
						retResoult=tempResoult;
					}
				}
				
				if(strIndex<minIndex) {
					strBuf.append(str[strIndex]);
					for(int i=strIndex+1;i<minIndex;i++) {
						strBuf.append(' ').append(str[i]);
					}
					resoult.data.put(elem.string, strBuf.toString());
					resoult.data.putAll(retResoult.data);
					ReturnValue= minCost;
					return ReturnValue;
				} else {
					ReturnValue= Config.sentenseCompareTreshold;				//which one? theoretically it is only one not matched element, but it is data return elem so can be no point in processing longer and return Config.sentenceSompareTreshold
					return ReturnValue;
//					return (short)(cost+Config.wordInsertionCost);
				}
			case string:
				short[] subCosts=new short[elem.shaniStringArray.length];
				var sr=new SentenceResoult[subCosts.length];
				
				for(int i=0;i<subCosts.length;i++) {
					var ret=ShaniString.getMatchingIndex(str, strIndex, sentence[sentenceIndex].shaniStringArray[i]);
					subCosts[i]=(short) (process((sr[i]=new SentenceResoult()),str,ret.endIndex,sentenceIndex+1)+ret.cost);
				}
				
				minIndex=0;
				for(int i=1;i<subCosts.length;i++) {
					if(subCosts[i]<subCosts[minIndex]) minIndex=i;
				}
				
				resoult.data.putAll(sr[minIndex].data);
				ReturnValue= subCosts[minIndex];
				return ReturnValue;
			case regex:
				short deleteCost=0;
				for(tempStrIndex=strIndex;tempStrIndex<str.length;tempStrIndex++) {
					if(str[tempStrIndex].isEquals(elem.regex)) {
						if(deleteCost<Config.wordInsertionCost) {
							ReturnValue= (short)(process(resoult,str,tempStrIndex+1,sentenceIndex+1)+deleteCost);
							return ReturnValue;
						} else {
							ReturnValue= (short)(process(resoult,str,strIndex+1,sentenceIndex+1)+Config.wordInsertionCost);
							return ReturnValue;
						}
					}
					deleteCost+=Config.wordDeletionCost;
				}
				ReturnValue= (short)(process(resoult,str,strIndex,sentenceIndex+1)+Config.wordInsertionCost);
				return ReturnValue;
				
			default: 
				assert false:"Trying to process unrecognized content SentenceElement in SentenceMatcher.";
				ReturnValue= Config.wordInsertionCost;
				return ReturnValue;
			}
			}finally {
//				System.out.println("---------: "+strIndex+" "+sentenceIndex+" "+ReturnValue);
			}
		}
	}
	
	/**Object containing resoult of matching ShaniString by SenetenceMenager
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
		
		public HashMap<String,String> getData(){
			return data;
		}
		public String getName() {
			return name;
		}
		public short getCost() {
			return cost;
		}
	}
}