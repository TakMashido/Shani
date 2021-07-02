package takMashido.shani.core.text;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import takMashido.shani.core.Config;
import takMashido.shani.core.IntendBase;
import takMashido.shani.core.Storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;

/**Object used to represent string values in SHANI.
 * 
 * It is optimized for fuzzy string matching and have some methods allowing it.
 * Also can contain one or more String for printing or used as keywords to search in other ShaniString objects.
 * 
 * @author TakMashido
 */
public class ShaniString implements IntendBase {
	//<StaticServiceFields><StaticServiceFields><StaticServiceFields><StaticServiceFields><StaticServiceFields>
	private static final Random random=new Random();
	private static final Pattern stringDivider=Pattern.compile("(?:\\*)+");
	private static byte[][] lookUpTable;				//~140KB of memory
	
	/**Determines method used to change String into ShaniString.
	 * raw -> do not perform any operations.
	 * split -> split on '*' occurrences.
	 * split_escape -> split on '*' occurrences, but allow to escape it with '\' to omit some. Use '\\' to insert '\'.
	 */
	public enum ParseMode{
		raw, split, splitEscape;
	};
	
	//<ShaniStringObjectFields><ShaniStringObjectFields><ShaniStringObjectFields><ShaniStringObjectFields>
	protected String[] value;
	protected String[][] words;
	protected char[][][] stemmedValue=null;											//[a][b][c]: a->wordsSet, b->word, c->letter
	/**Node containing this ShaniString data.*/
	private Node origin;

	//<InitializationCode><InitializationCode><InitializationCode><InitializationCode><InitializationCode>
	public static void staticInit(Element e){
		createLookUpTable(e);
	}
	private static void createLookUpTable(Element e){
		NodeList nodes=Storage.getNodes(e, "nationalSimilar.tag");

		char[][] nationalLettersTable=new char[nodes.getLength()][2];

		for(int i=0;i<nationalLettersTable.length;i++) {
			Element elem=(Element)nodes.item(i);
			nationalLettersTable[i][0]=elem.getAttribute("let").charAt(0);
			nationalLettersTable[i][1]=elem.getAttribute("rep").charAt(0);
		}
		
		char[][] qwertyTable=new char[][] {
			{'1','2','3','4','5','6','7','8','9','0','-'},
			{'q','w','e','r','t','y','u','i','o','p'},
			{'a','s','d','f','g','h','j','k','l'},
			{'z','x','c','v','b','n','m'}
		};
		
		/*char[][] shiftTable=new char[][] {
			{'1','!'},
			{'2','@'},
			{'3','#'},
			{'4','$'},
			{'5','%'},
			{'6','^'},
			{'7','&'},
			{'8','*'},
			{'9','('},
			{'0',')'}
		};*/
		
		char maxID=0;
		for(int i=0;i<nationalLettersTable.length;i++) {
			for(int j=0;j<nationalLettersTable[i].length;j++) {
				if(maxID<nationalLettersTable[i][j])maxID=nationalLettersTable[i][j];
			}
		}
		for(int i=0;i<qwertyTable.length;i++) {
			for(int j=0;j<qwertyTable[i].length;j++) {
				if(maxID<qwertyTable[i][j])maxID=qwertyTable[i][j];
			}
		}
		/*for(int i=0;i<shiftTable.length;i++) {
			for(int j=0;j<shiftTable[i].length;j++) {
				if(maxID<shiftTable[i][j])maxID=shiftTable[i][j];
			}
		}*/
		
		lookUpTable=new byte[maxID+1][maxID+1];
		for(int i=0;i<lookUpTable.length;i++) {
			Arrays.fill(lookUpTable[i],Config.differentCharacterCost);
		}
		
		for(int i=0;i<qwertyTable.length;i++) {				//TODO it use keys distance without looking for shift of rows. Fix if;
			for(int j=0;j<qwertyTable[i].length;j++) {
				for(int k=-1;k<2;k++) {
					int index=i+k;
					if(index<0||index>=qwertyTable.length)continue;
					for(int l=-1;l<2;l++) {
						int index2=j+l;
						if(index2<0||index2>=qwertyTable[index].length)continue;
						
						lookUpTable[qwertyTable[i][j]][qwertyTable[index][index2]]=Config.qwertyNeighbourCost;
						lookUpTable[qwertyTable[index][index2]][qwertyTable[i][j]]=Config.qwertyNeighbourCost;
					}
				}
			}
		}
		for(int i=0;i<nationalLettersTable.length;i++) {
			for(int j=0;j<nationalLettersTable[i].length;j++) {
				for(int k=j+1;k<nationalLettersTable[i].length;k++) {
					lookUpTable[nationalLettersTable[i][j]][nationalLettersTable[i][k]]=Config.nationalSimilarityCost;
					lookUpTable[nationalLettersTable[i][k]][nationalLettersTable[i][j]]=Config.nationalSimilarityCost;
				}
			}
		}
		for(int i=0;i<lookUpTable.length;i++) {
			lookUpTable[i][i]=0;
		}
	}
	
	//<RestOfCode><RestOfCode><RestOfCode><RestOfCode><RestOfCode><RestOfCode><RestOfCode><RestOfCode>
	/**Creates new empty ShaniString*/
	public ShaniString() {
		this("");
	}
	/**Creates new ShaniString.
	 * @param origin String containing ShaniString data. Will be cut on '*' occurrences.
	 */
	public ShaniString(String origin) {
		this(origin,true);
	}
	/**Creates new ShaniString.
	 * @param origin String containing ShaniString data.
	 * @param cut If cut based on '*' location.
	 * @deprecated Use {@link #ShaniString(ParseMode,String...)}
	 */
	@Deprecated
	public ShaniString(String origin,boolean cut) {
		parseString(cut?ParseMode.split:ParseMode.raw,origin);
	}
	/**Creates new ShaniString.
	 * @param origin Strings containing data. All of them will be cut on '*' occurrences.
	 */
	public ShaniString(String... origin) {
		this(true,origin);
	}
	/**Creates new ShaniString.
	 * @param cut If cut based on '*' location.
	 * @param origin Strings containing ShaniString data. Each one is treated as other version of represented sentence.
	 * @deprecated Use {@link #ShaniString(ParseMode,String...)}
	 */
	@Deprecated
	public ShaniString(boolean cut,String... origin) {
		parseString(cut?ParseMode.split:ParseMode.raw,origin);
	}
	/**Create new ShaniString using given String's as parts and parsing them using provided parse mode.
	 * @param mode Mode used to parse strings.
	 * @param parts String's to use as parts.
	 */
	public ShaniString(ParseMode mode, String... parts){
		parseString(mode,parts);
	}
	/**Load ShaniString from xml node. Any change done to this object will also be pushed to given node.
	 * @param origin XML Node storing ShaniString data.
	 */
	public ShaniString(Node origin) {
		if(origin==null) {
			assert false:"Origin node can't be null. Probably node is missing in shani main file.";
			value=new String[] {"MISSING_SHANI_STRING"};
		}
		
		if(origin.getNodeType()!=Node.ELEMENT_NODE){
			assert false:"Origin node has to be XML Elements.";
			value=new String[] {"MISSING_SHANI_STRING"};
		}
		
		Element el=(Element)origin;
		
		String raw=el.getAttribute("val");
		if(raw.isEmpty())
			raw=el.getTextContent();
		
		String modeStr=el.getAttribute("mode");
		ParseMode pMode;
		if(!modeStr.isEmpty())
			pMode=ParseMode.valueOf(modeStr);
		else pMode=ParseMode.split;
		
		parseString(pMode,raw);
		
		this.origin=origin;
	}
	/**Load ShaniString from xml node. Any change done to this object will also be pushed to given node.
	 * Also put origin data to created ShaniString and add it to Node.
	 * @param originNode XML Node storing ShaniString data.
	 * @param origin New data to push into created ShaniString.
	 */
	public ShaniString(Node originNode, String origin) {
		this(originNode);
		add(origin);
	}
	/**Parse given string array according to passed pares mode and append it to the value arrays.
	 * @param mode What processing do on passed String's.
	 * @param parts String's to add, each one is treated as alternative value for matching/printing purposes.
	 */
	private void parseString(ParseMode mode, String... parts){
		ArrayList<String> valueRet=new ArrayList<>();
		
		for(String str:parts){
			if(str=="")
				continue;
			
			switch(mode){
			case raw -> valueRet.add(str);
			case split -> Collections.addAll(valueRet, stringDivider.split(str));
			case splitEscape -> {
				StringBuilder builder=new StringBuilder();
				int lastPushedIndex=0;
				for(int i=0; i<str.length(); i++){
					if(str.charAt(i)=='\\'){
						i++;
						
						if(i>=str.length()){                //Assert false, and ignore.
							assert false: "ShaniString should end with '\\' character.";
							
							builder.append(str, lastPushedIndex, i);
							lastPushedIndex=i;
							break;
						}
						
						switch(str.charAt(i)){
						case '\\' -> {
							builder.append(str, lastPushedIndex, i-1);
							lastPushedIndex=i;
						}
						case '*' -> {
							builder.append(str, lastPushedIndex, i-1);
							builder.append('*');
							lastPushedIndex=i+1;
						}
						default -> {						//Assert false, and ignore.
							assert false: "After '\\' character can only be preset '\\' or '*' character.";
							builder.append(str, lastPushedIndex, i);
							lastPushedIndex=i;
						}}
					} else if(str.charAt(i)=='*'){
						builder.append(str,lastPushedIndex,i);
						lastPushedIndex=i+1;
						
						valueRet.add(builder.toString());
						builder.delete(0,builder.length());
					}
				}
				
				builder.append(str,lastPushedIndex,str.length());
				valueRet.add(builder.toString());
			}
			default -> {
				assert false: "New parse mode showed up but it's not handled properly during creation. Fix this.";
				parseString(ParseMode.split, parts);
				return;
			}}
		}
		
		if(valueRet.isEmpty())
			return;
		
		if(value==null){
			value=valueRet.toArray(new String[valueRet.size()]);
		} else {
			String[] wordArray=valueRet.toArray(new String[valueRet.size()]);
			
			String[] newValue=new String[value.length+wordArray.length];
			System.arraycopy(value, 0, newValue, 0, value.length);
			System.arraycopy(wordArray, 0, newValue, value.length, wordArray.length);
			value=newValue;
			
			saveData();
			
			if(stemmedValue!=null) {
				char[][][] newVal=new char[newValue.length][][];
				System.arraycopy(stemmedValue, 0, newValue, 0, stemmedValue.length);
				for(int i=0;i<wordArray.length;i++) {
					newVal[stemmedValue.length+1]=stem(wordArray[i]);
				}
				stemmedValue=newVal;
				
				if(words!=null) {
					String[][] newWords=new String[words.length+1][];
					System.arraycopy(words, 0, newWords, 0, words.length);
					newWords[words.length]=new String[stemmedValue[words.length].length];
					for(int i=0;i<newWords[words.length].length;i++)
						newWords[words.length][i]=new String(stemmedValue[words.length][i]);
					words=newWords;
				}
			}
		}
	}
	
	/**Splits ShaniString to words group and each to single words.
	 * @return Array[][] of words. Each entry is single word.
	 */
	public ShaniString[][] split() {
		return split(true);
	}
	/**Splits ShaniString to words group and each to single words.
	 * @param cut If cut given ShaniString on each occurrence of '*'
	 * @return Array[][] of words. Each entry is single word.
	 */
	public ShaniString[][] split(boolean cut){										//For further optimization can store once splitted val somewhere. It needs changes in ShaniString to make immutable. Any change on it have to be applied on new object like in original string.
		var Return=new ShaniString[value.length][];
		
		for(int i=0;i<value.length;i++) {
			var ls=new ArrayList<String>();
			@SuppressWarnings("resource")
			Scanner str=new Scanner(value[i]);
			while(str.hasNext()) ls.add(str.next());
			
			String[] stra=ls.toArray(new String[0]);
			Return[i]=new ShaniString[stra.length];
			for(int j=0;j<stra.length;j++) {
				Return[i][j]=new ShaniString(stra[j],cut);
			}
		}
		if(stemmedValue!=null) {
			for(int i=0;i<Return.length;i++) {
				assert stemmedValue[i].length==Return[i].length:"Looks like stemming in ShaniString started deleting words. Method split don't support it. Fix sentence word splitting alg.";
				for(int j=0;j<Return[i].length;j++) {
					char[][][] stval=new char[1][1][stemmedValue[i][j].length];
					System.arraycopy(stemmedValue[i][j], 0, stval[0][0], 0, stemmedValue[i][j].length);
					Return[i][j].stemmedValue=stval;
				}
			}
		}
		
		return Return;
	}
	
	/**Load ShaniString from xml node.
	 * Equivalent to {@link Storage#getShaniString(Node, String)}.
	 * @param doc Document to inside which search is performed.
	 * @param path Path to ShaniString inside node.*/
	public static ShaniString loadString(Document doc,String path) {
		return Storage.getShaniString(doc.getDocumentElement(), path);
	}
	/**Load ShaniString from xml node.
	 * Equivalent to {@link Storage#getShaniString(Node, String)}.
	 * @param where Base node for search.
	 * @param path Path to ShaniString inside node.*/
	public static ShaniString loadString(Node where,String path) {
		return Storage.getShaniString(where, path);
	}
	
	/**Sets XML node for storing data. Also push data to given node.
	 * @param node XML node which will be used as data storage.
	 */
	public void setNode(Node node) {
		origin=node;
		saveData();
	}
	private void saveData() {
		if(origin!=null) {
			origin.setTextContent(toFullString());
		}
	}
	
	/**Adds new entries.
	 * @param word String containing new entries.
	 */
	public void add(String word) {
		add(word,true);
	}
	/**Adds new entries.
	 * @param word String containing new entries.
	 * @param cut If cut based on '*' location.
	 */
	public void add(String word,boolean cut) {
		parseString(cut?ParseMode.split:ParseMode.raw,word);
	}
	/**Adds new entries.
	 * @param word ShaniString containing new entries
	 */
	public void add(ShaniString word) {
		if(word.stemmedValue!=null) {						//Prevents from stemming from scratch in next step  
			stem();
			if(word.words!=null)generateWords();
		}
		
		String[] newValue=new String[value.length+word.value.length];
		System.arraycopy(value, 0, newValue, 0, value.length);
		System.arraycopy(word.value, 0, newValue, value.length, word.value.length);
		value=newValue;
		
		saveData();
		
		if(stemmedValue!=null||word.stemmedValue!=null) {
			word.stem();
			
			char[][][] newStemmedValue=new char[stemmedValue.length+word.stemmedValue.length][][];
			System.arraycopy(stemmedValue, 0, newStemmedValue, 0, stemmedValue.length);
			System.arraycopy(word.stemmedValue, 0, newStemmedValue, stemmedValue.length, word.stemmedValue.length);
			stemmedValue=newStemmedValue;
			if(words!=null||word.words!=null) {
				word.generateWords();
				String[][] newWords=new String[stemmedValue.length][];
				System.arraycopy(words, 0, newWords, 0, words.length);
				System.arraycopy(word.words, 0, newWords, words.length, word.words.length);
				words=newWords;
			}
		}
	}
	
	protected void stem() {
		if(stemmedValue==null) {
			stemmedValue=new char[value.length][][];
			for(int i=0;i<value.length;i++) {
				stemmedValue[i]=stem(value[i]);
			}
		}
	}
	private char[][] stem(String data){
		char[] dataArray=data.toLowerCase().toCharArray();
		
		ArrayList<char[]> words=new ArrayList<char[]>();
		
		int wordLength=0;
		int wordStart=0;
		for(int j=0;j<dataArray.length;j++) {
			if(Character.isWhitespace(dataArray[j])) {
				if(wordLength!=0) {
					char[] word=new char[wordLength];
					System.arraycopy(dataArray, wordStart, word, 0, wordLength);
					words.add(word);
				}
				wordLength=0;
				wordStart=j+1;
			} else wordLength++;
		}
		if(wordLength!=0) {
			char[] word=new char[wordLength];
			System.arraycopy(dataArray, wordStart, word, 0, wordLength);
			words.add(word);
		}
		
		return words.toArray(new char[0][0]);
	}
	
	protected void generateWords() {
		if(words!=null)return;
		
		stem();
		words=new String[value.length][];
		for(int i=0;i<stemmedValue.length;i++) {
			words[i]=new String[stemmedValue[i].length];
			for(int j=0;j<stemmedValue[i].length;j++) {
				words[i][j]=new String(stemmedValue[i][j]);
			}
		}
	}
	
	/**Get cost of comparing this ShaniString and given String.
	 * @param str String to compare
	 * @return Cost of comparing this ShaniString and given String.
	 */
	public short getCompareCost(String str) {
		return getCompareCost(new ShaniString(str));
	}
	/**Get cost of comparing two ShaniStrings.
	 * @param str ShaniString to compare
	 * @return Cost of comparing this ShaniString and given ShaniString.
	 */
	public short getCompareCost(ShaniString str) {
		return getMatcher().apply(str).getCost();
	}
	
	/**Check if given String is equal to this ShaniString.
	 * Equivalent to {@link #equals(ShaniString)}.
	 * @param str String to check.
	 * @return If given String is equal to this ShaniString.
	 */
	public boolean equals(String str) {
		if(str.length()==0)return false;
		return equals(new ShaniString(str));
	}
	/**Check if this is equal to this. It don't check if two ShaniString are same, but perform fuzzy String Matching.
	 * @param str ShaniString to Check.
	 * @return If given shaniString is equal to this.
	 */
	public boolean equals(ShaniString str) {
		stem();
		str.stem();
		
		return getCompareCost(str)<Config.sentenceCompareThreshold;
	}
	
	/**Tries to find optimal match between two {@linkplain ShaniString} arrays using dtw algorithm.
	 * @param data Array of ShaniString where search will be performed.
	 * @param dataIndex Index on which matching will start. Data before it are skipped.
	 * @param str Acts like regex Pattern. Contain keywords which will be applied on data arg.
	 * @return {@link ArraysMatchingResoult} object containing cost of optimal match and index of last matched word in data array.
	 */
	public static ArraysMatchingResoult getMatchingIndex(ShaniString[] data,int dataIndex,ShaniString[] str) {
		short[][] costs=new short[data.length+1][str.length+1];
		
		for(int i=1;i<=str.length;i++) costs[dataIndex][i]=(short) (Config.wordInsertionCost*i);
		for(int i=dataIndex;i<=data.length;i++) costs[i][0]=(short) (Config.wordDeletionCost*(i-dataIndex));
		
		for(int i=dataIndex+1;i<costs.length;i++) {
			for(int j=1;j<costs[i].length;j++) {
				costs[i][j]=data[i-1].getCompareCost(str[j-1]);
			}
		}
		
		for(int i=dataIndex+1;i<=data.length;i++) {
			for(int j=1;j<costs[i].length;j++) {
				costs[i][j]=(short) min(costs[i-1][j-1]+costs[i][j],costs[i-1][j]+Config.wordDeletionCost,costs[i][j-1]+Config.wordInsertionCost);			//Config.wordDeletionCost can not work well here. It can skip some words which should be matched as data in SentenceMatcher
			}
		}
		
//		for(int i=0;i<costs.length;i++) {
//			for(int j=0;j<costs[i].length;j++) {
//				System.out.printf("% 5d ",costs[i][j]);
//			}
//			System.out.println();
//		}
//		System.out.println("--------------------");
		
		int endIndex=costs[0].length-1;
		int minIndex=dataIndex+1;
		for(int i=dataIndex+2;i<costs.length;i++) {
			if(costs[minIndex][endIndex]>costs[i][endIndex])minIndex=i;
		}
		
//		System.out.println(Arrays.toString(data)+" "+Arrays.toString(str)+" "+costs[minIndex][endIndex]);
		return new ArraysMatchingResoult(costs[minIndex][endIndex],dataIndex,minIndex);
	}
	/**Tries to find optimal match between two {@linkplain ShaniString} arrays using dtw algorithm.
	 * Starting point of match can be on any point greater then dataIndex. That is only difference between this and {@link #getMatchingIndex(ShaniString[], int, ShaniString[]) getMatchingIndex} method.
	 * @param data Array of ShaniString where search will be performed.
	 * @param dataIndex Index on which matching will start. Data before it are skipped.
	 * @param str Acts like regex Pattern. Contain keywords which will be applied on data arg.
	 * @return {@link ArraysMatchingResoult} object containing cost of optimal match, index of first and last matched word in data array.
	 */
	public static ArraysMatchingResoult getMatchingIndexMovable(ShaniString[] data,int dataIndex,ShaniString[] str) {
		short[][] costs=new short[data.length+1][str.length+1];
		
		for(int i=1;i<=str.length;i++) costs[dataIndex][i]=(short) (Config.wordInsertionCost*i);
		//for(int i=dataIndex;i<=data.length;i++) costs[i][0]=(short) (Config.wordDeletionCost*(i-dataIndex));
		
		for(int i=dataIndex+1;i<costs.length;i++) {
			for(int j=1;j<costs[i].length;j++) {
				costs[i][j]=data[i-1].getCompareCost(str[j-1]);
			}
		}
		
		for(int i=dataIndex+1;i<=data.length;i++) {
			for(int j=1;j<costs[i].length;j++) {
				costs[i][j]=(short) min(costs[i-1][j-1]+costs[i][j],costs[i-1][j]+Config.wordDeletionCost,costs[i][j-1]+Config.wordInsertionCost);			//Config.wordDeletionCost can not work well here. It can skip some words which should be matched as data in SentenceMatcher
			}
		}
		
//		for(int i=0;i<costs.length;i++) {
//			for(int j=0;j<costs[j].length;j++) {
//				System.out.printf("% 5d ",costs[i][j]);
//			}
//			System.out.println();
//		}
//		System.out.println("--------------------");
		
		int endIndex=costs[0].length-1;
		int minIndex=dataIndex+1;
		for(int i=dataIndex+2;i<costs.length;i++) {
			if(costs[minIndex][endIndex]>costs[i][endIndex])minIndex=i;
		}
		
		int startIndex=minIndex;
		for(int i=str.length;i>1;i--) {								//Skip first row. Always 0 here.
			if(startIndex==0)break;
			short minCost=(short) min(costs[startIndex][i],costs[startIndex-1][i],costs[startIndex][i-1]);
			if(minCost==costs[startIndex][i-1])continue;
			if(!(minCost==costs[startIndex-1][i-1]))i++;
			startIndex--;
		}
		startIndex--;
		
		return new ArraysMatchingResoult(costs[minIndex][endIndex],startIndex,minIndex);
	}
	public static class ArraysMatchingResoult{
		public final short cost;
		public final int startIndex;
		public final int endIndex;
		protected ArraysMatchingResoult(short cost,int startIndex,int endIndex) {
			this.cost=cost;
			this.startIndex=startIndex;
			this.endIndex=endIndex;
		}
		
		@Override
		public String toString() {
			return startIndex+":"+endIndex+"->"+cost;
		}
	}
	
	/**Check if one of underlying strings matches given regex Pattern.
	 * @param pattern Patter used to check equality.
	 * @return If one of underlying strings matches given regex Pattern.
	 */
	public boolean isEquals(Pattern pattern) {
		for(String str:value) {
			if(pattern.matcher(str).matches())return true;
		}
		return false;
	}
	
	/**
	 * Compare to char arrays using Damerau�Levenshtein algorithm.
	 * @param a model
	 * @param b compared number
	 * @return DTW distance between two char[]
	 */
	protected static short charCompare(char[]a, char[]b) {
		final short max=Short.MAX_VALUE-1000;
		
		short[][] val=new short[a.length+1][b.length+1];
		for(int i=1;i<val.length;i++)val[i][0]=max;
		for(int i=1;i<val[0].length;i++)val[0][i]=max;
		
		for(int x=1;x<=a.length;x++) {
			int x1=x-1;
			for(int y=1;y<=b.length;y++) {
				val[x][y]=(short) min(val[x1][y]+Config.characterInsertionCost,
									  val[x][y-1]+Config.characterDeletionCost,
									  val[x1][y-1]+compareChar(a[x-1],b[y-1]));
				if(x>1&&y>1) {
					short swapCost=(short) (compareChar(a[x-1],b[y-2])+compareChar(a[x-2],b[y-1]));
					if(swapCost<Config.characterSwapThreshold) {
						val[x][y]=(short) min(val[x-2][y-2]+swapCost+Config.characterSwapCost,
											  val[x][y]);
					}
				}
			}
		}
		
//		System.out.println(new String(a)+" "+new String(b));
//		for(int i=0;i<val.length;i++) {
//			for(int j=0;j<val[i].length;j++) {
//				System.out.printf("% 6d ",val[i][j]);
//			}
//			System.out.println();
//		}
		
		short Return=val[val.length-1][val[0].length-1];
		
		int length=Math.min(a.length,b.length);
		Return=Config.characterCompareCostMultiplier.multiple(Return, length);
		
		return Return;
	}
	
	private static byte compareChar(char a,char b) {
		if(a>lookUpTable.length||b>lookUpTable[a].length) {
			return Config.differentCharacterCost;
		}
		return lookUpTable[a][b];
	}
	
	/**Returns array of String containing each underlying String.
	 * @return Look above.
	 */
	public String[] getArray() {
		return value;
	}
	
	/**Prints content to System.out.
	 * Equivalent to System.out.println({@link #toString() toString()}).
	 * 
	 */
	public void printOut() {
		System.out.println(toString());
	}
	
	/**Returns full String representation. Contain each underlying String split with '*' character.
	 * new ShaniString(oldShaniString.toFullString()) should give object identical to oldShaniString.
	 * @return String representing this ShaniString.
	 */
	public String toFullString() {
		if(value==null)return "NULL_SHANI_STRING";
		if(value.length==0)return "EMPTY_SHANI_STRING";
		StringBuffer str=new StringBuffer();
		str.append(value[0]);
		for(int i=1;i<value.length;i++)str.append("*").append(value[i]);
		return str.toString();
	}
	/* Return one random String from inner String set.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if(value==null)return "NULL_SHANI_STRING";
		if(value.length<=0)return "EMPTY_SHANI_STRING";
		return value[random.nextInt(value.length)];
	}
	/**Create deep copy of this object.
	 * @return Deep copy of this object.
	 */
	@Override
	public ShaniString copy() {
		return new ShaniString(value.clone());
	}
	
	/**Check if contain any not empty String(containing any character except white characters).
	 * @return If this Shani String contain no data.
	 */
	public boolean isEmpty() {
		if(value.length==0)return true;
		for(var str:value) {
			if(!str.trim().isEmpty())return false;
		}
		return true;
	}
	
 	private static int min(int... args) {
		int min=args[0];
		int minIndex=0;
		for(int i=0;i<args.length;i++) {
			if(args[minIndex]>args[i]) {
				minIndex=i;
				min=args[i];
			}
		}
		return min;
	}
 	
	/**Return char array ready for fuzzy string matching.
	 * @return Look above.
	 */
	public char[][][] getStemmendValue(){
		if(stemmedValue==null)stem();
		return stemmedValue;
	}
	/**Creates and returns new ({@link ShaniMatcher}.
	 * @return Look above.
	 */
	public ShaniMatcher getMatcher() {
		return new ShaniMatcher(this);
	}
	/**
	 * An engine that performs match operations on a {@linkplain ShaniString ShaniString}.
	 * Warning: Not thread save, Some methods can give error when original {@linkplain ShaniString ShaniString} change after creation.
	 * @author TakMashido
	 */
	public class ShaniMatcher{
		private ShaniString data;
		private short[][] wordCost;					//minCost of each word
		private short[] costBias;					//cost bias
		
		protected ShaniMatcher(ShaniString origin) {
			origin.stem();
			data=origin;
			wordCost=new short[data.stemmedValue.length][];
			for(int i=0;i<wordCost.length;i++) {
				wordCost[i]=new short[data.stemmedValue[i].length];
				Arrays.fill(wordCost[i], Config.wordDeletionCost);
			}
			costBias=new short[data.stemmedValue.length];
		}
		
		/**Match given ShaniString[]. Equivalent to invoke apply(ShaniString) with each array element.
		 * @param comparables Array of ShaniString data which will be applied.
		 * @return this
		 */
		public ShaniMatcher apply(ShaniString... comparables) {
			for(var comp:comparables)apply(comp);
			return this;
		}
		/**Match given ShaniString.
		 * @param comparable ShaniString to be applied.
		 * @return this
		 */
		public ShaniMatcher apply(ShaniString comparable) {
			assert comparable!=null:"comparable item can't be null";
			assert comparable.value.length!=0:"comparable item shouldn't be empty";
			if(comparable==null) {
				for(int i=0;i<costBias.length;i++) {
					costBias[i]+=Config.wordInsertionCost;
				}
				return this;
			}
			if(comparable.value.length==0) {
				return this;
			}
			
			comparable.stem();
			
			for(int i=0;i<wordCost.length;i++) {
				processSet(i,comparable);
			}
			return this;
		}
		/**Match given String.
		 * Equivalent to apply(new ShaniString(data))
		 * @param data String to be applied.
		 * @return this
		 */
		public ShaniMatcher apply(String data) {
			apply(new ShaniString(data));
			return this;
		}
		private void processSet(int index, ShaniString comparable) {
			class CompareResoult implements Comparator<CompareResoult>{
				short cost;
				int index;
				CompareResoult(){}
				CompareResoult(short cost,int index){
					this.cost=cost;
					this.index=index;
				}
				
				@Override
				public int compare(CompareResoult o1, CompareResoult o2) {
					return o1.cost-o2.cost;
				}
			}
			short[][] wordsCosts=new short[comparable.stemmedValue.length][wordCost[index].length];									//wordCosts after matching exact comparable wordsSet
			for(int j=0;j<wordsCosts.length;j++)for(int k=0;k<wordsCosts[j].length;k++)wordsCosts[j][k]=wordCost[index][k];
			short[] costBiases=new short[comparable.stemmedValue.length];															//costBias  after matching exact comparable wordsSet
			Arrays.fill(costBiases, costBias[index]);
			ArrayList<CompareResoult> resoults=new ArrayList<CompareResoult>();
			for(int j=0;j<comparable.stemmedValue.length;j++) {					//comparable sets
				for(int k=0;k<comparable.stemmedValue[j].length;k++) {			//words in comparable
					for(int l=0;l<data.stemmedValue[index].length;l++) {
						short cost=charCompare(data.stemmedValue[index][l],comparable.stemmedValue[j][k]);
						if(cost<Config.wordCompareThreshold)
							resoults.add(new CompareResoult(cost,l));
					}
					if(resoults.isEmpty()) {
						costBiases[j]+=Config.wordInsertionCost;
						continue;
					}
					resoults.sort(new CompareResoult());
					
					boolean setted=false;
					for(CompareResoult resoult:resoults) {
						if(!(wordsCosts[j][resoult.index]<Config.wordDeletionCost)) {
							wordsCosts[j][resoult.index]=resoult.cost;
							setted=true;
						}
					}
					if(!setted) {																		//Can be changed to find real lowest possible cost
						wordsCosts[j][resoults.get(0).index]=resoults.get(0).cost;
						costBiases[j]+=Config.wordInsertionCost;
					}
					
					resoults.clear();
				}
			}
			
			int minCostIndex=0;
			short minCost=getCost(costBiases[0],wordsCosts[0]);
			short cost;
			for(int j=1;j<wordsCosts.length;j++) {
				if((cost=getCost(costBiases[j],wordsCosts[j]))<minCost) {
					minCost=cost;
					minCostIndex=j;
				}
			}
			
			costBias[index]=costBiases[minCostIndex];
			wordCost[index]=wordsCosts[minCostIndex];
		}
		
		/**Do word based exact compare. Search for combination of words which are {@link String#equals(Object)} to given ShaniString.
		 * <p>
		 * note: number of white characters is ignored, only the fact they exist end divide ShaniString value to words matters
		 * @param comparables ShaniString containing String values to be searched in underlying ShaniString.
		 * @return this ShaniMatcher object.
		 */
		public ShaniMatcher exactApply(ShaniString... comparables) {
			for(ShaniString comparable:comparables) {
				assert comparable!=null:"comparable item can't be null";
				assert comparable.value.length!=0:"comparable item shouldn't be empty";
				if(comparable==null) {
					for(int i=0;i<costBias.length;i++) {
						costBias[i]+=Config.wordInsertionCost;
					}
					return this;
				}
				if(comparable.value.length==0) {
					return this;
				}
				for(String str:comparable.value) {
					exactApply(str);
				}
			}
			return this;
		}
		/**Do word based exact compare. Search for combination of words which are {@link String#equals(Object)} to given ShaniString.
		 * <p>
		 * note: number of white spaces is ignored, only the fact they exist end divide ShaniString value to words matters
		 * @param comparable String to find in underlying ShaniString.
		 * @return this ShaniMatcher object.
		 */
		public ShaniMatcher exactApply(String comparable) {
			data.generateWords();
			
			ArrayList<String> comparableWords=new ArrayList<>();
			@SuppressWarnings("resource")
			Scanner sc=new Scanner(comparable);
			while(sc.hasNext())comparableWords.add(sc.next());
			
			boolean matched;
			int comparableIndex=0;
			for(int i=0;i<data.words.length;i++) {
				matched=false;
				for(int j=0;j<data.words[i].length;j++) {
					if(data.words[i][j].equals(comparableWords.get(comparableIndex))&&wordCost[i][j]>=Config.wordDeletionCost) {
						comparableIndex++;
						if(comparableIndex==comparableWords.size()) {
							matched=true;
							for(int k=0;k<comparableIndex;k++)
								wordCost[i][j-k]=0;
						}
					} else comparableIndex=0;
				}
				if(!matched)costBias[i]+=comparableWords.size()*Config.wordInsertionCost;
			}
			return this;
		}
		
		/**Check if cost of compare is smaller then Config.sentenceCompareThreshold.
		 * @return If underlying ShaniString is equal to all applied data.
		 */
		public boolean isEqual() {
			return getCost()<Config.sentenceCompareThreshold;
		}
		/**Check if applied data are presented in underlying ShaniString. Skip unmatched words.
		 * @return Look above.
		 */
		public boolean isSemiEqual() {
			return getMatchedCost()<Config.sentenceCompareThreshold &&getMatchedNumber()>0;
		}
		
		/**Returns ShaniString containing all unmatched data.
		 * @return Look above.
		 */
		public ShaniString getUnmatched() {
			ShaniString Return=new ShaniString();
			StringBuffer buffer;
			for(int i=0;i<wordCost.length;i++) {
				buffer=new StringBuffer();
				for(int j=0;j<wordCost[i].length;j++) {
					if(wordCost[i][j]>=Config.wordCompareThreshold)
						buffer.append(data.stemmedValue[i][j]).append(' ');
				}
				if(buffer.length()>0)buffer.substring(0,buffer.length()-1);
				Return.add(buffer.toString(),false);
			}
			
			return Return;
		}
		
		private short getCost(short bias, short[] wordCost) {
			short cost=bias;
			
			for(int i=0;i<wordCost.length;i++) {
				cost+=wordCost[i];
			}
			
			return cost;
		}
		private short getCost(int index) {
			if(index>=costBias.length)return Short.MAX_VALUE;
			return getCost(costBias[index],wordCost[index]);
		}
		/**Returns minimum cost of comparison with {@link takMashido.shani.core.text.ShaniString ShaniString's} given by apply(ShaniString) method.
		 * @return Comparison cost.
		 */
		public short getCost() {
			short cost;
			short min=getCost(0);
			for(int i=1;i<costBias.length;i++)if((cost=getCost(i))<min)min=cost;
			return min;
		}
		/**Returns minimum cost of comparison with {@link takMashido.shani.core.text.ShaniString ShaniString's} given by apply(ShaniString) method.
		 * Ignore unmatched words cost.
		 * @return Comparison cost.
		 */
		public short getMatchedCost() {
			short minCost=Short.MAX_VALUE;
			for(int i=0;i<wordCost.length;i++) {
				short cost=costBias[i];
				for(int j=0;j<wordCost[i].length;j++)if(wordCost[i][j]<Config.wordCompareThreshold)cost+=wordCost[i][j];
				if(cost<minCost)minCost=cost;
			}
			return minCost;
		}
		/**Counts number of already matched words.
		 * @return Number of matched words.
		 */
		public int getMatchedNumber() {
			int Return=0;
			for(int i=0;i<wordCost.length;i++) {
				int matched=0;
				for(int j=0;j<wordCost[i].length;j++)if(wordCost[i][j]<Config.wordCompareThreshold)matched++;
				if(matched>Return)Return=matched;
			}
			return Return;
		}
		
		/**Creates semi-deep copy of Object. State of Matching(cost, lastPosition) are copied, but base ShaniString object stays the same.
		 * @return Semi-deep copy of object;
		 */
		@Override
		public ShaniMatcher clone() {
			ShaniMatcher Return=new ShaniMatcher(data);
			Return.wordCost=wordCost.clone();
			Return.costBias=costBias.clone();
			return Return;
		}
	}
}