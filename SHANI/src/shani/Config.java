package shani;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;

/**Set of constants for SHANI API.
 * @author TakMashido
 */
public class Config {
	private Config() {}
	
	static {
		var prop=new Properties();
		try {
			prop.load(Config.class.getResourceAsStream("/files/config.properties"));
		} catch (IOException e) {
			System.err.println("Can't find properties file. Initializing with default values.");
			System.out.println("Config file not found.");
			e.printStackTrace();
		}
		
		String config2Name=prop.getProperty("secondConfig");
		File config2;
		if(config2Name!=null) {
			File preConfig=new File(prop.getProperty("secondConfig"));
			if(preConfig.exists())
				config2=new File(prop.getProperty("secondConfig"));
			else config2=null;
		} else config2=null;
		
		secondConfig=config2;
		
		Properties prop2=null;
		try {
			if(config2!=null) {
				prop2=new Properties();
				prop2.load(new FileInputStream(config2));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Properties[] props;
		if(prop2!=null) {
			props=new Properties[] {prop2,prop};
		} else {
			props=new Properties[] {prop};
		}
		
		mainFile=new File(prop.getProperty("mainFile","Shani.dat"));									//If failed to load properties from file dedfault onw will be used
		
		positiveResponeKey=new ShaniString(getProperty(props,"positiveResponeKey","failed to load positiveResponeKey"));
		negativeResponeKey=new ShaniString(getProperty(props,"negativeResponeKey","failed to load negaticeResponeKey"));
		
		diffrendCharacterCost=(byte)getIntProperty(props,"diffrendCharacterCost","50");
		qwertyNeighbourCost=(byte)getIntProperty(props,"qwertyNeighbourCost","30");
		nationalSimilarityCost=(byte)getIntProperty(props,"nationalSimilarityCost","10");
		
		wordCompareTreshold=(short)getIntProperty(props,"wordCompareTreshold","100");
		characterCompareCostMultiplier=new Multiplier(getProperty(props,"characterCompareCostMultiplier","10,5,2,1.2f,1"));
		characterDeletionCost=(short)getIntProperty(props,"characterDeletionCost","50");
		characterInsertionCost=(short)getIntProperty(props,"characterInsertionCost","50");
		characterSwapTreshold=(short)getIntProperty(props,"characterSwapTreshold","50");
		characterSwapCost=(short)getIntProperty(props,"characterSwapCost","30");
		
		sentenseCompareTreshold=(short)getIntProperty(props,"sentenseCompareTreshold","300");
		wordInsertionCost=(short)getIntProperty(props,"wordInsertionCost","300");
		wordDeletionCost=(short)getIntProperty(props,"wordDeletionCost","120");
		
		sentenceMatcherWordReturnImportanceBias=(short)getIntProperty(props, "sentenceMatcherWordReturnImportanceBias", "-5");
		sentenceMatcherRegexImportanceBias=(short)getIntProperty(props, "sentenceMatcherRegexImportanceBias", "5");
		
		importanceBiasMultiplier=getFloatProperty(props,"importanceBiasMultiplier",".5");
		
		socksProxyHost=getProperty(props,"socksProxyHost",null);
		socksProxyPort=getIntProperty(props,"socksProxyPort","0");
		
		HTTPProxyHost=getProperty(props,"HTTPProxyHost",null);
		HTTPProxyPort=getIntProperty(props,"HTTPProxyPort","0");
	}
	private static final String getProperty(Properties props[], String key, String defaultVal) {
		for(int i=0;i<props.length;i++) {
			String val=props[i].getProperty(key);
			if(val!=null)return val;
		}
		
		return defaultVal;
	}
	private static final int getIntProperty(Properties props[],String key,String defaultVal) {
		try {
			return Integer.parseInt(getProperty(props, key, defaultVal));
		} catch(NumberFormatException ex) {
			return Integer.parseInt(defaultVal);
		}
	}
	private static final float getFloatProperty(Properties props[],String key,String defaultVal) {
		try {
			return Float.parseFloat(getProperty(props, key, defaultVal));
		} catch(NumberFormatException ex) {
			return Float.parseFloat(defaultVal);
		}
	}
	
	/*Files location*/
	@SuppressWarnings("unused")
	private static final File secondConfig;
	public static final File mainFile;
	
	/*Basic responses*/												//TODO move to main file
	public static final ShaniString positiveResponeKey;
	public static final ShaniString negativeResponeKey;
	
	/*Characters cost*/
	public static final byte diffrendCharacterCost;
	public static final byte qwertyNeighbourCost;
	public static final byte nationalSimilarityCost;						//a,¹||c,æ...
	
	
	public static final short wordCompareTreshold;
	public static final Multiplier characterCompareCostMultiplier;					//Multiplier of cost for distance between short strings, val[length-1]
	public static final short characterDeletionCost;
	public static final short characterInsertionCost;
	public static final short characterSwapTreshold;
	public static final short characterSwapCost;
	
	public static final short sentenseCompareTreshold;
	public static final short wordInsertionCost;
	public static final short wordDeletionCost;
	
	/*SentenceMatcher*/
	public static final short sentenceMatcherWordReturnImportanceBias; 
	public static final short sentenceMatcherRegexImportanceBias;
	
	public static final float importanceBiasMultiplier;
	
	/*Network settings*/
	public static final String socksProxyHost;
	public static final int socksProxyPort;
	public static final String HTTPProxyHost;
	public static final int HTTPProxyPort;
	
	public static boolean verbose=false;
	
	/**Multiple value by parameter depending on another one.
	 * @author TakMashido
	 */
	public static final class Multiplier{
		private final float[] multipliers;
		
		public Multiplier(float... data) {
			multipliers=data;
		}
		public Multiplier(String data) {
			@SuppressWarnings("resource")
			Scanner in=new Scanner(data).useDelimiter("(?:\\s*,\\s*)+");
			
			ArrayList<Float> values=new ArrayList<>();
			while(in.hasNext())values.add(Float.parseFloat(in.next()));
			
			multipliers=new float[values.size()];
			for(int i=0;i<multipliers.length;i++)multipliers[i]=values.get(i);
		}
		
		public short multiple(short value, int length) {
			assert length>0;
			if(--length<multipliers.length)return (short)(value*multipliers[length]);
			else return (short)(value*multipliers[multipliers.length-1]);
		}
	}
}