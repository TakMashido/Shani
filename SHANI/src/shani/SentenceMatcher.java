package shani;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import liblaries.Pair;
import shani.SentenceMatcher.Tokenizer.SentenceToken;
import shani.SentenceMatcher.Tokenizer.SentenceToken.Type;

/**More powerful matching engine than ShaniMatcher.
 * <pre>
 * Match whole sentence and get data out of it.
 * 
 * It gets data directly from one xml node.
 * 
 * Subnodes named "sentence" are sentence templates.
 * You can specify "name" attribute in it for further recognizing which sentence was matched by matcher.
 * 
 * It's text content contain words representing Sentence Elements with additional special characters at from of each word to choose their type. Following word is name of sentence element.
 * If it's start with '*' character, could be skipped during sentence matching.
 * $ means ShaniString. It'll perform normal ShaniStrig Matching with this element.
 * ^ means regex. It'll try to match content with regex.
 * ? means return value. It'll store corresponding value from processed String in HashMap under given keyword.
 * You can take part of sentence into brackets to make group it. Placing * before it means optional match of sentence inside.
 * 
 * Subnodes with other names are used to provide additional data for sentence elements.
 * E.g. element "$foo" will compare input value with ShaniString stored under "foo" subnode. This also apply for regex matching.
 * 
 * Examples:
 * 
 * {@code <node>
 * 	<template name="morning">$greetings ?who</template>
 * 	<template name="afternoon">$greetings2 ?who</template>
 * 	<greetings>hello*good morning</greetings>
 * 	<greetings2>good afternoon</greetings2>
 * <node>}
 * Will match sentence "good morning mister" with element "mister" under "who" key in returned HashMap. Name of matched sentence is "morning".
 * Sentence "good afternoon foo" results: name=afternoon, Return Map: {who=foo}
 * Sentence "good morning" aren't have any match.
 * Sentence "good night bar" will not be matched(depends on values in Config file).
 * 
 * {@code<template>*$bar</template>}
 * Matcher with this setup tries to match ShaniString stored under "bar" but it can also match empty sentence.
 * 
 * {@code<template>$foo *(^regex ?return)</template>}
 * Assume ShaniString stored under foo is already matched. Next it tries to match sentence in brackets. If fail try to match with this part skipped.
 * 
 * Sentences are evaluated in order in which they appear inside xml node possibly causing invoking action marked by first one if compare costs are equal.
 * </pre>
 * 
 * @author TakMashido
 */
public class SentenceMatcher {
	private Sentence[] sentences;
	
	/**Creates new Sentence Matcher Object based on data from given node.
	 * @param node XML Node which contain Sentence templates and it's elements information.
	 */
	public SentenceMatcher(Node node) {
		var nodes=node.getChildNodes();
		HashMap<String,String> parts=new HashMap<String,String>();
		for(int i=0;i<nodes.getLength();i++) {
			if(nodes.item(i).getNodeName().equals("template")||nodes.item(i).getNodeType()==Node.TEXT_NODE)continue;
			parts.put(nodes.item(i).getNodeName(), nodes.item(i).getTextContent());
		}
		
		boolean error=false;
		Tokenizer tokenizer=new Tokenizer();
		nodes=((Element)node).getElementsByTagName("template");
		sentences=new Sentence[nodes.getLength()];
		for(int i=0;i<nodes.getLength();i++) {
			Node nod=nodes.item(i);
			var tokens=tokenizer.tokenize(nod.getTextContent());
			if(tokens==null) {
				error=true;
				continue;
			}
			
			sentences[i]=new Sentence(parts,tokens,((Element)nod).getAttribute("name"));
		}
		if(error) {
			ArrayList<Sentence> sentencesList=new ArrayList<>(sentences.length);
			for(Sentence sen:sentences) {
				if(sen!=null)sentencesList.add(sen);
			}
			sentences=sentencesList.toArray(sentences);
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
	 * @return Array of {@code SentenceResoult} object representing results of successful matching.
	 */
	public synchronized SentenceResoult[] process(ShaniString string) {
		var str=string.split(false);
		
		ArrayList<SentenceResoult> Return=new ArrayList<>();
		for(Sentence sen:sentences) {
			SentenceResoult sr=sen.process(str);
			if(sr!=null&&sr.cost<Config.sentenseCompareTreshold)Return.add(sr);
		}
		
		return Return.toArray(new SentenceResoult[Return.size()]);
	}
	
	/**Tokenizer for parsing sentence templates.
	 * Not thread save, can parse only one at time.*/
	protected static class Tokenizer{
		private static String errorMessage="SentenceMatcher tokenizer error: in \"%s\" %s%n";
		protected static class SentenceToken{
			protected static enum Type{
				ShaniString,			//$
				Regex,					//^
				DataReturn,				//?
				Optional(false),		//*
				Or(false),				//|
				Group(')'),				//()
				AnyOrderGroup(']'),		//[]
				
				//Helper tokens
				Root;
				
				protected boolean containData=true;				//If search for text based data after occurrence of token start character
				protected char closingCharacter;
				
				Type(){}
				Type(boolean isSimple){
					this.containData=isSimple;
				}
				Type(char closingCharacter){
					containData=false;
					this.closingCharacter=closingCharacter;
				}
				
				protected static Type getType(char identifier) {
					switch(identifier) {
					case '$': return ShaniString;
					case '^': return Regex;
					case '?': return DataReturn;
					case '*': return Optional;
					case '|': return Or;
					case '(': return Group;
					case '[': return AnyOrderGroup;
					}
					return null;
				}
			};
			
			protected Type type;
			protected String content;
			protected List<SentenceToken> subTokens;
		}
		
		int index=0;						//Cause of not being thread save
		
		protected List<SentenceToken> tokenize(String str) {
			index=0;
			List<SentenceToken> tokens=generateTokens(str,'\u0000');
			if(tokens==null)return null;				//Parsing error occurred, data already printed to System.err and Engine notified  
			
			mergeTokens(str, tokens);
			
			return tokens;
		}
		
		private List<SentenceToken> generateTokens(String str, char closingCharacter) {
			ArrayList<SentenceToken> ret=new ArrayList<>();
			
			boolean closingCharacterFound=false;
			for(;index<str.length();index++) {
				if(str.charAt(index)==closingCharacter) {
					closingCharacterFound=true;
					break;
				}
				if(Character.isWhitespace(str.charAt(index)))continue;
				
				SentenceToken token=new SentenceToken();
				token.type=SentenceToken.Type.getType(str.charAt(index));
				
				if(token.type==null) {
					System.err.printf(errorMessage,str,"encountered invalid character '"+str.charAt(index)+"' at index "+index);
					Engine.registerLoadException();
					return null;
				}
				
				if(token.type.containData) {
					index++;
					int j=index;
					for(;j<str.length()&&Character.isLetterOrDigit(str.charAt(j));j++) {}
					if(j==index) {
						System.err.printf(errorMessage,str,"encountered token without identifier at index "+(index-1));
						Engine.registerLoadException();
						return null;
					}
					
					token.content=str.substring(index, j);
					index=j-1;
				} else if(token.type.closingCharacter!=0) {
					index++;
					token.subTokens=generateTokens(str, token.type.closingCharacter);
					if(token.subTokens==null)
						return null;
				}
				
				ret.add(token);
			}
			
			if(closingCharacter!=0&&!closingCharacterFound) {
				System.err.printf(errorMessage,str,"can't find '"+closingCharacter+"' character to close group.");
				Engine.registerLoadException();
				return null;
			}
			
			return ret;
		}
		/**Merges or and optional tokens with it's target
		 * @param str Just for debug, to print whole sentence template to System.err if error occurs 
		 * @param tokensSet of tokens to filter
		 */
		private List<SentenceToken> mergeTokens(String str, List<SentenceToken> tokens) {
			for(int i=0;i<tokens.size();i++) {
				SentenceToken token=tokens.get(i);
				
				if(token.type==Type.Or) {
					if(i==0||i+1==tokens.size()) {
						System.err.printf(errorMessage,str,"'or' element have to between two others elements.");
						Engine.registerLoadException();
						return null;
					}
					
					token.subTokens=new ArrayList<SentenceToken>();
					token.subTokens.add(tokens.get(i-1));
					token.subTokens.add(tokens.get(i+1));
					
					tokens.remove(i+1);
					tokens.remove(i-1);
				} else if(token.type==Type.Optional) {
					if(i+1==tokens.size()) {
						System.err.printf(errorMessage,str,"'optional' can't be last token in group.");
						Engine.registerLoadException();
						//return null;				//Not critical, don't have to end whole operation
					}
					
					token.subTokens=new ArrayList<SentenceToken>();
					token.subTokens.add(tokens.get(i+1));
					tokens.remove(i+1);
				}
				
				if(token.subTokens!=null)
					mergeTokens(str, token.subTokens);
			}
			
			return tokens;
		}
	}
	protected class Sentence{
		private SentenceElement root;
		private String sentenceName;
		
		protected Sentence(HashMap<String,String> parts,List<SentenceToken> tokens,String name) {
			root=decodeGroup(parts, tokens).first;
			sentenceName=name;
		}
		protected Pair<SentenceElement,SentenceElement> decodeGroup(HashMap<String,String> parts,List<SentenceToken> tokens) {
			SentenceElement ret=null;
			
			SentenceElement element=null;
			SentenceElement current=null;
			for(SentenceToken token:tokens) {
				if(token.type==Type.Group)
					element=decodeGroup(parts, token.subTokens).first;
				else
					element=createElement(parts, token);
				
				
				if(current==null)
					ret=element;
				else
					current.linkElement(element);
				current=element;
			}
			
			return new Pair<SentenceElement,SentenceElement>(ret,current);
		}
		protected SentenceElement createElement(HashMap<String,String> parts, SentenceToken token) {
			switch (token.type) {
			case ShaniString:
				return new ShaniStringElement(parts, token);
			case DataReturn:
				return new DataReturnElement(parts, token);
			case Regex:
				return new RegexElement(parts, token); 
			case Optional:
				return new OptionalElement(parts, token);
			default:
				assert false:"Unknow token type encountered";
			}
			
			return null;
		}
		
		protected SentenceResoult process(ShaniString[][] str){
			var Return=new SentenceResoult[str.length];
			
			for(int i=0;i<str.length;i++)
				root.process((Return[i]=new SentenceResoult(sentenceName)),str[i],0);
			
			int minIndex=-1;
			short minCost=Short.MAX_VALUE;
			for(int i=0;i<str.length;i++) {
				if(Return[i].cost<Config.sentenseCompareTreshold) {
					short tmpCost=Return[i].getCombinedCost();
					if(tmpCost<minCost) {
						minCost=tmpCost;
						minIndex=i;
					}
				}
			}
			
			return minIndex!=-1?Return[minIndex]:null;
		}
		
		protected abstract class SentenceElement{
			protected SentenceElement nextElement;
			
			protected void linkElement(SentenceElement nextElement) {
				this.nextElement=nextElement;
			}
			
			/**Individual processing handling*/
			protected abstract void process(SentenceResoult resoult, ShaniString[] str, int strIndex);
			/**Process next element and handles end of matched string or end of sentence pattern.*/
			protected void processNext(SentenceResoult resoult, ShaniString[] str, int strIndex) {
				if(nextElement!=null) {
					if(strIndex<str.length) {
						nextElement.process(resoult, str, strIndex);
					} else {
						resoult.cost+=Config.wordDeletionCost*(strIndex-str.length+1);
					}
				} else {
					if(strIndex<str.length)
						resoult.cost+=Config.wordInsertionCost;
				}
			}
			
			@Override
			public String toString() {
				return getClass().getSimpleName().substring(0,5);
			}
		}
		protected class OptionalElement extends SentenceElement{
			private SentenceElement optionalElement;
			private SentenceElement lastOptionalElement;					//Last element being optional, used to link it to the following element
			
			protected OptionalElement(HashMap<String,String> parts,SentenceToken data) {
				SentenceToken optionalToken=data.subTokens.get(0);
				
				if(optionalToken.type==Type.Group) {
					var elems=decodeGroup(parts, optionalToken.subTokens);
					optionalElement=elems.first;
					lastOptionalElement=elems.second;
				} else {
					optionalElement=lastOptionalElement=createElement(parts, optionalToken);
				}
			}
			
			@Override
			protected void linkElement(SentenceElement nextElement) {
				super.linkElement(nextElement);
				
				lastOptionalElement.linkElement(nextElement);
			}
			
			@Override
			protected void process(SentenceResoult resoult, ShaniString[] str, int strIndex) {
				SentenceResoult skippedResoult=resoult.makeCopy();
				if(nextElement!=null)
					nextElement.process(skippedResoult, str, strIndex);
				
				optionalElement.process(resoult, str, strIndex);
				
				if(skippedResoult.getCombinedCost()<=resoult.getCombinedCost()) {
					resoult.set(skippedResoult);
				}
			}
			
			@Override
			public String toString() {
				return "Optional:"+optionalElement.toString();
			}
		}
		protected class DataReturnElement extends SentenceElement{
			private String returnKey;
			
			protected DataReturnElement(HashMap<String,String> parts,SentenceToken data) {
				returnKey=data.content;
			}
			
			@Override
			protected void process(SentenceResoult resoult, ShaniString[] str, int strIndex) {
				assert !(nextElement instanceof DataReturnElement):"Two data return elements shouldn't apper next to each other.";
				
				
				SentenceResoult retResoult=null;
				int minIndex=-1;
				if(nextElement==null) {			//last element of sentence
					minIndex=str.length;
					retResoult=resoult;
				} else {
					short minCost=Short.MAX_VALUE;
					for(int i=strIndex+1;i<=str.length;i++) {
						var tempResoult=resoult.makeCopy();
						processNext(tempResoult,str,i);
						
						if(tempResoult.cost>=Config.sentenseCompareTreshold)continue;
						short tempCost=tempResoult.getCombinedCost();
						
						if(tempCost<minCost) {
							minCost=tempCost;
							minIndex=i;
							retResoult=tempResoult;
						}
					}
				}
				
				if(minIndex>strIndex) {
					StringBuffer strBuf=new StringBuffer();
					strBuf.append(str[strIndex]);
					for(int i=strIndex+1;i<minIndex;i++) {
						strBuf.append(' ').append(str[i]);
					}
					resoult.set(retResoult);
					resoult.data.put(returnKey, strBuf.toString());
					resoult.importanceBias+=Config.sentenceMatcherWordReturnImportanceBias*(minIndex-strIndex);
				} else
					resoult.cost+=Config.sentenseCompareTreshold;				//Nothing matched, technically should be wordInsertionCost but dataReturn element is for gathering data, making it able to not gather it have no sense and every data gathered by it will have to be checked for existence later  
			}
			
			@Override
			public String toString() {
				return "DataReturn:"+returnKey;
			}
		}
		protected class ShaniStringElement extends SentenceElement{
			private ShaniString[][] value;

			protected ShaniStringElement(HashMap<String,String> parts,SentenceToken data) {
				String str=parts.get(data.content);
				if(str==null) throw new ParseException("Failed to parse: can't find \""+data+"\" in parts.");
				value=new ShaniString(str).split();
			}
			
			@Override
			protected void process(SentenceResoult resoult, ShaniString[] str, int strIndex) {
				var sr=new SentenceResoult[value.length];
				
				for(int i=0;i<value.length;i++) {
					var ret=ShaniString.getMatchingIndex(str, strIndex, value[i]);
					if(ret.cost<Config.wordCompareTreshold) {
						processNext((sr[i]=resoult.makeCopy()),str,ret.endIndex);						//TODO not check same sentenceIndex and String index multiple times. Store it somewhere.
						sr[i].cost+=ret.cost;
					}
				}
				
				int minIndex=-1;
				short minCost=Short.MAX_VALUE;
				for(int i=0;i<value.length;i++) {
					if(sr[i]!=null&&sr[i].cost<Config.sentenseCompareTreshold) {
						short tmpCost=sr[i].getCombinedCost();
						if(tmpCost<minCost) {
							minCost=tmpCost;
							minIndex=i;
						}
					}
				}
				
				if(minIndex==-1) {
					resoult.cost+=Config.wordInsertionCost;
					if(resoult.cost<Config.sentenseCompareTreshold)
						processNext(resoult, str, strIndex);
					return;
				}
				
				
				resoult.set(sr[minIndex]);
			}
			
			@Override
			public String toString() {
				StringBuffer ret=new StringBuffer();			//Method used only during debugging so assembling answer on each invocation is fine.
				
				for(var ss:value) {
					for(var s:ss) {
						ret.append(" ").append(s.toString());
					}
					ret.append("*");
				}
				
				ret.deleteCharAt(0);
				ret.deleteCharAt(ret.length()-1);
				
				ret.insert(0, "ShaniString:");
				
				return ret.toString();
			}
		}
		protected class RegexElement extends SentenceElement{
			private Pattern pattern;
			private String returnKey;
			
			protected RegexElement(HashMap<String,String> parts,SentenceToken data) {
				String str=parts.get(data.content);
				if(str==null) throw new ParseException("Failed to parse: can't find \""+data+"\" in parts.");
				pattern=Pattern.compile(str);
				
				returnKey=data.content;
			}
			
			@Override
			protected void process(SentenceResoult resoult, ShaniString[] str, int strIndex) {
				int tempStrIndex;
				short deleteCost=0;
				for(tempStrIndex=strIndex;tempStrIndex<str.length;tempStrIndex++) {
					if(str[tempStrIndex].isEquals(pattern)) {
						resoult.cost+=deleteCost;
						resoult.data.put(returnKey, str[tempStrIndex].toString());
						processNext(resoult, str, tempStrIndex+1);
						return;
					}
					
					deleteCost+=Config.wordDeletionCost;
					if(deleteCost>Config.wordInsertionCost)
						break;
				}
				
				resoult.cost+=Config.wordInsertionCost;
				processNext(resoult, str, strIndex);
			}
			
			@Override
			public String toString() {
				return "Regex:"+returnKey+":"+pattern.pattern();
			}
		}
	}
	
	/**Object containing result of matching ShaniString by SentenceMatcher.*/
	public static class SentenceResoult{
		/**Map containing words appeared in return node position.*/
		public final HashMap<String,String> data=new HashMap<String,String>();
		protected short cost;
		protected short importanceBias;
		/**Name of matched sentence. Specified by name attribute in representing xml node.
		 */
		public final String name;
		
		protected SentenceResoult() {name=null;}
		protected SentenceResoult(String name) {
			this.name=name;
		}
		
		/**Performs deep copy of this object. It not make copy of underlying String, but there are immutable so no sense to doing it.
		 * @return Deep copy of this object.
		 */
		public SentenceResoult makeCopy() {
			var copy=new SentenceResoult(name);
			copy.data.putAll(data);
			copy.cost=cost;
			copy.importanceBias=importanceBias;
			
			return copy;
		}
		protected void set(SentenceResoult sr) {
			assert name==null?sr.name==null:name.equals(sr.name):"Propably trying to set values from very diffrend element";
			cost=sr.cost;
			importanceBias=sr.importanceBias;
			data.clear();
			data.putAll(sr.data);
		}
		protected void add(SentenceResoult sr) {
			assert name==null?sr.name==null:name.equals(sr.name):"Propably trying to set values from very diffrend element";
			this.cost+=sr.cost;
			this.importanceBias+=sr.importanceBias;
			data.putAll(sr.data);
		}
		
		/**Return's {@link SentenceResoult#data data} Map.
		 * @return {@link SentenceResoult#data data} Map.
		 */
		public HashMap<String,String> getData(){
			return data;
		}
		/**Return's {@link SentenceResoult#name} of matched sentence result.
		 * @return {@link SentenceResoult#name} of matched sentence resoult.
		 */
		public String getName() {
			return name;
		}
		public short getCost() {
			return cost;
		}
		public short getImportanceBias() {
			return importanceBias;
		}
		/**Get cost combined with importance bias*/
		public short getCombinedCost() {
			return (short)(cost-importanceBias*Config.importanceBiasMultiplier);
		}
		
		@Override
		public String toString() {
			return "SentenceResult: "+name+':'+cost+':'+importanceBias+" "+data;
		}
	}
	protected class ParseException extends RuntimeException{
		private static final long serialVersionUID = -7564692708180202338L;

		ParseException(String message){
			super(message);
		}
	}
	
	
	public static void main(String[]args) throws SAXException, IOException, ParserConfigurationException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("test.xml"));
		doc.getDocumentElement().normalize();
		
		var matcher=new SentenceMatcher(((Element)doc.getElementsByTagName("sentence").item(0)).getElementsByTagName("calculate").item(0));
		var resoults=matcher.process(new ShaniString("dodaj 2 do 2"));
		for(var res:resoults) {
			System.out.println(res);
		}
		
	}
	
	public static void tokenizerTest() {
		List<SentenceToken> tokens=new Tokenizer().tokenize("$ShaniString *(^another [$any|($order ?token) ^group])");
		
		if(tokens!=null)
			printTokens(tokens,0);
		System.out.println("\n---End---");
	}
	public static void printTokens(List<SentenceToken> tokens, int depth) {
		String depthMarker="";
		for(int i=0;i<depth;i++)depthMarker+="\t";
		
		for(var token:tokens) {
			if(token.content!=null)
				System.out.println(depthMarker+token.type+": \""+token.content+"\"");
			else
				System.out.println(depthMarker+token.type+":");
			
			if(token.subTokens!=null)
				printTokens(token.subTokens, depth+1);
		}
	}
}