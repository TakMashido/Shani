package takMashido.shani.orders;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import takMashido.shani.Engine;
import takMashido.shani.core.Intend;
import takMashido.shani.core.Storage;
import takMashido.shani.intedParsers.ActionGetter;
import takMashido.shani.intedParsers.IntendParser;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**Base class for Orders using IntendParser's as it's bridge for parsing user inputs.
 * <pre>
 * Load's itself from XML node. It's in form:
 * {@code
 * <name classname="...">					//standard part of order
 *		<parsers>							//Sub node containing all parsers
 *		 	<parser classname="...">		//Parser description, has to contain classname attribute, name of node is not important.
 *		 		<initNode/>					//Whatever provided parser need's to initialize itself.
 *		 	</parser>
 *		 </parsers>
 *		 <otherOrderInitializationNode/>
 * </name>
 * }
 * </pre>
 */
public abstract class IntendParserOrder extends Order implements ActionGetter{
	protected List<IntendParser> parsers=new ArrayList<>();
	
	public IntendParserOrder(Element e){
		super(e);
		
		Node parsersNode=Storage.getNode(e,"parsers");
		if(parsersNode==null)
			throw new IllegalArgumentException("Passed Node do not contain \"parsers\" subnode.");
		
		NodeList nodes=parsersNode.getChildNodes();
		for(int i=0;i<nodes.getLength();i++){
			if(nodes.item(i).getNodeType()!=Node.ELEMENT_NODE)
				continue;
			
			Element parserElement=(Element)nodes.item(i);
			
			String className=parserElement.getAttribute("classname");
			try{
				IntendParser parser=(IntendParser)Class.forName(className).getDeclaredConstructor(Element.class,ActionGetter.class).newInstance(parserElement,this);
				
				parsers.add(parser);
			}catch(InstantiationException|IllegalAccessException|InvocationTargetException|NoSuchMethodException|ClassNotFoundException ex){
				Engine.registerLoadException();
				System.err.println("Cannot create instance of IntendParser \""+className+'"');
				ex.printStackTrace();
			}
		}
	}
	
	@Override
	public List<Executable> getExecutables(Intend intend){
		List<Executable> ret=null;
		
		for(IntendParser parser:parsers){
			List<Executable> newExecutables=parser.getExecutables(intend);
			
			if(ret==null)
				ret=newExecutables;
			else
				ret.addAll(newExecutables);
		}
		
		return ret;
	}
}
