package shani;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import liblaries.ArgsDecoder;
import shani.orders.templates.Executable;
import shani.orders.templates.Order;
import shani.tools.MainFileTools;

public class Engine {
	public static PrintStream debug;
	public static PrintStream info;
	private static PrintStream commands;
	
	private static ShaniString helloMessage;												//Have to be initialzed after loading doc.
	private static ShaniString notUnderstandMessage;
	private static ShaniString licenseConfirmationMessage;						//Sentence construction may be very not accurate.
	
	public static ShaniString licensesNotConfirmedMessage;
	public static ShaniString errorMessage;
	
	public static final Scanner in=new Scanner(System.in);
	
	public static Document doc;
	private static ArrayList<Order> orders = new ArrayList<Order>();
	
	private static Executable lastExecuted;
	
	private static ShaniString lastCommand;
	
	private static boolean initialized=false;
	
	public static void main(String[] args) {
		new ShaniString();								//FIXME becouse of some strange reason in static init of Config class something go wrong and ShaniString matching don't work property(jvm/javac bug??) have to investigate it. Occur in java 10 other versions not checked
		initialize(args);
		start();
	}
	public static void initialize(String[]args) {
		if(initialized) throw new RuntimeException("Engine already initialized");
		initialized=true;
		ArgsDecoder argsDec=new ArgsDecoder(args);
		
		if(argsDec.containFlag("-c","--custom")) {		//costom args
			System.out.println("Please give args.");
			@SuppressWarnings("resource")
			Scanner in=new Scanner(new Scanner(System.in).nextLine());
			ArrayList<String> argsList=new ArrayList<>();
			while(in.hasNextLine())argsList.add(in.next());
			args=argsList.toArray(new String[0]);
			argsDec=new ArgsDecoder(args);
			System.out.println("New args set. Running shani with it.");
		}
		
		if(argsDec.containFlag("-d","--debug")) {		//debug
			debug=System.out;
		}else {
			try {
				System.setErr(new PrintStream(new FileOutputStream("Errors.log",true)));
			} catch (FileNotFoundException e1) {
				System.out.println("Failed to set err file");
				e1.printStackTrace();
			}
			try {
				debug=new PrintStream(new BufferedOutputStream(new FileOutputStream("Debug.log",true),1024));
			} catch (FileNotFoundException e1) {
				System.out.println("Failed to set debug file");
				e1.printStackTrace();
			}
		}
		if(argsDec.containFlag("-u","--update")) {
			try {
				System.out.println("Updating shani mainfile...");
				MainFileTools.update(Engine.class.getResourceAsStream("/files/DefaultFile.dat"), Config.mainFile);
				System.out.println("Main file updated.");
			} catch (SAXException | IOException | ParserConfigurationException e) {
				System.out.println("Failed to update main file. Closing...");
				e.printStackTrace();
				System.exit(0);
			}
		}
		if(!argsDec.isProcesed()) {
			System.out.println("Program input contain un recognized parameters:");
			var unmatched=argsDec.getUnProcesed();
			for(var un:unmatched)System.out.println(un);
			System.exit(0);
		}
		
		try {
			new File("info.txt").delete();
			info=new PrintStream(new BufferedOutputStream(new FileOutputStream("Info.log",true),1024));
		} catch (FileNotFoundException e1) {
			System.out.println("Failed to set info file");
			e1.printStackTrace();
		}
		
		if (Config.mainFile.exists()) {
			try {
				parseMainFile();
			} catch (ParserConfigurationException | SAXException | IOException | IllegalArgumentException | SecurityException | DOMException e) {
				System.out.println("Failed to parse main data file");
				e.printStackTrace();
				return;
			}
		} else {
			System.out.println("Main config file doesn't exist. Create now y/n?");
			if (in.next().equals("y")) {
				try {
					createMainFile();
					System.out.println("File created. Please restart Shani.");
					System.exit(0);
				} catch (IOException | TransformerFactoryConfigurationError e) {
					System.out.println("Failed to create main file");
					e.printStackTrace();
					return;
				}
			} else {
				System.out.println("Closing Shani");
				System.exit(0);
			}
		}
		
		try {
			commands=new PrintStream(new BufferedOutputStream(new FileOutputStream("Commands.log")));
		} catch (FileNotFoundException e) {
			System.out.println("Failed to set commands file");
			e.printStackTrace();
		}
		
		if(Storage.isErrorOccured()) {
			System.out.println("Error ocured during loading storage data. Please read error log.");
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				shutdownHook();
			}
		});
	}
	public static void start() {
		if(!initialized) initialize(new String[0]);
		
		@SuppressWarnings("resource")
		Scanner in=new Scanner(System.in);
		System.out.println(helloMessage);
		
		while (in.hasNextLine()) {
			String str=in.nextLine().trim().toLowerCase();
			if(str.length()==0)continue;
			ShaniString command=new ShaniString(str,false);
			if(command.equals(""))continue;
			Executable exec=interprete(command);
			
			if (exec==null) {
				System.out.println(notUnderstandMessage);
				commands.print('!');
			}
			else if(exec.isSuccesful()) lastExecuted=exec;
			commands.println(str);
		}
	}
	
	/**Copy file from "/files/DefaultFile.dat" in jar as mainFile specyfied in config
	 * @throws IOException when failed to create mainFile
	 * @throws NullPointerException when failed to read file inside jar
	 */
	private static void createMainFile() throws IOException{
		Config.mainFile.createNewFile();
		
		InputStream in=Engine.class.getResourceAsStream("/files/DefaultFile.dat");
		OutputStream out=new FileOutputStream(Config.mainFile);
		
		byte[] buf=new byte[1024];
		int toCopy;
		while((toCopy=in.read(buf))>0) {
			out.write(buf, 0, toCopy);
		}
		
		in.close();
		out.close();
	}
	private static void parseMainFile() throws SAXException, IOException, ParserConfigurationException {
		doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Config.mainFile);
		doc.getDocumentElement().normalize();
		NodeList ordersNode = ((Element)doc.getElementsByTagName("orders").item(0)).getElementsByTagName("order");
		
		helloMessage=ShaniString.loadString("engine.helloMessage");					
		notUnderstandMessage=ShaniString.loadString("engine.notUnderstandMessage");
		errorMessage=ShaniString.loadString("engine.errorMessage");                
		licenseConfirmationMessage=ShaniString.loadString("engine.licenseConfirmationMessage");
		licensesNotConfirmedMessage=ShaniString.loadString("engine.licensesNotConfirmedMessage");
		
		for (int i = 0; i < ordersNode.getLength(); i++) {
			Node node = ordersNode.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) node;
				try {
					String className=e.getAttribute("classname");
					if(className.equals("")) {
						System.out.println("Error in mainfile found");
						System.err.println("Classname of an order is not specyfied.");
						continue;
					}
					info.printf("Loading \"%s\" module%n",className);
					Order order = (Order) Class.forName(className).getDeclaredConstructor().newInstance();
					info.println("Module loaded");
					order.init(e);
					orders.add(order);
				} catch(ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
					System.out.println("Failed to parse \""+e.getAttribute("classname")+"\" order from main file.");
					ex.printStackTrace();
				}
			}
		}
	}
	public static void saveMainFile(){										//TODO fix xml parsing. It throws new lines everywhere. Curretly cleaned after output creation
		for(var order:orders)order.save();
		try {
			if(doc==null)return;
			doc.normalize();
			var transformerFactory=TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", 4);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,"yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			//transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			//transformer.transform(new DOMSource(doc), new StreamResult(Config.mainFile));
			
			var string=new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(string));
			Scanner output=new Scanner(string.toString());
			//var fileOut=new PrintStream(new FileOutputStream(Config.mainFile));
			var fileOut=new OutputStreamWriter(new FileOutputStream(Config.mainFile),StandardCharsets.UTF_8);
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
	public static void flushBuffers() {
		debug.flush();
		info.flush();
		commands.flush();
	}
	
	public static Executable interprete(String command) {
		return interprete(new ShaniString(command));
	}
	public static Executable interprete(ShaniString command) {
		if(command.isEmpty())return null;
		lastCommand=command;
		Executable toExec=getExecutable(command);
		
		if(toExec!=null&&toExec.cost<=Config.sentenseCompareTreshold) {
			toExec.execute();
			System.gc();
			return toExec;
		} else {
			Engine.debug.println("cannot execute: "+command);			
			return null;
		}
	}
	public static boolean canExecute(ShaniString command) {
		short minCost=Config.sentenseCompareTreshold;
		for (Order order : orders) {
			List<Executable> execs=order.getExecutables(command);
			if(execs==null)continue;
			for(Executable exec:execs) {
				if(exec.cost<minCost) {
					return true;
				}
			}
		}
		
		return false;
	}
	public static Executable getExecutable(ShaniString command) {
		Executable Return=null;
		short minCost=Config.sentenseCompareTreshold;
		
		for (Order order : orders) {
			List<Executable> execs=order.getExecutables(command);
			if(execs==null)continue;
			for(Executable exec:execs) {
//				System.out.println(exec.action.getClass().toString()+" "+exec.cost);
				if(exec.cost<minCost) {
					minCost=exec.cost;
					Return=exec;
				}
			}
		}
		
		return Return!=null&&Return.cost<Config.sentenseCompareTreshold?Return:null;
	}
	
	public static Executable getLastExecuted() {
		return lastExecuted;
	}
	
	public static ShaniString getLastCommand() {
		return lastCommand.copy();
	}
	
	public static boolean getLicenseConfirmation(String name) {
		String nameToSearch=name.replace('.', '-');
		boolean confirmed=Storage.getUserBoolean("acceptedLicences."+nameToSearch);
		if(confirmed)return true;
		
		@SuppressWarnings("resource")
		Scanner in=new Scanner(System.in);
		System.out.printf(licenseConfirmationMessage.toString(),name);
		System.out.println();
		Boolean confirmed2=isInputPositive(new ShaniString(in.nextLine()));
		if(confirmed2==null)return false;
		if(confirmed2)Storage.writeUserData("acceptedLicences."+nameToSearch, true);
		return confirmed2;
	}
	public static Boolean isInputPositive(ShaniString input) {
		if(Config.positiveResponeKey.equals(input))return true;
		if(Config.negativeResponeKey.equals(input))return false;
		return null;
	}
	
	/**
	 * Invoced on application close
	 */
	private static void shutdownHook() {
		saveMainFile();
		debug.flush();
		info.flush();
	}
}