package takMashido.shani.core;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import takMashido.shani.Engine;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.orders.Order;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Scanner;
import java.util.regex.Pattern;

/**Class for getting and saving data in Shani data files.
 * 
 * Access to data is done by String containing path to it.
 * It's form is standard java path. Names of nodes/elements divided with '.' Character. Empty String is marking same as current node.
 * E.g "takMashido.shaniModules.orders.TimerOrder.timers.target" is going to search takMashido XML element in root node.
 * If more than one is present the first one is taken an "shaniModules" is searched under it,
 * and continues in same fashion until if fails to find element with given name, or path is fully processed and returns the last node/NodeList found.
 * Effectively it works similar to command line cd command by directories are separated with "." instead of "/" and there can be multiple directories with same name(as stated by XML standard)
 * 
 * @author TakMashido
 */
public class Storage {
	private static final Pattern divider=Pattern.compile("(?:\\s*\\.\\s*)+");

	/**Get xml nodes under given node pointed by path.
	 * @param where Root node of search
	 * @param path Path to search for.
	 * @return List of nodes pointed by given path.
	 */
	public static NodeList getNodes(Node where, String path) {
		return getNodes(where, path, false);
	}
	/**Get xml nodes under given node pointed by path. Can also create nodes if necessary.
	 * @param where Root node of search.
	 * @param path Path to search for.
	 * @param createNodes If create new nodes if no matching the path was found during search.
	 * @return List of nodes pointed by given path.
	 */
	@SuppressWarnings("resource")
	public static NodeList getNodes(Node where, String path, boolean createNodes) {
		Scanner scanner=new Scanner(path).useDelimiter(divider);

		if(path.isEmpty())
			return new NodeList() {
				@Override
				public Node item(int i) {
					if(i==0)
						return where;
					else
						return null;
				}

				@Override
				public int getLength() {
					return 1;
				}
			};

		Element previousNode;
		if(where instanceof Document) {
			previousNode = ((Document) where).getDocumentElement();
			if(!previousNode.getNodeName().equals(scanner.next())){
				if(createNodes)
					throw new NodeNotPresentException(where, path, "Creation of another root node in Document is not permitted.");
				throw new NodeNotPresentException(where, path);
			}
		}else
			previousNode=(Element) where;
		
		NodeList Return=null;
		while(scanner.hasNext()) {
			String nextNodeName=scanner.next();
			
			Return=previousNode.getElementsByTagName(nextNodeName);
			
			if(Return.getLength()==0) {								//Node not found
				if(createNodes) {
					Element newNode=previousNode.getOwnerDocument().createElement(nextNodeName);
					previousNode.appendChild(newNode);
					
					Return=previousNode.getElementsByTagName(nextNodeName);
					previousNode=newNode;
				} else {
					throw new NodeNotPresentException(where,path);
				}
			} else
				previousNode=(Element)Return.item(0);
		}
		return Return;
	}
	/**Get xml node under given node pointed by path.
	 * @param where Root node of search.
	 * @param path Path to search for.
	 * @return List of nodes pointed by given path.
	 */
	public static Node getNode(Node where, String path) {
		return getNode(where,path,false);
	}
	/**Get xml node under given node pointed by path. Can also create nodes if necessary.
	 * @param where Root node of search.
	 * @param path Path to search for.
	 * @param createNodes If create new nodes if no matching the path was found during search.
	 * @return List of nodes pointed by given path.
	 */
	public static Node getNode(Node where, String path, boolean createNodes) {
		NodeList list=getNodes(where, path, createNodes);

		return list.item(0);
	}

	/**Create new xml element.
	 * It's not accepting path like most functions. Only name of Element.
	 * @param where Where to create new Element.
	 * @param name Name of new XML Element. Note that is not path and can't contain '.' character.
	 * @return New Element.
	 */
	public static Element createElement(Node where, String name){
		if(name.contains("."))
				throw new IllegalArgumentException("Name parameter value is \"%s\". It contain invalid '.'.");

		Element ret=where.getOwnerDocument().createElement(name);
		where.appendChild(ret);
		return ret;
	}

	/**Load shaniString from node pointed by path under where root node.
	 * @param where Root node of search.
	 * @param stringPath Path of node to initialize ShaniString from.
	 * @return {@link ShaniString} object described by pointed xml node.
	 */
	public static ShaniString getShaniString(Node where, String stringPath) {			//All changes here have to be made also in getString(Node,String). This methods do the same but output is different.
		var nodes=getNodes(where,stringPath);

		return new ShaniString(nodes.item(0));
	}
	/**Load ShaniString from node pointed by path under where root node.
	 * @param where Root node of search.
	 * @param stringPath Path of node to initialize ShaniString from.
	 * @return {@link ShaniString} object stored in pointed xml node.
	 */
	public static String getString(Node where, String stringPath) {					//All changes here have to be made also in getShaniString(Node,String). This methods do the same but output is different.
		var nodes=getNodes(where,stringPath);

		var node=nodes.item(0);
		String str=((Element)node).getAttribute("val");
		if(!str.isEmpty())return str;
		return node.getTextContent();
	}
	/**Load IntendBase from node pointed by path under where root node.
	 * @param where Root node of search.
	 * @param stringPath Path of node to initialize ShaniString from.
	 * @return {@link IntendBase} object stored in pointed xml node.
	 */
	public static IntendBase getIntendBase(Node where, String stringPath) throws ReflectionLoadException {
		Element elem=(Element)getNode(where, stringPath);

		IntendBase loader= null;
		try {
			loader = (IntendBase)(Class.forName(elem.getAttribute("classname")).getDeclaredConstructor().newInstance());
		} catch (InstantiationException|IllegalAccessException|InvocationTargetException|
				NoSuchMethodException|ClassNotFoundException e) {
			throw new ReflectionLoadException("Refrecion exception occured during loading",e);
		}
		return loader.loadNew(elem);
	}

	/**Write string into pointed node.
	 * @param where Root node of provided path.
	 * @param path Path to node where string will be saved.
	 * @param value Value to save.*/
	public static void writeString(Node where, String path, String value){
		Element location=(Element)getNode(where,path,true);
		
		location.setAttribute("val",value);
	}
	/**Write integer into pointed node.
	 * @param where Root node of provided path.
	 * @param path Path to node where integer will be saved.
	 * @param value Value to save.*/
	public static void writeInt(Node where, String path, int value){
		writeString(where,path,Integer.toString(value));
	}
	/**Write boolean into pointed node.
	 * @param where Root node of provided path.
	 * @param path Path to node where boolean will be saved.
	 * @param value Value to save.*/
	public static void writeBool(Node where, String path, boolean value){
		writeString(where, path, Boolean.toString(value));
	}
	/**Write IntendBase object under specified path.
	 * @param where Node under which start search.
	 * @param stringPath Where to write intendBase.
	 * @param intend IntendBase to write.
	 */
	public static void writeIntendBase(Node where, String stringPath, IntendBase intend){
		Element target=(Element)getNode(where, stringPath, true);
		target.setAttribute("classname", intend.getClass().getCanonicalName());
		intend.save(target);
	}

	/**Delete node from given root pointed by given path. If node do not exist does nothing.
	 * @param where Root node of search.
	 * @param path Path to node to delete.
	 */
	public static void deleteNode(Node where, String path){
		Node node=getNode(where, path);

		node.getParentNode().removeChild(node);
	}
	
	public static Node getOrderData(Order order) {
		return getNodes(ordersData, order.getClass().getCanonicalName(),true).item(0);
	}

	/**Get Boolean stored under userData node, or null if not found.
	 * @param where Root node of search.
	 * @param path Where to search for boolean.
	 * @return Stored value or null if not found.
	 */
	public static Integer getInt(Node where,String path) {
		String val=getString(where,path);
		
		if(val==null)return null;
		return Integer.valueOf(val);
	}
	/**Get Boolean stored under userData node, or null if not found.
	 * @param where Root node of search.
	 * @param path Where to search for boolean.
	 * @return Stored value or null if not found.
	 */
	public static Boolean getBool(Node where, String path) {
		var ret=getString(where,path);
		
		if(ret==null)return null;
		return Boolean.parseBoolean(ret);
	}
	
	//<new><version><new><version><new><version><new><version><new><version><new><version>
	static {
		Document doc=null;
		Node shaniDataNode=null;
		Node storageNode=null;
		Node userDataNode=null;
		Node ordersDataNode=null;
		
		try {
			doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Config.dataFile);
			
			shaniDataNode=doc.getElementsByTagName("shaniData").item(0);
			storageNode=((Element)shaniDataNode).getElementsByTagName("storage").item(0);
			userDataNode=((Element)storageNode).getElementsByTagName("userdata").item(0);
			ordersDataNode=((Element)shaniDataNode).getElementsByTagName("ordersData").item(0);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
			Engine.registerLoadException();
		}
		
		assert doc!=null:"ShaniData doc can't be load.";
		assert shaniDataNode!=null:"shaniData node from data doc can't be load.";
		assert storageNode!=null:"storage node from data doc can't be load.";
		assert userDataNode!=null:"userData node from data doc can't be load.";
		assert ordersDataNode!=null:"ordersData node from data doc can't be load.";
		
		shaniDataDoc=doc;
		shaniData=shaniDataNode;
		storage=storageNode;
		userData=userDataNode;
		ordersData=ordersDataNode;
	}

	/**Document object of shaniData file.*/
	private static final Document shaniDataDoc;
	/**Main node of shaniData file.*/
	public static final Node shaniData;
	/**Root of general storage nodes containing other data nodes.*/
	public static final Node storage;
	/**Node containing data describing user.*/
	public static final Node userData;
	/**Node containing orders data.*/
	public static final Node ordersData;

	/**Save dataFile changes to file.*/
	public static final void save() {
		Engine.saveDocument(shaniDataDoc, Config.dataFile);
	}

	/**Exception indicating that requested node was not found.*/
	public static class NodeNotPresentException extends RuntimeException{
		private NodeNotPresentException(Node where, String path){
			super(String.format("Can't find \"%s\" in node %s.",path,where.getNodeName()));
		}
		private NodeNotPresentException(Node where, String path, String anotherMessage){
			super(String.format("Can't find \"%s\" in node %s. %s",path,where.getNodeName(),anotherMessage));
		}
	}
	/**Exception indicating that something bad happened in reflection code responsible for loading.
	 * It's more a wrapper for exception so only single instance will be thrown to client code instead of possibility of full set of reflection related Exception.
	 */
	public static class ReflectionLoadException extends RuntimeException{
		private ReflectionLoadException(String message, Exception cause){
			super(message,cause);
		}
	}
}