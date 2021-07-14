package takMashido.shani.orders.targetAction;

import org.w3c.dom.Element;
import takMashido.shani.libraries.Pair;

import java.util.Map;

/**Class being target for TargetAction.*/
public interface Target{
	/**Get similarity between this target and data returned by IntendParser.
	 * @param actionName Action name returned by IntendParser
	 * @param parameters Parameters of Action extracted from Intend by IntendParser.
	 * @return Distance from this Target and user input in form Pair(cost,importanceBias)
	 */
	Pair<Short,Short> getSimilarity(String actionName, Map<String,?> parameters);
	
	/**Set XML element being location of this Target data.
	 * @param e Look above.
	*/
	void setSaveElement(Element e);
	
	/**Override to save targets into file.*/
	void save();
}
