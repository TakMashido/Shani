package takMashido.shani.intedParsers.text;

import org.w3c.dom.Element;
import takMashido.shani.core.Intend;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.core.text.ShaniString.ShaniMatcher;
import takMashido.shani.intedParsers.ActionGetter;
import takMashido.shani.intedParsers.IntendParser;
import takMashido.shani.orders.Action;
import takMashido.shani.orders.Executable;

import java.util.List;
import java.util.Map;

public final class KeywordIntendParser extends IntendParser{
	/**Name of this keyword matcher.*/
	private String name;
	/**Keyword to search for.*/
	private ShaniString keyword;
	
	/**Create new ExecutableGetter
	 * @param element Xml Node used to create this parser.
	 * @param getter  ActionGetter used to get actions for Executables creation.
	 */
	public KeywordIntendParser(Element element, ActionGetter getter){
		super(element, getter);
		
		keyword=ShaniString.loadString(element,"keyword");
	}
	
	@Override
	public List<Executable> getExecutables(Intend intend){
		if(!(intend.value instanceof ShaniString))
			return null;
		
		ShaniMatcher matcher=((ShaniString)intend.value).getMatcher().apply(keyword);
		if(!matcher.isSemiEqual())
			return null;
		
		Action action=actionGetter.getAction();
		action.init(name,Map.of("unmatched",matcher.getUnmatched()));
		
		return List.of(action.getExecutable(matcher.getMatchedCost()));
	}
}
