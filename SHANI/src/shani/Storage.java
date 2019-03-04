package shani;

import java.util.Scanner;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Storage {
	private static final Node storage;
	
	private static final Pattern divider=Pattern.compile("(?:\\s*\\.\\s*)+");
	
	static {
		storage=((Element)(Engine.doc.getElementsByTagName("shani").item(0))).getElementsByTagName("storage").item(0);
	}
	
	private static boolean isErrorOccured=false;
	public static boolean isErrorOccured() {return isErrorOccured;}
	
	public static NodeList getNodes(String catName) {
		@SuppressWarnings("resource")
		Scanner scanner=new Scanner(catName).useDelimiter(divider);
		NodeList Return=((Element)storage).getElementsByTagName(scanner.next());
		while(scanner.hasNext()) {
			var node=(Element)Return.item(0);
			if(node==null)return null;
			Return=(node).getElementsByTagName(scanner.next());
		}
		return Return;
	}
	public static ShaniString getString(String stringPath) {
		var nodes=getNodes(stringPath);
		if(nodes==null) {
			System.err.printf("Can't find \"%s\" in storage.",stringPath);
			System.err.println();
			isErrorOccured=true;
			return null;
		}
		var node=nodes.item(0);
		if(node==null) {
			System.err.printf("Can't find \"%s\" in storage.",stringPath);
			System.err.println();
			isErrorOccured=true;
			return null;
		}
		return new ShaniString(node);
	}
}