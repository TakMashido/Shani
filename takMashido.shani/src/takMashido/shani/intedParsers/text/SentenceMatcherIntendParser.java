package takMashido.shani.intedParsers.text;

import org.w3c.dom.Element;
import takMashido.shani.core.Intend;
import takMashido.shani.core.text.SentenceMatcher;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.intedParsers.ActionGetter;
import takMashido.shani.intedParsers.IntendParser;
import takMashido.shani.orders.Executable;
import takMashido.shani.orders.IntendParserAction;

import java.util.ArrayList;
import java.util.List;

public final class SentenceMatcherIntendParser extends IntendParser<String>{
	private SentenceMatcher sentenceMatcher;
	
	/**Create new ExecutableGetter
	 * @param e Xml element containing description of this object.
	 * @param getter ActionGetter used to get actions for Executables creation.
	 */
	public SentenceMatcherIntendParser(Element e, ActionGetter<? extends IntendParserAction<String>> getter){
		super(e,getter);
		
		sentenceMatcher=new SentenceMatcher(e);
	}
	
	@Override
	public List<Executable> getExecutables(Intend intend){
		if(!(intend.value instanceof ShaniString))
			return null;

		ShaniString str=(ShaniString)intend.value;

		var results=sentenceMatcher.process(str);
		ArrayList<Executable> Return=new ArrayList<>();
		
		for(var result:results) {
			IntendParserAction<String> action=actionGetter.getAction();

			action.init(result.getName(),result.data);
			Return.add(action.getExecutable(result.getCost(),result.getImportanceBias()));
		}
		
		return Return;
	}
}
