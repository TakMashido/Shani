package takMashido.shani.intedParsers.text;

import org.w3c.dom.Element;
import takMashido.shani.core.Config;
import takMashido.shani.core.Intend;
import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.core.text.ShaniString.ShaniMatcher;
import takMashido.shani.intedParsers.ActionGetter;
import takMashido.shani.intedParsers.IntendParser;
import takMashido.shani.orders.Executable;
import takMashido.shani.orders.IntendParserAction;

import java.util.List;
import java.util.Map;

/**Uses simple keyword search as intend parsing method.
 * It's stored as XML subnode named "keyword" being normal ShaniString node.
 */
public final class KeywordIntendParser extends IntendParser<ShaniString>{
	/**Name of this keyword matcher.*/
	private String name;
	/**Keyword to search for.*/
	private ShaniString keyword;
	
	/**Create new ExecutableGetter
	 * @param element Xml Node used to create this parser.
	 * @param getter  ActionGetter used to get actions for Executables creation.
	 */
	public KeywordIntendParser(Element element, ActionGetter<? extends IntendParserAction<ShaniString>> getter){
		super(element, getter);
		
		keyword=ShaniString.loadString(element,"keyword");
		if(keyword==null){
			ShaniCore.registerLoadException("Can't find KeywordIntendParser \""+name+"\" keyword.");
			keyword=new ShaniString("");
		}
	}
	
	@Override
	public List<Executable> getExecutables(Intend intend){
		if(!(intend.value instanceof ShaniString))
			return null;
		
		ShaniMatcher matcher=((ShaniString)intend.value).getMatcher().apply(keyword);
		if(!matcher.isSemiEqual())
			return null;

		IntendParserAction<ShaniString> action=actionGetter.getAction();
		ShaniString unmatched=matcher.getUnmatched();
		action.init(name,Map.of("unmatched",unmatched));
		
		String unmatchedString=unmatched.toString();
		int unmatchedWords=0;
		boolean word=false;				//If previous index was word part
		for(int i=0;i<unmatchedString.length();i++){
			if(Character.isWhitespace(unmatchedString.charAt(i))){
				word=false;
			} else {
				if(!word)
					unmatchedWords++;
				word=true;
			}
		}
		
		short cost=(short)(matcher.getMatchedCost()+unmatchedWords*Config.wordSemimatchCost);
		return List.of(action.getExecutable(cost<Config.sentenceCompareThreshold?cost:Config.sentenceCompareThreshold-1));
	}
}
