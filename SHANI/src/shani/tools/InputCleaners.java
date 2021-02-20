package shani.tools;

import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import shani.Storage;

public class InputCleaners {
	private static String[] stemSuffix;
	private static String[] stemReplacement;
	
	private static char[] nationalChar;
	private static char[] nationalReplacement;
	
	public static void staticInit(Element e) {
		//<StemInit><StemInit><StemInit><StemInit><StemInit>
		NodeList nodes=Storage.getNodes(e, "stem.tag");
		
		stemSuffix=new String[nodes.getLength()];
		stemReplacement=new String[nodes.getLength()];
		
		for(int i=0;i<stemSuffix.length;i++) {
			Element elem=(Element)nodes.item(i);
			stemSuffix[i]=elem.getAttribute("suf");
			stemReplacement[i]=elem.getAttribute("rep");
		}
		
		//<NationalReplacementInit><NationalReplacementInit>
		nodes=Storage.getNodes(e, "nationalReplacement.tag");
		
		nationalChar=new char[nodes.getLength()];
		nationalReplacement=new char[nodes.getLength()];
		
		for(int i=0;i<stemSuffix.length;i++) {
			Element elem=(Element)nodes.item(i);
			nationalChar[i]=elem.getAttribute("let").charAt(0);
			nationalReplacement[i]=elem.getAttribute("rep").charAt(0);
		}
	}
	
	/**Perform primitive stemming by replacing suffixes with others. All white char chains are replaced with single space.
	 * @param dat String to stem
	 * @return string with suffixes replaces on all words*/
	public static String stem(String dat) {
		final Pattern whiteSplitRegex=Pattern.compile("\\w");
		
		String[] words=whiteSplitRegex.split(dat);
		words:
		for(int w=0;w<words.length;w++) {
			for(int i=0;i<stemSuffix.length;i++) {
				if(words[w].endsWith(stemSuffix[i])) {
					words[w]=words[w].substring(0,dat.length()-stemSuffix[i].length())+stemReplacement[i];
					continue words;
				}
			}
		}
		
		StringBuilder ret=new StringBuilder();
		ret.append(words[0]);
		for(int i=1;i<words.length;i++)
			ret.append(' ').append(words[i]);
		
		return ret.toString();
	}
	public static String clear(String dat) {
		dat=dat.trim();
		
		if(dat.endsWith("\"")&&dat.startsWith("\""))dat=dat.substring(1, dat.length()-1);
		
		return dat;
	}
	/**
	 * Removes national characters from given string and replace them with English representatives.
	 * @param str String being processed
	 * @return str without national specific characters
	 */
	public static String removeNational(String str) {
		for(int i=0;i<nationalChar.length;i++)
			str=str.replace(nationalChar[i],nationalReplacement[i]);
		
		return str;
	}
}