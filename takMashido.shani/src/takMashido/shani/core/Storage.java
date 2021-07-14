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
import java.util.Scanner;
import java.util.regex.Pattern;

/**Class for getting and saving data in Shani data files.
 * 
 * Access to data is done by String containing path to it.
 * It's form is standard java path. Names of nodes/elements divided with '.' Character.
 * 
 * @author TakMashido
 */
public class Storage {
	private static final Pattern divider=Pattern.compile("(?:\\s*\\.\\s*)+");
	
	/**Get nodes under given path in Storage part of data file.
	 * Creates new nodes if node pointed by path not exists and returns it.
	 * @param path Path to super node.
	 * @return NodeList pointed by given path.
	 */
	public static NodeList getNodes(String path) {
		return getNodes(storage,path,true);
	}
	
	/**Get ShaniString under given path in Storage part of file.
	 * @param stringPath Path to Node containing wanted ShaniString.
	 * @return ShaniString pointed by path.
	 */
	public static ShaniString getString(String stringPath) {
		return getShaniString(storage, stringPath);
	}
	
	/**Get nodes under given path in UserData part of file.
	 * @param path Path to super node.
	 * @return NodeList pointed by given path.
	 */
	public static NodeList getUserNodes(String path) {
		return getNodes(userData,path);
	}
	/**Get ShaniString under given path in UserData part of file.
	 * @param stringPath Path to Node containing wanted ShaniString.
	 * @return ShaniString pointed by path.
	 */
	public static ShaniString getUserShaniString(String stringPath) {
		return getShaniString(userData,stringPath);
	}
	/**Get Boolean stored under userData node.
	 * @param path Where to search for boolean.
	 * @return Stored value or false if not found.
	 */
	public static boolean getUserBoolean(String path) {
		var ret=getUserBool(path);
		
		if(ret==null)
			return false;
		return ret;
	}
	/**Get Boolean stored under userData node, or null if not found.
	 * @param path Where to search for boolean.
	 * @return Stored value or null if not found.
	 */
	public static Boolean getUserBool(String path) {
		var ret=getString(userData,path);
		
		if(ret==null)
			return null;
		return Boolean.parseBoolean(ret);
	}
	public static int getUserInt(String path) {
		return Integer.parseInt(getString(userData,path));
	}
	
	public static NodeList getNodes(Node where, String path) {
		return getNodes(where, path, false);
	}
	@SuppressWarnings("resource")
	private static NodeList getNodes(Node where, String path, boolean createNodes) {
		Scanner scanner=new Scanner(path).useDelimiter(divider);
		
		Element previousNode;
		if(where instanceof Document) {
			previousNode = ((Document) where).getDocumentElement();
			if(!previousNode.getNodeName().equals(scanner.next())){
				if(createNodes)
					System.err.println("Creation of another root node in Document is not permitted.");
				return null;
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
				} else return null;
			} else
				previousNode=(Element)Return.item(0);
		}
		return Return;
	}
	public static Node getNode(Node where, String path) {
		return getNode(where,path,false);
	}
	private static Node getNode(Node where, String path, boolean createNodes) {
		NodeList list=getNodes(where, path, createNodes);
		if(list==null)
			return null;
		return list.item(0);
	}
	public static ShaniString getShaniString(Node where, String stringPath) {			//All changes here have to be made also in getString(Node,String). This methods do the same but output is different.
		var nodes=getNodes(where,stringPath);
		if(nodes==null||nodes.item(0)==null) {
			System.err.printf("Can't find \"%s\" in %s.%n",stringPath,where.getNodeName());
			Engine.registerLoadException();
			return null;
		}
		return new ShaniString(nodes.item(0));
	}
	public static String getString(Node where, String stringPath) {					//All changes here have to be made also in getShaniString(Node,String). This methods do the same but output is different.
		var nodes=getNodes(where,stringPath);
		if(nodes==null||nodes.item(0)==null) {
			System.err.printf("Can't find \"%s\" in %s.%n",stringPath,where.getNodeName());
			Engine.registerLoadException();
			return null;
		}
		var node=nodes.item(0);
		String str=((Element)node).getAttribute("val");
		if(!str.isEmpty())return str;
		return node.getTextContent();
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
	
	/**Delete node from given root pointed by given path. If node do not exist does nothing.
	 * @param where Root node of search.
	 * @param path Path to node to delete.
	 */
	public static void deleteNode(Node where, String path){
		Node node=getNode(where, path);
		if(node==null)
			return;
		
		node.getParentNode().removeChild(node);
	}
	
	public static void writeUserData(String path,int data) {
		writeUserData(path,Integer.toString(data));
	}
	public static void writeUserData(String path,boolean data) {
		writeUserData(path,Boolean.toString(data));
	}
	/**Writes given ShaniString under path in UserData part of file.
	 * @param path Path pointing to location in which data will be stored.
	 * @param data Data to store.
	 */
	public static void writeUserData(String path,ShaniString data) {
		writeUserData(path, data.toFullString());
	}
	/**Writes given String under path in UserData part of file.
	 * @param path Path pointing to location in which data will be stored.
	 * @param data Data to store.
	 */
	public static void writeUserData(String path,String data) {
		Element stor=(Element)createDirectory(shaniDataDoc,userData,path);
		stor.setAttribute("val", data);
	}
	
	@SuppressWarnings("resource")
	private static Node createDirectory(Document doc, Node where, String path) {
		Scanner scanner=new Scanner(path).useDelimiter(divider);
		String nodeName;
		Node parentNode=where;
		Node Return = null;
		while(scanner.hasNext()) {
			nodeName=scanner.next();
			Return=((Element)where).getElementsByTagName(nodeName).item(0);
			if(Return==null) {
				Return=doc.createElement(nodeName);
				parentNode.appendChild(Return);
				parentNode=Return;
				while(scanner.hasNext()) {
					nodeName=scanner.next();
					Return=doc.createElement(nodeName);
					parentNode.appendChild(Return);
					parentNode=Return;
				}
				return Return;
			} else {
				parentNode=Return;
			}
		}
		return Return;
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
	
	private static final Document shaniDataDoc;
	public static final Node shaniData;
	public static final Node storage;
	public static final Node userData;
	public static final Node ordersData;
	
	public static final void save() {
		Engine.saveDocument(shaniDataDoc, Config.dataFile);
	}
}