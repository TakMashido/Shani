package shani;

import java.io.File;

/**Set of costans for SHANI API.
 * @author TakMashido
 */
public class Config {
	private Config() {}
	
	public static final File mainFile=new File("Shani.dat");
	
	public static final ShaniString positiveResponeKey=new ShaniString("youp*tak*yes");
	public static final ShaniString megaticeResponeKey=new ShaniString("nie*nope*niet*no");

	public static final byte diffrendCharacterCost = 50;
	public static final short qwertyNeighbourCost = 30;
	public static final short nationalSimilarityCost = 10;						//a,¹||c,æ...
	
	public static final short wordCompareTreshold = 100;
	public static final Multiplier characterCompareCostMultiplier=new Multiplier(10,5,2,1.5f,1);					//Multiplier of cost for distance beetwen short strings, val[lenght-1]
	public static final short characterDeletionCost=50;
	public static final short characterInsertionCost=50;
	public static final short characterSwapTreshold=50;
	public static final short characterSwapCost=30;
	
	public static final short sentenseCompareTreshold=300;
	public static final short wordInsertionCost=300;
	public static final short wordDeletionCost=120;
	
	
	/**Multilple value by parameter dependent in another one.
	 * @author TakMashido
	 */
	public static final class Multiplier{
		private final float[] multipliers;
		
		public Multiplier(float... data) {
			multipliers=data;
		}
		
		public short multiple(short value, int length) {
			assert length>0;
			if(--length<multipliers.length)return (short)(value*multipliers[length]);
			else return (short)(value*multipliers[multipliers.length-1]);
		}
	}
}