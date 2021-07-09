package takMashido.shani.intedParsers;

import org.w3c.dom.Element;
import takMashido.shani.core.Intend;
import takMashido.shani.orders.Executable;

import java.util.List;

/**Executable getters are classes responsible for interpreting {@link Intend Intend's}, and adding interpretation data into Action.
 * The should contain object performing interpretation and use results to create list of Executables.*/
public abstract class IntendParser{
	protected ActionGetter actionGetter;
	
	/**Create new ExecutableGetter
	 * @param element Xml Node used to create this parser.
	 * @param getter ActionGetter used to get actions for Executables creation.
	 */
	public IntendParser(Element element, ActionGetter getter){
		actionGetter=getter;
	}
	
	/**Get executables matching this Intend.
	 * @param intend {@link Intend} to interpret.
	 * @return List of matching executables.
	 */
	public abstract List<Executable> getExecutables(Intend intend);
}
