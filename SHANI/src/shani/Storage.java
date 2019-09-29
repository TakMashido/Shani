package shani;

import java.util.Scanner;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**Class for getting and saving data in Shani data files.
 * 
 * Access to data is done by String containg path to it.
 * It's form is standard java path. Names of nodes/elements divided with '.' Character.
 * 
 * @author TakMashido
 */
public class Storage {
	private static final Node mainNode;
	private static final Node storage;
	private static final Node userdata;
	
	private static final Pattern divider=Pattern.compile("(?:\\s*\\.\\s*)+");
	
	static {
		mainNode=Engine.doc.getElementsByTagName("shani").item(0);
		storage=((Element)mainNode).getElementsByTagName("storage").item(0);						//Can throw error if initialized before invocing Engin.initialize
		userdata=((Element)mainNode).getElementsByTagName("userdata").item(0);
	}
	
	private static boolean isErrorOccured=false;
	public static boolean isErrorOccured() {return isErrorOccured;}
	
	/**Get nodes under given path in Storage part of file.
	 * @param path Path to super node.
	 * @return NodeList poinded by given path.
	 */
	public static NodeList getNodes(String path) {
		return getNodes(storage,path);
	}
	
	/**Get ShaniString under given path in Storage part of file.
	 * @param stringPath Path to Node conataing wanted ShaniString.
	 * @return ShaniString pointed by path.
	 */
	public static ShaniString getString(String stringPath) {
		return getShaniString(storage,stringPath);
	}
	
	/**Returns node from "shani" node from mainFile pointed by provided path.
	 * @param path Path to Node
	 * @return Node from given path or null if not found.
	 */
	public static Node readNodeBase(String path) {
		NodeList nodes=getNodes(mainNode,path);
		if(nodes!=null)return nodes.item(0);
		return null;
	}
	/**Read data from directly from "shani" node in mainFile. 
	 * @param path Path to data in xml file.
	 * @return String from node under provided path.
	 */
	public static String readStringBase(String path) {
		return getString(mainNode,path);
	}
	/**Read data from directly from "shani" node in mainFile. 
	 * @param path Path to data in xml file.
	 * @return ShaniString from node under provided path.
	 */
	public static ShaniString readShaniStringBase(String path) {
		return getShaniString(mainNode,path);
	}
	
	/**Get nodes under given path in UserData part of file.
	 * @param path Path to super node.
	 * @return NodeList poinded by given path.
	 */
	public static NodeList getUserNodes(String path) {
		return getNodes(userdata,path);
	}
	/**Get ShaniString under given path in UserData part of file.
	 * @param stringPath Path to Node conataing wanted ShaniString.
	 * @return ShaniString pointed by path.
	 */
	public static ShaniString getUserShaniString(String stringPath) {
		return getShaniString(userdata,stringPath);
	}
	public static boolean getUserBoolean(String path) {
		return Boolean.parseBoolean(getString(userdata,path));
	}
	public static int getUserInt(String path) {
		return Integer.parseInt(getString(userdata,path));
	}
	
	private static NodeList getNodes(Node where, String path) {
		@SuppressWarnings("resource")
		Scanner scanner=new Scanner(path).useDelimiter(divider);
		NodeList Return=((Element)where).getElementsByTagName(scanner.next());
		while(scanner.hasNext()) {
			var node=(Element)Return.item(0);
			if(node==null)return null;
			Return=(node).getElementsByTagName(scanner.next());
		}
		return Return;
	}
	private static ShaniString getShaniString(Node where, String stringPath) {			//All changes here have to be made also in getString(Node,String). This methods do the same but output is diffrend.
		var nodes=getNodes(where,stringPath);
		if(nodes==null) {
			System.err.printf("Can't find \"%s\" in %s.%n",where.getNodeName(),stringPath);
			isErrorOccured=true;
			return null;
		}
		var node=nodes.item(0);
		if(node==null) {
			System.err.printf("Can't find \"%s\" in %s.%n",stringPath,where.getNodeName());
			isErrorOccured=true;
			return null;
		}
		return new ShaniString(node);
	}
	private static String getString(Node where, String stringPath) {					//All changes here have to be made also in getShaniString(Node,String). This methods do the same but output is diffrend.
		var nodes=getNodes(where,stringPath);
		if(nodes==null) {
			System.err.printf("Can't find \"%s\" in %s.%n",stringPath,where==storage?"storage":"userdata");
			isErrorOccured=true;
			return null;
		}
		var node=nodes.item(0);
		if(node==null) {
			System.err.printf("Can't find \"%s\" in %s.%n",stringPath,where==storage?"storage":"userdata");
			isErrorOccured=true;
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
		Node stor=createDirectory(userdata,path);
		stor.setTextContent(data.toFullString());
	}
	/**Writes given String under path in UserData part of file.
	 * @param path Path pointing to location in which data will be stored.
	 * @param data Data to store.
	 */
	public static void writeUserData(String path,String data) {
		Node stor=createDirectory(userdata,path);
		stor.setTextContent(data);
	}
	
	private static Node createDirectory(Node where,String path) {
		@SuppressWarnings("resource")
		Scanner scanner=new Scanner(path).useDelimiter(divider);
		String nodeName;
		Node parentNode=where;
		Node Return = null;
		while(scanner.hasNext()) {
			nodeName=scanner.next();
			Return=((Element)where).getElementsByTagName(nodeName).item(0);
			if(Return==null) {
				Return=Engine.doc.createElement(nodeName);
				parentNode.appendChild(Return);
				parentNode=Return;
				while(scanner.hasNext()) {
					nodeName=scanner.next();
					Return=Engine.doc.createElement(nodeName);
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
}