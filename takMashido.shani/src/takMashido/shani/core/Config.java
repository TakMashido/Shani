package takMashido.shani.core;

import takMashido.shani.Engine;
import takMashido.shani.core.text.ShaniString;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.Scanner;
import java.util.regex.Pattern;

/**Set of constants for SHANI API.
 * @author TakMashido
 */
public class Config {
	private static final Pattern stringSplitRegex= Pattern.compile("\\*");
	
	private Config() {}
	
	private static boolean ignoreMissingPropertyErrors=false;
	private static final ArrayList<String> errors=new ArrayList<>();
	
	static {
		Properties[] props=null;
		
		try {
			props=getProperties(Config.class.getResource("/takMashido/shani/files/config.properties"));
		} catch(IOException ex) {
			System.out.println("Error during parsing configuration files.");
			ex.printStackTrace();
			System.exit(-1);
		}
		
		language=getProperty(props,"language");
		
		extensionsDirectory=getFileProperty(props, "extensionsDirectory");
		initFileLocation=split(getAdditiveProperty(props, "initFileLocation"));
		
		dataFile=getFileProperty(props, "dataFile", Config.class.getResourceAsStream("/takMashido/shani/files/templates/shaniData.xml"));
		
		logsDirectory=getFileProperty(props, "logsDirectory");
		
		positiveResponseKey =new ShaniString(getProperty(props,"positiveResponseKey"));
		negativeResponseKey =new ShaniString(getProperty(props,"negativeResponseKey"));
		
		differentCharacterCost =(byte)getIntProperty(props,"differentCharacterCost");
		qwertyNeighbourCost=(byte)getIntProperty(props,"qwertyNeighbourCost");
		nationalSimilarityCost=(byte)getIntProperty(props,"nationalSimilarityCost");
		
		wordCompareThreshold =(short)getIntProperty(props,"wordCompareThreshold");
		characterCompareCostMultiplier=new Multiplier(getProperty(props,"characterCompareCostMultiplier"));
		characterDeletionCost=(short)getIntProperty(props,"characterDeletionCost");
		characterInsertionCost=(short)getIntProperty(props,"characterInsertionCost");
		characterSwapThreshold =(short)getIntProperty(props,"characterSwapThreshold");
		characterSwapCost=(short)getIntProperty(props,"characterSwapCost");
		
		sentenceCompareThreshold =(short)getIntProperty(props,"sentenceCompareThreshold");
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
		
		testMode=getBooleanProperty(props,"testMode");
		testManifestLocation=split(getAdditiveProperty(props, "testManifestLocation"));
		
		if(!errors.isEmpty()) {
			Engine.registerLoadException();
			System.err.println("Error during parsing config file:");
			for(String str:errors) {
				System.err.println("\t"+str);
			}
		}
	}
	
	//<loaders><loaders><loaders><loaders><loaders><loaders><loaders><loaders><loaders>
	private static final String getProperty(Properties props[], String key) {
		if(Launcher.configOverride.containsKey(key))
			return Launcher.configOverride.get(key);
		
		for(int i=0;i<props.length;i++) {
			String val=props[i].getProperty(key);
			if(val!=null)return val;
		}
		
		if(!ignoreMissingPropertyErrors)
			errors.add("Can't find property \""+key+"\" in config files.");
		
		return null;
	}
	/**Works line {@link #getProperty(Properties[], String)} but instead returning single property from most important config file returns array of values from all config files containing this one.
	 * @param props Properties object to load from.
	 * @param key Key to search for.
	 * @return All found occurrences of given key in props objects.
	 */
	private static final String[] getAdditiveProperty(Properties[] props, String key){
		ArrayList<String> ret=new ArrayList<>();
		
		if(Launcher.configOverride.containsKey(key)){
			Collections.addAll(ret,split(Launcher.configOverride.get(key)));
		}
		
		for(int i=0;i<props.length;i++) {
			String val=props[i].getProperty(key);
			if(val!=null)
				ret.add(val);
		}
		
		return ret.toArray(new String[ret.size()]);
	}
	
	private static final File getFileProperty(Properties props[], String key) {
		return getFileProperty(props, key, null);
	}
	private static final File getFileProperty(Properties props[], String key, InputStream defaultFileContent) {
		String source=getProperty(props, key);
		if(source==null)
			return null;			//Error already handled in getProperty(...) and end of static initializer block
		
		File ret = new File(source);
		
		if(ret.isDirectory()||source.endsWith("/")) {
			if (!ret.exists()) {
				if (!ret.mkdirs()) {
					Engine.registerLoadException();
					System.err.println("Failed to create config directory: " + ret.getAbsolutePath());
				}
			}
		} else {
			File parent = ret.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}
		}
		
		if(defaultFileContent!=null&&!ret.exists()) {
			try(OutputStream out=new FileOutputStream(ret)){
				ret.createNewFile();
				
				byte[] buf=new byte[1024];
				int toCopy;
				while((toCopy=defaultFileContent.read(buf))>0) {
					out.write(buf, 0, toCopy);
				}
			} catch (IOException e) {
				errors.add("Error encountered during data file creation.");
				e.printStackTrace();
			}
			
		}
		
		
		return ret;
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
	private static final boolean getBooleanProperty(Properties[] props, String key){
		String str=getProperty(props, key);
		if(str==null)return false;
		try {
			return Boolean.parseBoolean(str);
		} catch (NumberFormatException e) {
			errors.add('"'+str+"\" in property \""+key+"\" is not valid float.");
		}
		return false;
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
				
				String configDirs=prop.getProperty("configLocation");
				
				if(configDirs!=null) {
					for(String configDir:configDirs.split("\\*")) {
						File file=new File(configDir);
						
						if(file.isDirectory()||configDir.endsWith("/")) {
							if(!file.exists()) {
								if(!file.mkdirs()) {
									Engine.registerLoadException();
									System.err.println("Failed to create config directory: "+file.getCanonicalPath());
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
								if(file.getParentFile()!=null&&file.getParentFile().mkdirs()){						//File inside shani file system root
									Engine.registerLoadException();
									System.out.println(file.getParentFile());
									System.err.println("Failed to create config file: "+file.getCanonicalPath()+" cannot create parent directory.");
								} else if(!file.createNewFile()) {
									Engine.registerLoadException();
									System.err.println("Failed to create config file: "+file.getCanonicalPath()+".");
								}
								continue;
							}
							toProcess.add(file.toURI().toURL());
						}
					}
				}
			}
		}
		
		return ret.toArray(new Properties[ret.size()]);
	}
	
	//<helpers><helpers><helpers><helpers><helpers><helpers><helpers>
	/**Split all elements of array based on {@link #stringSplitRegex}.
	 * @param strs Strings to split
	 * @return Array of split strings.
	 */
	private static String[] split(String ... strs){
		ArrayList<String> ret=new ArrayList<>();
		
		for(String str:strs){
			String[] parts=stringSplitRegex.split(str);
			
			for(String part:parts)
				ret.add(part);
		}
		
		return ret.toArray(new String[ret.size()]);
	}
	
	//<values><values><values><values><values><values><values><values><values>
	public static final String language;
	
	public static final File extensionsDirectory;
	public static final String[] initFileLocation;
	
	public static final File dataFile;
	
	public static final File logsDirectory;
	
	/*Basic responses*/												//TODO move to main file
	public static final ShaniString positiveResponseKey;
	public static final ShaniString negativeResponseKey;
	
	/*Characters cost*/
	public static final byte differentCharacterCost;
	public static final byte qwertyNeighbourCost;
	public static final byte nationalSimilarityCost;						//a,ą||c,ć||...
	
	/*Character matching costs*/
	public static final short wordCompareThreshold;
	public static final Multiplier characterCompareCostMultiplier;					//Multiplier of cost for distance between short strings, val[length-1]
	public static final short characterDeletionCost;
	public static final short characterInsertionCost;
	public static final short characterSwapThreshold;
	public static final short characterSwapCost;
	
	/*Words matching cost*/
	public static final short sentenceCompareThreshold;
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
	
	public static final boolean testMode;
	public static final String[] testManifestLocation;
	
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