package shani.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class MainFileTools {
	private static String pathDivider="(?:\\s*\\.\\s*)+";
	
	public static void main(String[] args) throws IOException {
		if(args.length==0) {System.out.println("Please provide data origin, and target file names.");return;}
		try {
			switch(args[0].toLowerCase()) {
			case "-h":case "--help":
				printHelp();
				return;
			case "-u":case"--update":
				if(args.length==2)System.out.println("Please provide sourse file name");
					udpate(new File(args[1]+".dat"),new File(args.length>2?args[2]+".dat":"shani.dat"));
				break;
			case "-ss":case "--storestring":
				StringBuffer buffer=new StringBuffer().append(args[3]);
				for(int i=4;i<args.length;i++)buffer.append(" ").append(args[i]);
				storeString(new File(args[1]+".dat"),args[2],buffer.toString());
				break;
			}
			System.out.println("Action successfully completed.");
		} catch (Exception e) {
			File file=new File("errors.txt");
			PrintWriter out=new PrintWriter(new FileWriter(file,true));
			e.printStackTrace(out);
			e.printStackTrace();
			System.out.println("Failed to perform operation.");
			out.close();
		}
	}
	private static void printHelp() {
		System.out.println("Performs udpate of data in shani mainfile.\n"
				+ "As first argument give operation specyfier:\n"
				+ "-u, --update:\n"
				+ "\tUpdates data in one file with data in another.\n"
				+ "\tAs second argument give file name containing data used to update.\n"
				+ "\tAs third argument give name of file which you want to update. If not provided default file(shani.dat) will be used.\n"
				+ "-ss, --storeString:\n"
				+ "\tInsert given string to storage section under specyfied path.\n"
				+ "\tAs second argument give target file name\n"
				+ "\tAs third argument give path pointing to save position\n"
				+ "\tOn the end give string witch you want to store(can contain spaces).");
	}
	
	/**Udates target file witch info from origin file.
	 * All storage data from origin are copied into target, and adds orders occuring in origin not presented(Orders are equal if their classname field points to the same class) in target to it.
	 * @param origin File containing new data.
	 * @param target File into witch new data be inserted.
	 * @throws ParserConfigurationException When failed to parse one of the xml files.
	 * @throws IOException When failed to access one of the files.
	 * @throws SAXException When failed to parse one of the xml files.
	 */
	public static void udpate(File origin, File target) throws SAXException, IOException, ParserConfigurationException {
		update(new FileInputStream(origin),target);
	}
	public static void update(InputStream origin,File target) throws SAXException, IOException, ParserConfigurationException {
		var originDoc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(origin);
		var targetDoc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(target);
		
		var mainOriginNode=originDoc.getElementsByTagName("shani").item(0);
		var mainTargetNode=targetDoc.getElementsByTagName("shani").item(0);
		
		var originNode=((Element)mainOriginNode).getElementsByTagName("orders").item(0);
		var targetNode=((Element)mainTargetNode).getElementsByTagName("orders").item(0);
		
		var originNL=((Element)originNode).getElementsByTagName("order");
		var targetNL=((Element)targetNode).getElementsByTagName("order");
		
		String[] targetOrders=new String[targetNL.getLength()];
		for(int i=0;i<targetOrders.length;i++) {
			targetOrders[i]=((Element)targetNL.item(i)).getAttribute("classname");
			if(targetOrders[i].equals(""))System.out.println("Found corruption in main file. Property \"classname\" in an order not found");
		}
		
		orders:
		for(int i=0;i<originNL.getLength();i++) {																	//coping orders
			String name=((Element)originNL.item(i)).getAttribute("classname");
			for(String str:targetOrders)if(str.equals(name))continue orders;
			targetNode.appendChild(targetDoc.importNode(originNL.item(i), true));
		}
		
		originNL=((Element)mainOriginNode).getElementsByTagName("storage");
		targetNL=((Element)mainTargetNode).getElementsByTagName("storage");
		
		deepCopyNode(originNL.item(0),originDoc,targetNL.item(0),targetDoc);
		
		saveMainFile(targetDoc,target);
	}
	
	private static void deepCopyNode(Node origin, Document originDoc, Node target, Document targetDoc) {
		var originNL=((Element)origin).getElementsByTagName("*");
		var targetNL=((Element)target).getElementsByTagName("*");
		
		String[] targetNames=new String[targetNL.getLength()];
		for(int i=0;i<targetNames.length;i++) {
			if(targetNL.item(i).getParentNode()==target)
				targetNames[i]=targetNL.item(i).getNodeName();
		}
		
		originNodes:
		for(int i=0;i<originNL.getLength();i++) {
			Node originNode=originNL.item(i);
			if(originNode.getParentNode()!=origin)continue;
			String originName=originNode.getNodeName();
			for(int j=0;j<targetNames.length;j++) {
				if(originName.equals(targetNames[j])) {
					deepCopyNode(originNode,originDoc,targetNL.item(j),targetDoc);
					continue originNodes;
				}
			}
			target.appendChild(targetDoc.importNode(originNode, true));
		}
	}
	
	/**Stores given string in shani mainfile.
	 * Warning trying save string in already used directory will overvrite data and can cause it's corruption.
	 * @param target File into which insert new data.
	 * @param path Path poiting where store string.
	 * @param data String which you wand to save.
	 * @throws SAXException  When failed to parse one of the xml files.
	 * @throws IOException When failed to access to the file.
	 * @throws ParserConfigurationException  When failed to parse one of the xml files.
	 */
	public static void storeString(File target, String path, String data) throws SAXException, IOException, ParserConfigurationException {
		var targetDoc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(target);
		@SuppressWarnings("resource")
		Scanner pathScanner=new Scanner(path).useDelimiter(pathDivider);
		
		Node node=targetDoc.getElementsByTagName("storage").item(0);
		while(pathScanner.hasNext()) {
			String name=pathScanner.next();
			var newNode=((Element)node).getElementsByTagName(name).item(0);
			if(newNode!=null)node=newNode;
			else {
				newNode=targetDoc.createElement(name);
				node.appendChild(newNode);
				node=newNode;
			}
		}
		node.setTextContent(data);
		
		saveMainFile(targetDoc,target);
	}
	
	/**Save xml Document to a file.
	 * @param data Data to be saved.
	 * @param outputFile Ouput file.
	 */
	public static void saveMainFile(Document data, File outputFile){										//TODO fix xml parsing. It throws new lines everywhere. Curretly cleaned after output creation
		try {
			data.normalize();
			var transformerFactory=TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", 4);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,"yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			//transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			//transformer.transform(new DOMSource(doc), new StreamResult(Config.mainFile));
			
			var string=new StringWriter();
			transformer.transform(new DOMSource(data), new StreamResult(string));
			Scanner output=new Scanner(string.toString());
			//var fileOut=new PrintStream(new FileOutputStream(Config.mainFile));
			var fileOut=new OutputStreamWriter(new FileOutputStream(outputFile),StandardCharsets.UTF_8);
			while(output.hasNextLine()) {
				String line=output.nextLine();
				if(!line.trim().isEmpty()) {
					fileOut.write(line);
					fileOut.write("\r\n");
				}
			}
			output.close();
			fileOut.close();
		} catch(TransformerException|IOException ex) {
			ex.printStackTrace();
		}
	}
}