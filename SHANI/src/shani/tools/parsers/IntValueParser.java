package shani.tools.parsers;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shani.ShaniString;

/**Changes int value to it's representation string. Eg. 1 into "minute", 2 into "2 minutes". Based on date from initialing xml Node.
 * <br>Data are stored in following form:
 * <pre>{@code
 *<root>
 * 	<strict>
 *		<_1>godzina</_1>
 *		<_12-14>godzin</_12-14>
 *	</strict>
 *	<lastDigit>
 *		<_0-1>godzin</_0-1>
 *		<_2-4>godziny</_2-4>
 *		<_5-9>godzin</_5-9>
 *	</lastDigit>
 * </root>
 * }</pre>
 * "strict" sub tag contain replacers values exactly in given range and under "lastDigit" tag takes into account only last decimal digit of number. 
 * <br>Names of second level tags are in following form: "_number" or "_min-max". There are matching single number or numbers in given inclusive range.
 * It's text content is added after the number with one exception, if tag matches exactly one number original int value is skipped in returned string.
 * Supports also multiple replacers under one tag in {@link ShaniString} manner, with '*' between substrings.
 * <br>E.g. with given initializing node 14 passed in {@link #parse(int)} method returns "14 godzin", 1 returns "godzina" and 22 "22 godziny".
 * 
 * @author TakMashido
 */
public class IntValueParser{
	private final List<Rule> strictRules;
	private final List<Rule> lastDigitRules;
	
	private final String name;					//For debug proposes
	
	private final boolean isDump;
	
	/**Creates new IntValueParser from xml node.
	 * @param node Node containing initializing data. Described on class doc.
	 */
	public IntValueParser(Node node){
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
	
	public String parse(int value) {
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
	
	protected class Rule{
		int begin;
		int end;
		ShaniString value;
		protected boolean inRange(int value) {
			return value>=begin&&value<=end;
		}
	}
}