package shani.tools.parsers;

import org.w3c.dom.Element;

import shani.ShaniString;
import shani.Storage;;

/**Changes time into ready to print string.
 * @author TakMashido
 */
public class TimeParser{
	private static IntValueParser h;
	private static IntValueParser m;
	private static IntValueParser s;
	
	private static ShaniString timeAndWord;
	
	public static void staticInit(Element e) {
		timeAndWord=ShaniString.loadString(e,"andWord");
		
		h=new IntValueParser(Storage.getNode(e, "h"));
		m=new IntValueParser(Storage.getNode(e,"m"));
		s=new IntValueParser(Storage.getNode(e, "s"));
	}
	
	public static String parseTime(int seconds) {
		int hours=seconds/3600;
		seconds%=3600;
		int mins=seconds/60;
		seconds%=60;
		return parseTime(hours,mins,seconds);
	}
	public static String parseTime(int hours, int mins, int seconds) {
		StringBuffer ret=new StringBuffer();
		
		if(hours!=0) ret.append(h.parse(hours)).append(' ');
		if(mins!=0) ret.append(m.parse(mins)).append(' ');
		if((hours!=0||mins!=0)&&seconds!=0)ret.append(timeAndWord.toString()).append(' ');
		if(seconds!=0)ret.append(s.parse(seconds)).append(' ');
		
		ret.deleteCharAt(ret.length()-1);
		
		return ret.toString();
	}
}