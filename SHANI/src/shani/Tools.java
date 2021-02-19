package shani;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Tools {
	private static String[] suffix;
	private static String[] replacement;
	
	public static void staticInit(Element e) {
		NodeList nodes=e.getElementsByTagName("tag");
		
		suffix=new String[nodes.getLength()];
		replacement=new String[nodes.getLength()];
		
		for(int i=0;i<suffix.length;i++) {
			Element elem=(Element)nodes.item(i);
			suffix[i]=elem.getAttribute("suf");
			replacement[i]=elem.getAttribute("rep");
		}
	}
	
	public static String stem(String dat) {
		for(int i=0;i<suffix.length;i++) {
			if(dat.endsWith(suffix[i])) {
				return dat.substring(0,dat.length()-suffix[i].length())+replacement[i];
			}
		}
		
		return dat;
	}
	public static String clear(String dat) {
		dat=dat.trim();
		
		
		if(dat.endsWith("\"")&&dat.startsWith("\""))dat=dat.substring(1, dat.length()-1);
		
		return dat;
	}
	/**
	 * Removes national characters from given string and repleace them with english representatives.
	 * @param com String being procesed
	 * @return com without national specyfic characters
	 * Deprecated.
	 * Config.nationalSimilarityCost allows ShaniMatching to handle this without problems. 
	 */
	@Deprecated
	public static String removeNational(String com) {
		com.replaceAll("¹", "a");
		com.replaceAll("æ", "c");
		com.replaceAll("ê", "e");
		com.replaceAll("³", "l");
		com.replaceAll("ñ", "n");
		com.replaceAll("ó", "o");
		com.replaceAll("œ", "s");
		com.replaceAll("[¿Ÿ]", "z");
		return com;
	}
	
	/**Check if ans value stands for confirmation 
	 * @param ans Value to be checked.
	 * @return If ans represents confirmation message.
	 * @deprecated It's  dublication of {@link Engine#isInputPositive(ShaniString)}. Use it instead
	 */
	@Deprecated
	public static boolean isNegativeAnswer(ShaniString ans) {
		Boolean ret=Engine.isInputPositive(ans);
		if(ret==null)return false;
		return !ret;
	}
	/**Check if ans value stands for confirmation 
	 * @param ans Value to be checked.
	 * @return If ans represents confirmation message.
	 * @deprecated It's  dublication of {@link Engine#isInputPositive(ShaniString)}. Use it instead
	 */
	@Deprecated
	public static boolean isNegativeAnswer(String ans) {
		return isNegativeAnswer(new ShaniString(ans));
	}
}