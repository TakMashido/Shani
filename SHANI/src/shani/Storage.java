package shani;

import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**Class for getting and saving data in Shani data files.
 * 
 * Access to data is done by String containing path to it.
 * It's form is standard java path. Names of nodes/elements divided with '.' Character.
 * 
 * @author TakMashido
 */
public class Storage {
	@Deprecated
	private static final Node mainNodeOld;
	@Deprecated
	private static final Node storageOld;
	@Deprecated
	private static final Node userdataOld;
	
	private static final Pattern divider=Pattern.compile("(?:\\s*\\.\\s*)+");
	
	static {
		mainNodeOld=Engine.doc.getElementsByTagName("shani").item(0);
		storageOld=((Element)mainNodeOld).getElementsByTagName("storage").item(0);						//Can throw error if initialized before invocing Engin.initialize
		userdataOld=((Element)mainNodeOld).getElementsByTagName("userdata").item(0);
	}
	
	/**Get nodes under given path in Storage part of file.
	 * @param path Path to super node.
	 * @return NodeList pointed by given path.
	 */
	public static NodeList getNodes(String path) {
		return getNodes(storageOld,path);
	}
	
	/**Get ShaniString under given path in Storage part of file.
	 * @param stringPath Path to Node containing wanted ShaniString.
	 * @return ShaniString pointed by path.
	 */
	public static ShaniString getString(String stringPath) {
		return getShaniString(storageOld,stringPath);
	}
	
	/**Returns node from "shani" node from mainFile pointed by provided path.
	 * @param path Path to Node
	 * @return Node from given path or null if not found.
	 */
	public static Node readNodeBase(String path) {
		NodeList nodes=getNodes(mainNodeOld,path);
		if(nodes!=null)return nodes.item(0);
		return null;
	}
	/**Read data from directly from "shani" node in mainFile. 
	 * @param path Path to data in xml file.
	 * @return String from node under provided path.
	 */
	public static String readStringBase(String path) {
		return getString(mainNodeOld,path);
	}
	/**Read data from directly from "shani" node in mainFile. 
	 * @param path Path to data in xml file.
	 * @return ShaniString from node under provided path.
	 */
	public static ShaniString readShaniStringBase(String path) {
		return getShaniString(mainNodeOld,path);
	}
	
	/**Get nodes under given path in UserData part of file.
	 * @param path Path to super node.
	 * @return NodeList pointed by given path.
	 */
	public static NodeList getUserNodes(String path) {
		return getNodes(userdataOld,path);
	}
	/**Get ShaniString under given path in UserData part of file.
	 * @param stringPath Path to Node containing wanted ShaniString.
	 * @return ShaniString pointed by path.
	 */
	public static ShaniString getUserShaniString(String stringPath) {
		var ret=getShaniString(userdataOld,stringPath);
		if(ret==null)
			return getShaniString(userData,stringPath);
		return ret;
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
		var ret=getString(userdataOld,path);
		if(ret==null)
			ret=getString(userData,path);
		
		if(ret==null)
			return null;
		return Boolean.parseBoolean(ret);
	}
	public static int getUserInt(String path) {
		return Integer.parseInt(getString(userdataOld,path));
	}
	
	@SuppressWarnings("resource")
	private static NodeList getNodes(Node where, String path) {
		Scanner scanner=new Scanner(path).useDelimiter(divider);
		NodeList Return=((Element)where).getElementsByTagName(scanner.next());
		while(scanner.hasNext()) {
			var node=(Element)Return.item(0);
			if(node==null)return null;
			Return=(node).getElementsByTagName(scanner.next());
		}
		return Return;
	}
	public static ShaniString getShaniString(Node where, String stringPath) {			//All changes here have to be made also in getString(Node,String). This methods do the same but output is different.
		var nodes=getNodes(where,stringPath);
		if(nodes==null) {
			System.err.printf("Can't find \"%s\" in %s.%n",where.getNodeName(),stringPath);
			Engine.registerLoadException();
			return null;
		}
		var node=nodes.item(0);
		if(node==null) {
			System.err.printf("Can't find \"%s\" in %s.%n",stringPath,where.getNodeName());
			Engine.registerLoadException();
			return null;
		}
		return new ShaniString(node);
	}
	public static String getString(Node where, String stringPath) {					//All changes here have to be made also in getShaniString(Node,String). This methods do the same but output is different.
		var nodes=getNodes(where,stringPath);
		if(nodes==null) {
			System.err.printf("Can't find \"%s\" in %s.%n",stringPath,where==storageOld?"storage":"userdata");
			Engine.registerLoadException();
			return null;
		}
		var node=nodes.item(0);
		if(node==null) {
			System.err.printf("Can't find \"%s\" in %s.%n",stringPath,where==storageOld?"storage":"userdata");
			Engine.registerLoadException();
			return null;
		}
		return node.getTextContent();
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
		//Write to both storages for now. After removing original one it will be written to one again.
		Node stor=createDirectory(Engine.doc,userdataOld,path);
		stor.setTextContent(data);
		
		stor=createDirectory(shaniDataDoc,shaniData,path);
		stor.setTextContent(data);
	}
	
	@SuppressWarnings("resource")
	private static Node createDirectory(Document doc, Node where,String path) {
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
	
	//<new><version><new><version><new><version><new><version><new><version><new><version>
	static {
		Document doc=null;
		Node shaniDataNode=null;
		Node storageNode=null;
		Node userDataNode=null;
		
		try {
			doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Config.dataFile);
			
			shaniDataNode=doc.getElementsByTagName("shaniData").item(0);
			storageNode=((Element)shaniDataNode).getElementsByTagName("storage").item(0);
			userDataNode=((Element)shaniDataNode).getElementsByTagName("userdata").item(0);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
			Engine.registerLoadException();
		}
		
		shaniDataDoc=doc;
		shaniData=shaniDataNode;
		storage=storageNode;
		userData=userDataNode;
	}
	
	private static final Document shaniDataDoc;
	public static final Node shaniData;
	public static final Node storage;
	public static final Node userData;
	
	public static final void save() {
		Engine.saveDocument(shaniDataDoc, Config.dataFile);
	}
}