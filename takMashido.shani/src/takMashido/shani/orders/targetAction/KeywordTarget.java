package takMashido.shani.orders.targetAction;

import org.w3c.dom.Element;
import takMashido.shani.core.Config;
import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.libraries.Pair;

import java.util.Map;

public abstract class KeywordTarget implements Target{
	/**Results to return by getSimilarity if required keyword do not exist in parameters map, or is wrong type.*/
	private static final Pair<Short,Short> NO_PARAMETER_RESULT=new Pair<>(Config.wordInsertionCost,(short)0);
	public static final String DEFAULT_PARAMETER_NAME="unmatched";
	
	public ShaniString keyword;
	public String parameterName;
	
	protected Element saveLocation;
	
	/**Create new Keyword target using given keyword.
	 * @param keyword Keyword representing this target.
	 */
	public KeywordTarget(ShaniString keyword){
		this(DEFAULT_PARAMETER_NAME,keyword);
	}
	/**Create new Keyword target using given keyword.
	 * @param parameterName Name of parameter in parameters map compared with keyword.
	 * @param keyword Keyword representing this target.
	 */
	public KeywordTarget(String parameterName, ShaniString keyword){
		this.keyword=keyword;
		this.parameterName=parameterName;
	}
	/**Load keyword target from XML file.
	 * @param e XML element containing this KeywordTarget data.
	 */
	public KeywordTarget(Element e){
		this(DEFAULT_PARAMETER_NAME, e);
	}
	/**Load keyword target from XML file.
	 * @param parameterName Name of parameter in parameters map compared with keyword.
	 * @param e XML element containing this KeywordTarget data.
	 */
	public KeywordTarget(String parameterName, Element e){
		saveLocation=e;
		this.parameterName=parameterName;
		
		keyword=ShaniString.loadString(e,"keyword");
	}
	
	@Override
	public void setSaveElement(Element e){
		assert saveLocation==null:"This method should be only executed once.";
		
		saveLocation=e;
		
		Element keywordNode=saveLocation.getOwnerDocument().createElement("keyword");
		saveLocation.appendChild(keywordNode);
		keyword.setNode(keywordNode);
	}
	@Override
	public void save(){}
	
	@Override
	public Pair<Short,Short> getSimilarity(String actionName, Map<String,?> parameters){
		if(!parameters.containsKey(parameterName)){
			ShaniCore.errorOccurred("parameterName \""+parameterName+"\" not found in Target "+this.getClass().getName()+" with keyword \""+keyword.toFullString()+'"');
			return NO_PARAMETER_RESULT;
		}
		
		Object parameter=parameters.get(parameterName);
		ShaniString comparable=null;
		if(parameter instanceof ShaniString)
			comparable=(ShaniString)parameter;
		else if(parameter instanceof String)
			comparable=new ShaniString(ShaniString.ParseMode.raw,(String)parameter);
		if(comparable==null)
			return NO_PARAMETER_RESULT;
		
		ShaniString.ShaniMatcher matcher=comparable.getMatcher().apply(keyword);
		return new Pair<>(matcher.getCost(),(short)0);
	}
}
