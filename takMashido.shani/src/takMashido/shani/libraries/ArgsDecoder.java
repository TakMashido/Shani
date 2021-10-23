package takMashido.shani.libraries;

import java.util.ArrayList;

/**Simple engine for procesing args.
 * @author TakMashido
 */
public class ArgsDecoder {
	private String[] args;
	private boolean[] used;
	
	/**Creates new ArgsDecoder object.
	 * @param args Args which will be processed by this Object.
	 */
	public ArgsDecoder(String... args) {
		this.args=args;
		used=new boolean[args.length];
	}
	
	/**Gets index of flag occurence and marks it as used.
	 * @param flag Arrays of flags to search for.
	 * @return Index of first flag occurence or -1 if not found.
	 */
	private int getFlagIndex(String... flag) {
		for(int i=0;i<args.length;i++) {
			for(int j=0;j<flag.length;j++) {
				if(args[i].equals(flag[j])&!used[i]) {
					used[i]=true;
					return i;
				}
			}
		}
		return -1;
	}
	
	/**Gets string value marked with specyfic flag.
	 * @param flag Flag to search for.
	 * @return First word after flag ocurence or empty string if flag not found.
	 */
	public String getString(String... flag) {
		int index=getFlagIndex(flag);
		if(index==-1||++index==args.length||used[index])return null;
		used[index]=true;
		return args[index];
	}
	/**Gets integer value marked with specyfic flag
	 * @param flag Flag to search for.
	 * @return Integer following given flag or null if flag not found.
	 */
	public Integer getInt(String...flag) {
		String val=getString(flag);
		if(val==null)return null;
		return Integer.parseInt(val);
	}
	/**Gets float value marked with specyfic flag.
	 * @param flag Flag to search for.
	 * @return Float following given flag or null if flag not found.
	 */
	public Float getFloat(String...flag) {
		String val=getString(flag);
		if(val==null)return null;
		return Float.parseFloat(val);
	}
	/**Check if given args have specyfic flag included.
	 * @param flag Flag to search for. For exemple "-v","-ea".
	 * @return If args conatain one of given flags.
	 */
	public boolean containFlag(String... flag) {
		return getFlagIndex(flag)!=-1;
	}
	
	/**Chech if all input parameters was used.
	 * @return If all args was used.
	 */
	public boolean isProcesed() {
		for(var bool:used) if(!bool)return false;
		return true;
	}
	
	/**Returns array of unused args.
	 * @return Look above.
	 */
	public String[] getUnprocesed() {
		ArrayList<String> Return=new ArrayList<String>();
		
		for(int i=0;i<args.length;i++) {
			if(!used[i])Return.add(args[i]);
		}
		
		return Return.toArray(new String[0]);
	}
}