package takMashido.shani.orders;

import java.util.Map;

/**Action to use with IntendMatcherOrder.
 * Contain additional methods to allow passing data from IntendMatching System.
 * @param <T> Type of expected data. E.g. for action intended to work only with text - String.
 */
public abstract class IntendParserAction<T> extends Action{
	/**Name of Intend match, used to determine what exactly has to be done.*/
	protected String name;
	/**Contain parameters for execution isolated by ExecutableGetter or it's underlying interpreter.*/
	protected Map<String,? extends T> parameters;

	/**Used by ExecutableGetters to inject information provided by user in Intend.
	 * @param name Name of Intend match, used to determine what exactly has to be done.
	 * @param parameters Parameters with additional information about what to execute.
	 */
	public void init(String name, Map<String,? extends T> parameters){
		this.name=name;
		this.parameters=parameters;
	}

	/**Create stringHash of this Action.
	 * It's default way of creating string hash for IntendParserAction.
	 * You can return it directly in {@link #hashString()} unless there are other fields storing Action state.
	 * @return Default hash of this action.
	 */
	protected String getBaseStringHash(){
		return name+":"+parameters.toString();
	}
}
