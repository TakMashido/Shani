package takMashido.shani.intedParsers;

import org.w3c.dom.Element;
import takMashido.shani.core.Intend;
import takMashido.shani.orders.Executable;
import takMashido.shani.orders.IntendParserAction;

import java.util.List;

/**Executable getters are classes responsible for interpreting {@link Intend Intend's}, and adding interpretation data into Action.
 * The should contain object performing interpretation and use results to create list of Executables.
 * @param <T> Type of data returned by this IntendParser subclass.
 */
public abstract class IntendParser<T>{
	protected ActionGetter<? extends IntendParserAction<T>> actionGetter;
	
	/**Create new ExecutableGetter
	 * @param element Xml Node used to create this parser.
	 * @param getter ActionGetter used to get actions for Executables creation.
	 */
	public IntendParser(Element element, ActionGetter<? extends IntendParserAction<T>> getter){
		actionGetter=getter;
	}
	
	/**Get executables matching this Intend.
	 * @param intend {@link Intend} to interpret.
	 * @return List of matching executables.
	 */
	public abstract List<Executable> getExecutables(Intend intend);
}
