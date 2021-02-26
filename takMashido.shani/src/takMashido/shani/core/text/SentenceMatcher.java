package takMashido.shani.core.text;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import takMashido.shani.Config;
import takMashido.shani.Engine;
import takMashido.shani.core.text.SentenceMatcher.Tokenizer.SentenceToken;
import takMashido.shani.core.text.SentenceMatcher.Tokenizer.SentenceToken.Type;
import takMashido.shani.libraries.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**More powerful matching engine than ShaniMatcher.
 * <pre>
 * Matches whole sentence and get data out of it.
 * 
 * Data for it are loaded from xml Node.
 * 
 * Subnodes named "sentence" are sentence templates.
 * You can specify "name" attribute in it for further recognizing which sentence given best match.
 * 
 * It's text content contain words representing Sentence Elements with additional special characters at from of each word to choose their type. Following word is name of sentence element.
 * $ means ShaniString. It'll perform normal ShaniStrig Matching with this element.
 * ? means return value. It'll store corresponding value from processed String in HashMap under given keyword.
 * ^ means regex. It'll try to match content with regex, works only for single words, additionally puts it's matched word into dataReturn HashMap.
 * * means optional match. Matching is with and without that element and better match is chosen.  
 * | is or element. Uses it's neighbor elements and chooses which one use during matching.
 * You can also group elements with (). Optional match character * apply to whole group.
 * [] is AnyOrder group. Elements inside it can be applied in any order by matcher.
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
 * Sentence "good morning" is not matched.
 * Sentence "good night bar" will not be matched(depends on values in Config file).
 * 
 * {@code<template>*$bar</template>}
 * Matcher with this setup tries to match ShaniString stored under "bar" but it can also match empty sentence.
 * 
 * {@code<template>$foo *(^regex ?return)</template>}
 * Assume ShaniString stored under foo is already matched. Next it tries to match sentence in brackets. If fail try to match with this part skipped.
 * 
 * {@code<template>$foo|^bar ?data</template>}
 * Match either foo or bar and next match data.
 * 
 * Sentences are evaluated in order in which they appear inside xml node possibly causing invoking action marked by first one if compare costs and importance bias are equal.
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

	/**Works like {@link #process(String)} but returns only best match.*/
	public SentenceResult processBest(String string) {
		return processBest(new ShaniString(string,false));
	}
	/**Works like {@link #process(ShaniString)} but returns only best match.*/
	public SentenceResult processBest(ShaniString string) {
		return getBestMatch(process(string));
	}
	
	/**Process given String.
	 * @param string String in which engine search for matches.
	 * @return Array of {@code Sentenceresult} object representing results of successful matching.
	 */
	public SentenceResult[] process(String string) {
		return process(new ShaniString(string,false));
	}
	/**Process given ShaniString.
	 * @param string {@code ShaniString} in which engine searches for matches.
	 * @return Array of {@Link #SentenceResult} object containing all matched sentences.
	 */
	public synchronized SentenceResult[] process(ShaniString string) {
		var str=string.split(false);
		
		ArrayList<SentenceResult> Return=new ArrayList<>();
		for(Sentence sen:sentences) {
			SentenceResult sr=sen.process(str);
			if(sr!=null&&sr.cost<Config.sentenseCompareTreshold)Return.add(sr);
		}
		
		return Return.toArray(new SentenceResult[Return.size()]);
	}
	
	/**Get best {@link SentenceResult} from given set, taking into account comparison cost and importance bias.
	 * @param results Set of result to choose from.
	 * @return {@link SentenceResult} which is most accurate from ones in the set. 
	 */
	public static SentenceResult getBestMatch(SentenceResult[] results) {
		SentenceResult ret=null;
		short minCost=Short.MAX_VALUE;
		
		for(var res:results) {
			if(res!=null&&res.cost<Config.sentenseCompareTreshold) {
				short tmpCost=res.getCombinedCost();
				if(tmpCost<minCost) {
					minCost=tmpCost;
					ret=res;
				}
			}
		}
		
		return ret;
	}
	/**Get best {@link SentenceResult} from given set, taking into account comparison cost and importance bias.
	 * @param results Set of result to choose from.
	 * @return {@link SentenceResult} which is most accurate from ones in the set. 
	 */
	public static SentenceResult getBestMatch(List<SentenceResult> results) {
		SentenceResult ret=null;
		short minCost=Short.MAX_VALUE;
		
		for(var res:results) {
			if(res!=null&&res.cost<Config.sentenseCompareTreshold) {
				short tmpCost=res.getCombinedCost();
				if(tmpCost<minCost) {
					minCost=tmpCost;
					ret=res;
				}
			}
		}
		
		return ret;
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
				AnyOrderGroup(']');		//[]
				
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
		 * @param tokens of tokens to filter.
		 * @return merged tokens tree ready being ready sentence template.
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
	protected static class Sentence{
		private SentenceElement root;
		private String sentenceName;
		
		protected Sentence(HashMap<String,String> parts,List<SentenceToken> tokens,String name) {
			root=decodeGroup(parts, tokens).first;
			sentenceName=name;
		}
		/**Handles creation of elements chain from Group token.
		 * @param parts Sentence parts, data used by some elements. 
		 * @param tokens Tokens inside processed group.
		 * @return Pair containing entry to created Elements graph as {@link Pair#first}, and last, exiting element as {@link Pair#second}.
		 */
		protected static Pair<SentenceElement,SentenceElement> decodeGroup(HashMap<String,String> parts,List<SentenceToken> tokens) {
			SentenceElement ret=null;
			
			Pair<SentenceElement,SentenceElement> element=null;
			SentenceElement current=null;
			for(SentenceToken token:tokens) {
				if(token.type==Type.Group)
					element=decodeGroup(parts, token.subTokens);
				else
					element=createElement(parts, token);
				
				
				if(current==null)
					ret=element.first;
				else
					current.linkElement(element.first);
				current=element.second;
			}
			
			return new Pair<SentenceElement,SentenceElement>(ret,current);
		}
		/**Handles creation of Element based on given token.
		 * @param parts Sentence parts, data used by some elements. 
		 * @param token Token representing element to create.
		 * @return Pair containing entry to created Elements graph as {@link Pair#first}, and last, exiting element as {@link Pair#second}.
		 */
		protected static Pair<SentenceElement,SentenceElement> createElement(HashMap<String,String> parts, SentenceToken token) {
			switch (token.type) {
			case ShaniString:
				return new Pair<>(new ShaniStringElement(parts, token),true);
			case DataReturn:
				return new Pair<>(new DataReturnElement(parts, token),true);
			case Regex:
				return new Pair<>(new RegexElement(parts, token),true);
			case Optional:
				return new Pair<>(new OptionalElement(parts, token),true);
			case Group:
				return decodeGroup(parts,token.subTokens);
			case Or:
				return new Pair<>(new OrElement(parts, token),true);
			case AnyOrderGroup:
				return new Pair<>(new AnyOrderGroup(parts, token),true);
			default:
				assert false:"Unknow token type encountered";
			}
			
			return null;
		}
		
		protected SentenceResult process(ShaniString[][] str){
			var Return=new SentenceResult[str.length];
			
			for(int i=0;i<str.length;i++)
				root.process((Return[i]=new SentenceResult(sentenceName)),str[i],0);
			
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
		
		protected static abstract class SentenceElement{
			protected SentenceElement nextElement;
			
			@SuppressWarnings("hiding")
			protected void linkElement(SentenceElement nextElement) {
				this.nextElement=nextElement;
			}
			
			/**Individual processing handling*/
			protected abstract void process(SentenceResult result, ShaniString[] str, int strIndex);
			/**Process next element and handles end of matched string or end of sentence pattern.*/
			protected void processNext(SentenceResult result, ShaniString[] str, int strIndex) {
				if(nextElement!=null) {
					if(strIndex<str.length) {
						nextElement.process(result, str, strIndex);
					} else {
						nextElement.getInsertionCost(result);
					}
				} else {
					if(strIndex<str.length)
						result.cost+=Config.wordDeletionCost*(str.length-strIndex);
				}
			}
			
			protected void getInsertionCost(SentenceResult result) {
				result.cost+=Config.wordInsertionCost;
				if(nextElement!=null)
					nextElement.getInsertionCost(result);
			}
		}
		protected static class OptionalElement extends SentenceElement{
			private SentenceElement optionalElement;
			private SentenceElement lastOptionalElement;					//Last element being optional, used to link it to the following element
			
			protected OptionalElement(HashMap<String,String> parts,SentenceToken data) {
				SentenceToken optionalToken=data.subTokens.get(0);
				
				var elems=createElement(parts, optionalToken);
				optionalElement=elems.first;
				lastOptionalElement=elems.second;
			}
			
			@SuppressWarnings("hiding")
			@Override
			protected void linkElement(SentenceElement nextElement) {
				super.linkElement(nextElement);
				
				lastOptionalElement.linkElement(nextElement);
			}
			
			@Override
			protected void process(SentenceResult result, ShaniString[] str, int strIndex) {
				SentenceResult skippedresult=result.makeCopy();
				if(nextElement!=null)
					nextElement.process(skippedresult, str, strIndex);
				
				optionalElement.process(result, str, strIndex);
				
				result.setIfBetter(skippedresult);
			}
			
			@Override
			protected void getInsertionCost(SentenceResult result) {
				//result.cost+=0;
				if(lastOptionalElement.nextElement!=null)
					lastOptionalElement.getInsertionCost(result);
			}
		}
		protected static class DataReturnElement extends SentenceElement{
			private String returnKey;
			
			protected DataReturnElement(HashMap<String,String> parts,SentenceToken data) {
				returnKey=data.content;
			}
			
			@Override
			protected void process(SentenceResult result, ShaniString[] str, int strIndex) {
				assert !(nextElement instanceof DataReturnElement):"Two data return elements shouldn't apper next to each other.";
				
				SentenceResult retresult=null;
				int minIndex=-1;
				if(nextElement==null) {			//last element of sentence
					minIndex=str.length;
					retresult=result;
				} else {
					short minCost=Short.MAX_VALUE;
					for(int i=strIndex+1;i<=str.length;i++) {
						var tempresult=result.makeCopy();
						processNext(tempresult,str,i);
						
						if(tempresult.cost>=Config.sentenseCompareTreshold)continue;
						short tempCost=tempresult.getCombinedCost();
						
						if(tempCost<minCost) {
							minCost=tempCost;
							minIndex=i;
							retresult=tempresult;
						}
					}
				}
				
				if(minIndex>strIndex) {
					StringBuffer strBuf=new StringBuffer();
					strBuf.append(str[strIndex]);
					for(int i=strIndex+1;i<minIndex;i++) {
						strBuf.append(' ').append(str[i]);
					}
					result.set(retresult);
					result.data.put(returnKey, strBuf.toString());
					result.importanceBias+=Config.sentenceMatcherWordReturnImportanceBias*(minIndex-strIndex);
				} else
					result.cost+=Config.sentenseCompareTreshold;				//Nothing matched, technically should be wordInsertionCost but dataReturn element is for gathering data, making it able to not gather it have no sense and every data gathered by it will have to be checked for existence later  
			}
			
			@Override
			protected void getInsertionCost(SentenceResult result) {
				result.cost+=Config.sentenseCompareTreshold;
			}
			
			@Override
			public String toString() {
				return "DataReturn:"+returnKey;
			}
		}
		protected static class ShaniStringElement extends SentenceElement{
			private ShaniString[][] value;
			private short insertionCost;
			
			protected ShaniStringElement(HashMap<String,String> parts,SentenceToken data) {
				String str=parts.get(data.content);
				if(str==null) throw new ParseException("Failed to parse: can't find \""+data.content+"\" in parts.");
				value=new ShaniString(str).split();
				
				insertionCost=Short.MAX_VALUE;
				for(var sstr:value) {
					if(sstr.length<insertionCost)
						insertionCost=(short)sstr.length;
				}
				
				if(insertionCost==Short.MAX_VALUE) {
					insertionCost=0;
				} else
					insertionCost*=Config.wordInsertionCost;
			}
			
			@Override
			protected void process(SentenceResult result, ShaniString[] str, int strIndex) {
				var sr=new SentenceResult[value.length];
				
				for(int i=0;i<value.length;i++) {
					var ret=ShaniString.getMatchingIndex(str, strIndex, value[i]);
					if(ret.cost<Config.wordCompareTreshold) {
						sr[i]=result.makeCopy();
						sr[i].cost+=ret.cost;
						processNext(sr[i],str,ret.endIndex);						//TODO do not check same sentenceIndex and String index multiple times. Store it somewhere.
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
					result.cost+=Config.wordInsertionCost;
					if(result.cost<Config.sentenseCompareTreshold)
						processNext(result, str, strIndex);
					return;
				}
				
				result.set(sr[minIndex]);
			}
			
			@Override
			protected void getInsertionCost(SentenceResult result) {
				result.cost+=insertionCost;
				if(nextElement!=null)
					nextElement.getInsertionCost(result);
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
		protected static class RegexElement extends SentenceElement{
			private Pattern pattern;
			private String returnKey;
			
			protected RegexElement(HashMap<String,String> parts,SentenceToken data) {
				String str=parts.get(data.content);
				if(str==null) throw new ParseException("Failed to parse: can't find \""+data+"\" in parts.");
				pattern=Pattern.compile(str);
				
				returnKey=data.content;
			}
			
			@Override
			protected void process(SentenceResult result, ShaniString[] str, int strIndex) {
				int tempStrIndex;
				short deleteCost=0;
				for(tempStrIndex=strIndex;tempStrIndex<str.length;tempStrIndex++) {
					if(str[tempStrIndex].isEquals(pattern)) {
						result.cost+=deleteCost;
						result.importanceBias+=Config.sentenceMatcherRegexImportanceBias;
						result.data.put(returnKey, str[tempStrIndex].toString());
						processNext(result, str, tempStrIndex+1);
						return;
					}
					
					deleteCost+=Config.wordDeletionCost;
					if(deleteCost>Config.wordInsertionCost)
						break;
				}
				
				result.cost+=Config.wordInsertionCost;
				processNext(result, str, strIndex);
			}
			
			@Override
			public String toString() {
				return "Regex:"+returnKey+":"+pattern.pattern();
			}
		}
		protected static class OrElement extends SentenceElement{
			private SentenceElement firstChoice;
			private SentenceElement firstChoiceEnd;
			
			private SentenceElement secondChoice;
			private SentenceElement secondChoiceEnd;
			
			public OrElement(HashMap<String,String> parts,SentenceToken data) {
				var pair=createElement(parts, data.subTokens.get(0));
				firstChoice=pair.first;
				firstChoiceEnd=pair.second;
				
				pair=createElement(parts, data.subTokens.get(1));
				secondChoice=pair.first;
				secondChoiceEnd=pair.second;
			}
			
			@SuppressWarnings("hiding")
			@Override
			protected void linkElement(SentenceElement nextElement) {
				firstChoiceEnd.linkElement(nextElement);
				secondChoiceEnd.linkElement(nextElement);
			}
			
			@Override
			protected void process(SentenceResult result, ShaniString[] str, int strIndex) {
				if(strIndex>=str.length) {
					result.cost+=Config.wordDeletionCost;
					return;
				}
				
				var secondResult=result.makeCopy();
				secondChoice.process(secondResult, str, strIndex);
				
				firstChoice.process(result, str, strIndex);
				
				result.setIfBetter(secondResult);
			}
			
			@Override
			protected void getInsertionCost(SentenceResult result) {
				var secondResult=result.makeCopy();
				secondChoice.getInsertionCost(secondResult);
				
				firstChoice.getInsertionCost(result);
				
				result.setIfBetter(secondResult);
			}
		}
		protected static class AnyOrderGroup extends SentenceElement{
			private SentenceElement[] optionalElements;
			
			private boolean[] usedElements;
			private int treeLevel=0;								//Amount of currently used elements/how deep in processing tree process is
			
//			private ArrayList<SentenceResult> endResults=new ArrayList<SentenceMatcher.SentenceResult>();
			
			protected AnyOrderGroup(HashMap<String,String> parts, SentenceToken token) {
				optionalElements=new SentenceElement[token.subTokens.size()];
				
				for(int i=0;i<optionalElements.length;i++) {
					var dat=createElement(parts, token.subTokens.get(i));
					
					optionalElements[i]=dat.first;
					dat.second.linkElement(this);
				}
				
				usedElements=new boolean[optionalElements.length];
			}
			
			@Override
			protected void process(SentenceResult result, ShaniString[] str, int strIndex) {
				if(result.cost>Config.sentenseCompareTreshold)
					return;
				
				if(treeLevel<optionalElements.length) {
					SentenceResult[] sr=new SentenceResult[optionalElements.length];
					SentenceResult[] srInsertion=null;
					if(Config.wordInsertionCost<Config.sentenseCompareTreshold)
						srInsertion=new SentenceResult[optionalElements.length];
					
					treeLevel++; 
					for(int i=0;i<optionalElements.length;i++) {
						if(!usedElements[i]) {
							usedElements[i]=true;
							
							if(Config.wordInsertionCost<Config.sentenseCompareTreshold) {
								srInsertion[i]=result.makeCopy();
								srInsertion[i].cost+=Config.wordInsertionCost;
								process(srInsertion[i], str, strIndex);
							}
							optionalElements[i].process((sr[i]=result.makeCopy()), str, strIndex);
							
							usedElements[i]=false;
						}
					}
					treeLevel--;
					
					SentenceResult best=getBestMatch(sr);
					if(Config.wordInsertionCost<Config.sentenseCompareTreshold) {
						SentenceResult best2=getBestMatch(srInsertion);
						if(best2!=null&&best2.cost<Config.sentenseCompareTreshold&&best2.getCombinedCost()<best.getCombinedCost())
							best=best2;
					}
					
					if(best!=null)
						result.set(best);
					else
						getInsertionCost(result);
				} else {
					processNext(result, str, strIndex);
				}
			}
			
			private boolean insertionInProgress=false;				//Guard to invoke insertion cost calculating only once
			@Override
			protected void getInsertionCost(SentenceResult result) {
				if(treeLevel!=optionalElements.length) {
					if(insertionInProgress)
						return;
					insertionInProgress=true;
					try {
						for(int i=0;i<usedElements.length;i++) {
							if(!usedElements[i]) {
								optionalElements[i].getInsertionCost(result);
								if(result.getCost()>Config.sentenseCompareTreshold) {
									return;
								}
							}
						}
					} finally {
						insertionInProgress=false;
					}
				}
				
				if(nextElement!=null)
					nextElement.getInsertionCost(result);
			}
		}
	}
	
	/**Object containing result of matching ShaniString by SentenceMatcher.*/
	public static class SentenceResult{
		/**Map containing words mached into sentence elements.*/
		public final HashMap<String,String> data=new HashMap<String,String>();
		protected short cost;
		protected short importanceBias;
		/**Name of matched sentence. Specified by name attribute in representing xml node.
		 */
		public final String name;
		
		protected SentenceResult() {name=null;}
		protected SentenceResult(String name) {
			this.name=name;
		}
		
		/**Performs deep copy of this object. It not make copy of underlying String, but there are immutable so no sense to doing it.
		 * @return Deep copy of this object.
		 */
		public SentenceResult makeCopy() {
			var copy=new SentenceResult(name);
			copy.data.putAll(data);
			copy.cost=cost;
			copy.importanceBias=importanceBias;
			
			return copy;
		}
		protected void set(SentenceResult sr) {
			assert name==null?sr.name==null:name.equals(sr.name):"Propably trying to set values from very diffrend element";
			
			if(sr==this)return;
			
			cost=sr.cost;
			importanceBias=sr.importanceBias;
			data.clear();
			data.putAll(sr.data);
		}
		protected void setIfBetter(SentenceResult sr) {
			if(sr.cost>=Config.sentenseCompareTreshold) {
				if(cost<Config.sentenseCompareTreshold) {
					return;
				}
			} else if(cost>=Config.sentenseCompareTreshold) {
				set(sr);
				return;
			}
			if(sr.getCombinedCost()<getCombinedCost()) {
				set(sr);
			}
		}
		
		protected void add(SentenceResult sr) {
			assert name==null?sr.name==null:name.equals(sr.name):"Propably trying to set values from very diffrend element";
			this.cost+=sr.cost;
			this.importanceBias+=sr.importanceBias;
			data.putAll(sr.data);
		}
		
		/**Return's {@link SentenceResult#data data} Map.
		 * @return {@link SentenceResult#data data} Map.
		 */
		public HashMap<String,String> getData(){
			return data;
		}
		/**Return's {@link SentenceResult#name} of matched sentence result.
		 * @return {@link SentenceResult#name} of matched sentence result.
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
	protected static class ParseException extends RuntimeException{
		private static final long serialVersionUID = -7564692708180202338L;

		ParseException(String message){
			super(message);
		}
	}

	/**Debug method for printing SentenceTokens tree.
	 * @param tokens Tree of SentenceTokens.
	 * @param depth Actual depth in tree for indentation. Should be 0 during call from outside code.
	 */
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

	/*public static void main(String[]args) throws SAXException, IOException, ParserConfigurationException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("test.xml"));
		doc.getDocumentElement().normalize();
		
		var matcher=new SentenceMatcher(DOMWalker.walk(doc, "tests/sentenceMatcher/test"));
		
//		var reses=matcher.process("witam pana gneera�a pu�kownika");
		var reses=matcher.process("must mork hog");
		System.out.println("\nfinal resoults:");
		for(var res:reses)
			System.out.println(res);
		System.out.println("End");
	}*/
}