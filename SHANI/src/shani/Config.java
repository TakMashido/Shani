package shani;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.Scanner;

/**Set of constants for SHANI API.
 * @author TakMashido
 */
public class Config {
	private Config() {}
	
	private static boolean ignoreMissingPropertyErrors=false;
	private static final ArrayList<String> errors=new ArrayList<>();
	
	static {
		Properties[] props=null;
		
		try {
			props=getProperties(Config.class.getResource("/files/config.properties"));
		} catch(IOException ex) {
			System.out.println("Error during parsing configuration files.");
			ex.printStackTrace();
			System.exit(-1);
		}
		
		mainFile=new File(getProperty(props,"mainFile"));
		
		positiveResponeKey=new ShaniString(getProperty(props,"positiveResponeKey"));
		negativeResponeKey=new ShaniString(getProperty(props,"negativeResponeKey"));
		
		diffrendCharacterCost=(byte)getIntProperty(props,"diffrendCharacterCost");
		qwertyNeighbourCost=(byte)getIntProperty(props,"qwertyNeighbourCost");
		nationalSimilarityCost=(byte)getIntProperty(props,"nationalSimilarityCost");
		
		wordCompareTreshold=(short)getIntProperty(props,"wordCompareTreshold");
		characterCompareCostMultiplier=new Multiplier(getProperty(props,"characterCompareCostMultiplier"));
		characterDeletionCost=(short)getIntProperty(props,"characterDeletionCost");
		characterInsertionCost=(short)getIntProperty(props,"characterInsertionCost");
		characterSwapTreshold=(short)getIntProperty(props,"characterSwapTreshold");
		characterSwapCost=(short)getIntProperty(props,"characterSwapCost");
		
		sentenseCompareTreshold=(short)getIntProperty(props,"sentenseCompareTreshold");
		wordInsertionCost=(short)getIntProperty(props,"wordInsertionCost");
		wordDeletionCost=(short)getIntProperty(props,"wordDeletionCost");
		
		sentenceMatcherWordReturnImportanceBias=(short)getIntProperty(props, "sentenceMatcherWordReturnImportanceBias");
		sentenceMatcherRegexImportanceBias=(short)getIntProperty(props, "sentenceMatcherRegexImportanceBias");
		
		importanceBiasMultiplier=getFloatProperty(props,"importanceBiasMultiplier");
		
		ignoreMissingPropertyErrors=true;
		socksProxyHost=getProperty(props,"socksProxyHost");
		socksProxyPort=getIntProperty(props,"socksProxyPort");
		
		HTTPProxyHost=getProperty(props,"HTTPProxyHost");
		HTTPProxyPort=getIntProperty(props,"HTTPProxyPort");
		ignoreMissingPropertyErrors=false;
		
		if(!errors.isEmpty()) {
			Engine.registerLoadException();
			System.err.println("Config file is corrupted:");
			for(String str:errors) {
				System.err.println("\t"+str);
			}
		}
	}
	private static final String getProperty(Properties props[], String key) {
		for(int i=0;i<props.length;i++) {
			String val=props[i].getProperty(key);
			if(val!=null)return val;
		}
		
		if(!ignoreMissingPropertyErrors)
			errors.add("Can't find property \""+key+"\" in config files.");
		
		return null;
	}
	
	private static final int getIntProperty(Properties props[],String key) {
		String str=getProperty(props, key);
		if(str==null)return 0;
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			errors.add('"'+str+"\" in property \""+key+"\" is not valid integer.");
		}
		return 0;
	}
	private static final float getFloatProperty(Properties props[],String key) {
		String str=getProperty(props, key);
		if(str==null)return 0;
		try {
			return Float.parseFloat(str);
		} catch (NumberFormatException e) {
			errors.add('"'+str+"\" in property \""+key+"\" is not valid float.");
		}
		return 0;
	}
	
	private static final Properties[] getProperties(URL resource) throws IOException {
		ArrayList<Properties> ret=new ArrayList<>();
		
		Queue<URL> toProcess=new LinkedList<>();
		toProcess.add(resource);
		
		while(!toProcess.isEmpty()) {
			URL source=toProcess.poll();
			
			try(InputStream stream=source.openStream()){
				Properties prop=new Properties();
				prop.load(stream);
				ret.add(prop);
				
				String configDir=prop.getProperty("configLocation");
				
				if(configDir!=null) {
					File file=new File(configDir);
					
					if(file.isDirectory()||configDir.endsWith("/")) {
						if(!file.exists()) {
							if(!file.mkdirs()) {
								Engine.registerLoadException();
								System.err.println("Failed to create config directory: "+file);
							}
							continue;
						}
						
						String[] files=file.list();
						Arrays.sort(files);
						
						for(String str:files) {
							toProcess.add(new File(str).toURI().toURL());
						}
					} else {
						if(!file.exists()) {
							if(!(file.getParentFile().mkdir()&&file.createNewFile())) {
								Engine.registerLoadException();
								System.err.println("Failed to create config file: "+file);
							}
							continue;
						}
						toProcess.add(file.toURI().toURL());
					}
				}
			}
		}
		
		return ret.toArray(new Properties[ret.size()]);
	}
	
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
			int l=length;
			if(--l<multipliers.length)return (short)(value*multipliers[l]);
			else return (short)(value*multipliers[multipliers.length-1]);
		}
	}
}