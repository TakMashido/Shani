package shani;

public class Tools {
	public static char[] stem(String dat) {
		return dat.trim().toLowerCase().toCharArray();
	}
	public static String clear(String dat) {
		dat=dat.trim();
		
		if(dat.endsWith("\""))dat=dat.substring(0, dat.length());
		if(dat.startsWith("\""))dat=dat.substring(1, dat.length()-1);
		
		return dat;
	}
	/**
	 * Removes national characters from given string and repleace them with english representatives.
	 * @param com String being procesed
	 * @return com without national specyfic characters
	 */
	public static String removeNational(String com) {
		com.replaceAll("�", "a");
		com.replaceAll("�", "c");
		com.replaceAll("�", "e");
		com.replaceAll("�", "l");
		com.replaceAll("�", "n");
		com.replaceAll("�", "o");
		com.replaceAll("�", "s");
		com.replaceAll("[��]", "z");
		return com;
	}
	
	public static boolean isNegativeAnswer(ShaniString ans) {
		if(ans.equals("nie"))return true;
		else return false;
	}
	public static boolean isNegativeAnswer(String ans) {
		return isNegativeAnswer(new ShaniString(ans));
	}
}