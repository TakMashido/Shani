package shani.tools;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shani.ShaniString;
import shani.Storage;;

/**Contain methods to parse diffrend forms of data to ready to display String.
 * @author TakMashido
 */
public class Parsers {
	private static IntValueParser h;
	private static IntValueParser m;
	private static IntValueParser s;
	
	private static final ShaniString timeAndWord=Storage.readShaniStringBase("parsers.TimeParser.andWord");
	
	public static String parseTime(int seconds) {
		int hours=seconds/3600;
		seconds%=3600;
		int mins=seconds/60;
		seconds%=60;
		return parseTime(hours,mins,seconds);
	}
	public static String parseTime(int hours, int mins, int seconds) {
		if(h==null)h=new IntValueParser(Storage.readNodeBase("parsers.TimeParser.h"));
		if(m==null)m=new IntValueParser(Storage.readNodeBase("parsers.TimeParser.m"));
		if(s==null)s=new IntValueParser(Storage.readNodeBase("parsers.TimeParser.s"));
		StringBuffer ret=new StringBuffer();
		
		if(hours!=0) ret.append(h.parse(hours)).append(' ');
		if(mins!=0) ret.append(m.parse(mins)).append(' ');
		if((hours!=0||mins!=0)&&seconds!=0)ret.append(timeAndWord.toString()).append(' ');
		if(seconds!=0)ret.append(s.parse(seconds)).append(' ');
		
		ret.deleteCharAt(ret.length()-1);
		
		return ret.toString();
	}
	
	private static class IntValueParser{
		private final List<Rule> strictRules;
		private final List<Rule> lastDigitRules;
		
		private final String name;					//For debug propouses
		
		private final boolean isDump;
		
		IntValueParser(Node node){
			if(node==null) {
				isDump=true;
				name="dump";
				strictRules=null;
				lastDigitRules=null;
				System.err.println("Failed to load ParsersIntValueParser: null provided");
				assert false;
				return;
			}
			name=node.getNodeName();
			
			strictRules=parseRules(((Element)node).getElementsByTagName("strict").item(0));
			lastDigitRules=parseRules(((Element)node).getElementsByTagName("lastDigit").item(0));
			
			isDump=strictRules==null||lastDigitRules==null;
			if(isDump) {
				System.err.println("Failed to load Parsers.IntValueParser "+name+". Error in one or more rule set.");
				assert false;
			}
		}
		private List<Rule> parseRules(Node node){
			if(node==null)return null;
			ArrayList<Rule> rules=new ArrayList<Rule>();
			NodeList ruleList=node.getChildNodes();
			for(int i=0;i<ruleList.getLength();i++) {
				Node nod=ruleList.item(i);
				if(!(nod.getNodeType()==Node.ELEMENT_NODE))continue;
				String range=nod.getNodeName().substring(1);
				nod.getTextContent();
				
				Rule rule=new Rule();
				
				if(range.contains("-")) {
					String[] ranges=range.split("-");
					if(ranges.length!=2) {
						System.err.println("Error during parsing \""+range+"\" range in Parsers.ValueParser");
						continue;
					}
					
					try {
						rule.begin=Integer.parseInt(ranges[0]);
						rule.end=Integer.parseInt(ranges[1]);
					} catch(NumberFormatException ex) {
						System.err.println("Error during parsing \""+range+"\" range in Parsers.ValueParser: NumberFormatException");
						continue;
					}
				} else {
					try {
						rule.begin=rule.end=Integer.parseInt(range);
					} catch(NumberFormatException ex) {
						System.err.println("Error during parsing \""+range+"\" range in Parsers.ValueParser: NumberFormatException");
						continue;
					}
				}
				
				rule.value=new ShaniString(nod.getTextContent());
				rules.add(rule);
			}
			rules.trimToSize();
			return rules;
		}
		
		private String parse(int value) {
			if(isDump)return Integer.toString(value);
			
			for(Rule rule:strictRules) {
				if(rule.inRange(value)) {
					if(rule.begin==rule.end)return rule.value.toString();
					else return value+" "+rule.value.toString();
				}
			}
			int valueLastDigit=value%10;
			for(Rule rule:lastDigitRules) {
				if(rule.inRange(valueLastDigit)) {
					return value+" "+rule.value.toString();
				}
			}
			
			System.err.println(name+" IntValueParser parser can't parse "+value);
			return Integer.toString(value);
		}
		
		private class Rule{
			int begin;
			int end;
			ShaniString value;
			private boolean inRange(int value) {
				return value>=begin&&value<=end;
			}
		}
	}
}