package shani;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.w3c.dom.Node;

public class ShaniString {
	private static final Random random=new Random();
	private static final Pattern stringDivider=Pattern.compile("(?:\\s*\\*\\s*)+");
	private static byte[][] lookUpTable;				//~140KB of memory
	
	private String[] value;
	private char[][][] stemmedValue=null;											//[a][b][c]: a->wordsSet, b->word, c->letter
	/**Node containing this ShaniString data.*/
	private Node origin;
	
	static {
		createLookUpTable();
	}
	private static void createLookUpTable(){
		char[][] nationalLettersTable=new char[][] {
			{'a','¹'},
			{'c','æ'},
			{'e','ê'},
			{'l','³'},
			{'n','ñ'},
			{'o','ó'},
			{'s','œ'},
			{'z','¿','Ÿ'},
		};
		
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
			for(int j=0;j<lookUpTable[i].length;j++) {
				lookUpTable[i][j]=Config.diffrendCharacterCost;
			}
		}
		
		for(int i=0;i<qwertyTable.length;i++) {				//TODO it use keys distace without looking for shift of rows. Fix if;
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
	
	public ShaniString() {
		this("");
	}
	public ShaniString(String origin) {
		value=processString(origin);
	}
	public ShaniString(String... origin) {
		ArrayList<String> buf=new ArrayList<String>();
		for(String str:origin)processString(str,buf);
		value=buf.toArray(new String[] {});
	}
	public ShaniString(Node origin) {
		assert origin!=null:"Origin node can't be null. Propably node is missing in shani main file.";
		value=processString(origin.getTextContent());
		this.origin=origin;
	}
	public ShaniString(Node originNode, String origin) {
		this(originNode);
		add(origin);
	}
	private String[] processString(String str) {
		return processString(str,new ArrayList<String>()).toArray(new String[] {});
	}
	private ArrayList<String> processString(String str, ArrayList<String> out) {
		@SuppressWarnings("resource")
		Scanner in=new Scanner(str).useDelimiter(stringDivider);
		
		while(in.hasNext())out.add(in.next());
		
		return out;
	}
	
	/**Loads string from storage section.
	 * @param path Path to String represetation of ShaniString in storage.
	 * @return New ShaniString object containing data pointed by path.
	 */
	public static ShaniString loadString(String path) {
		return Storage.getString(path);
	}
	
	public void setNode(Node node) {
		origin=node;
		saveData();
	}
	private void saveData() {
		if(origin!=null) {
			origin.setTextContent(toFullString());
		}
	}
	
	public void add(String word) {
		String[] wordArray=processString(word);
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
		}
	}
	public void add(ShaniString word) {
		String[] newValue=new String[value.length+word.value.length];
		System.arraycopy(value, 0, newValue, 0, value.length);
		System.arraycopy(word.value, 0, newValue, value.length, word.value.length);
		value=newValue;
		
		saveData();
		
		if(stemmedValue!=null||word.stemmedValue!=null) {
			this.stem();
			word.stem();
			
			char[][][] newStemmedValue=new char[stemmedValue.length+word.stemmedValue.length][][];
			System.arraycopy(stemmedValue, 0, newStemmedValue, 0, stemmedValue.length);
			System.arraycopy(word.stemmedValue, 0, newStemmedValue, stemmedValue.length, word.stemmedValue.length);
			stemmedValue=newStemmedValue;
		}
	}
	
	private void stem() {
		if(stemmedValue==null) {
			stemmedValue=new char[value.length][][];
			for(int i=0;i<value.length;i++) {
				stemmedValue[i]=stem(value[i]);
			}
		}
	}
	private char[][] stem(String data){
		char[] dataArray=data.toCharArray();
		
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
	
	public boolean equals(String str) {
		if(str.length()==0)return false;
		return equals(new ShaniString(str));
	}
	public boolean equals(ShaniString str) {
		stem();
		str.stem();
		
		return getMatcher().apply(str).getCost()<Config.sentenseCompareTreshold;
	}
	
	/**
	 * Compare to char arrays using Damerau–Levenshtein algoritm.
	 * @param a model
	 * @param b compared number
	 * @return DTW distance beetwen two char[]
	 */
	private static short charCompare(char[]a, char[]b) {
		final short max=Short.MAX_VALUE-1000;
		
		short[][] val=new short[a.length+1][b.length+1];
		for(int i=1;i<val.length;i++)val[i][0]=max;
		for(int i=1;i<val[0].length;i++)val[0][i]=max;
		
		for(int x=1;x<=a.length;x++) {
			int x1=x-1;
			for(int y=1;y<=b.length;y++) {
				val[x][y]=(short) min(val[x1][y]+Config.characterInsertionCost,
									  val[x][y-1]+Config.characterDeletionCost,
									  val[x1][y-1]+lookUpTable[a[x-1]][b[y-1]]);
				if(x>2&&y>2) {
					short swapCost=(short) (lookUpTable[a[x-1]][b[y-2]]+lookUpTable[a[x-2]][b[y-1]]);
					if(swapCost<Config.characterSwapTreshold) {
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
		
		if(Return<Config.wordCompareTreshold*2)
			Engine.info.println(new String(a)+"-> "+new String(b)+": "+Return);
		
		return Return;
	}
	
	public String[] getArray() {
		return value;
	}
	
	public String toFullString() {
		StringBuffer str=new StringBuffer();
		str.append(value[0]);
		for(int i=1;i<value.length;i++)str.append("*").append(value[i]);
		return str.toString();
	}
	public String toString() {
		if(value.length<=0)return null;
		return value[random.nextInt(value.length)];
	}
	public ShaniString copy() {
		return new ShaniString(value);
	}
	
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
 	
	public char[][][] getStemmendValue(){
		if(stemmedValue==null)stem();
		return stemmedValue;
	}
	public ShaniMatcher getMatcher() {
		return new ShaniMatcher(this);
	}
	/**
	 * An engine that performs match operations on a {@linkplain shani.ShaniString
	 * ShaniString}.
	 * @warning Not thread save, can some methods give error when original {@linkplain shani.ShaniString ShaniString} change after creation. 
	 * @author TakMashido
	 */
	public class ShaniMatcher{
		private ShaniString data;
		private short[][] wordCost;					//minCost of each word
		private short[] costBias;							//cost bias
		
		private ShaniMatcher(ShaniString origin) {
			origin.stem();
			data=origin;
			wordCost=new short[data.stemmedValue.length][];
			for(int i=0;i<wordCost.length;i++) {
				wordCost[i]=new short[data.stemmedValue[i].length];
				Arrays.fill(wordCost[i], Config.wordDeletionCost);
			}
			costBias=new short[data.stemmedValue.length];
		}
		
		/**Match given ShaniString[]. Equivalent to invoce apply(ShaniString) with each array element.
		 * @param comparables
		 * @return
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
			comparable.stem();
			
			for(int i=0;i<wordCost.length;i++) {
				processSet(i,comparable);
			}
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
						if(cost<Config.wordCompareTreshold) 
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
					if(!setted) {																		//Can be changed to finding real lowest possible cost
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
		
		public boolean isEqual() {
			return getCost()<Config.sentenseCompareTreshold;
		}
		public boolean isSemiEqual() {
			return getMatchedCost()<Config.sentenseCompareTreshold&&getMatchedNumber()>0;
		}
		
		public ShaniString getUnmatched() {
			ShaniString Return=new ShaniString();
			StringBuffer buffer;
			for(int i=0;i<wordCost.length;i++) {
				buffer=new StringBuffer();
				for(int j=0;j<wordCost[i].length;j++) {
					if(wordCost[i][j]>=Config.wordCompareTreshold)
						buffer.append(data.stemmedValue[i][j]).append(' ');
				}
				if(buffer.length()>0)buffer.substring(0,buffer.length()-1);
				Return.add(buffer.toString());
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
		/**Returns minimum cost of comparison with {@link shani.ShaniString ShaniString's} given by apply(ShaniString) method.
		 * @return Comparison cost.
		 */
		public short getCost() {
			short cost;
			short min=getCost(0);
			for(int i=1;i<costBias.length;i++)if((cost=getCost(i))<min)min=cost;
			return min;
		}
		/**Returns minimum cost of comparison with {@link shani.ShaniString ShaniString's} given by apply(ShaniString) method.
		 * Ignore unmatched words cost.
		 * @return Comparison cost.
		 */
		public short getMatchedCost() {
			short minCost=Short.MAX_VALUE;
			for(int i=0;i<wordCost.length;i++) {
				short cost=costBias[i];
				for(int j=0;j<wordCost.length;j++)if(wordCost[i][j]<Config.wordCompareTreshold)cost+=wordCost[i][j];
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
				for(int j=0;j<wordCost[i].length;j++)if(wordCost[i][j]<Config.wordCompareTreshold)matched++;
				if(matched>Return)Return=matched;
			}
			return Return;
		}
		
		/**Creates semi-deep copy of Object. State of Mathig(cost, lastPosition) are copied, but base ShaniString object stays the same.
		 * @return Semi-deep copy of object;
		 */
		public ShaniMatcher clone() {
			ShaniMatcher Return=new ShaniMatcher(data);
			Return.wordCost=wordCost.clone();
			Return.costBias=costBias.clone();
			return Return;
		}
	}
	
//	public static void main(String[]args){
//		Engine.debug=new PrintStream(new OutputStream() {
//			public void write(int b) throws IOException {}
//		});
////		Engine.debug=System.out;
////		char[] a="eclipse".toCharArray();
////		char[] b="eagle".toCharArray();
//		
////		char[] a="shani".toCharArray();
////		char[] b="shahni".toCharArray();
//		
////		char[] a="shani".toCharArray();
////		char[] b="shaini".toCharArray();
//		
////		char[] a="tpt".toCharArray();
////		char[] b="tor".toCharArray();
//		
//		ShaniString a=new ShaniString("odpal nic");
//		ShaniMatcher b=a.getMatcher().apply(new ShaniString("odpal"));
//		//ShaniString a=new ShaniString("odpal flackflag");
//		//ShaniMatcher b=a.getMatcher().apply(new ShaniString("odpal")).apply(new ShaniString("black flaga"));
//		System.out.println("final cost= "+b.getCost());
//		System.out.println(b.getUnmatched());
//		//System.out.println(charCompare(a, b));
//	}
}