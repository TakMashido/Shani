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
import shani.modules.templates.FilterModule;
import shani.modules.templates.ShaniModule;
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
	private static ShaniString closeMessage;
	
	public static ShaniString licensesNotConfirmedMessage;
	public static ShaniString errorMessage;
	
	public static final Scanner in=new Scanner(System.in);
	
	public static Document doc;
	private static ArrayList<Order> orders = new ArrayList<Order>();
	private static ArrayList<FilterModule> filterModules = new ArrayList<>();
	
	private static Executable lastExecuted;
	
	private static ShaniString lastCommand;
	
	private static boolean initialized=false;
	private static boolean LOADING_ERROR=false;
	
	public static void main(String[] args) {
		initialize(args);
		start();
	}
	public static void initialize(String[]args) {
		new ShaniString();								//FIXME becouse of some strange reason something go wrong and ShaniString matching don't work properly.
		if(initialized)
			throw new RuntimeException("Engine already initialized");					//Can make problems. Throw not visible.
//			return;
		long time=System.currentTimeMillis();
		
		initialized=true;
		ArgsDecoder argsDec=new ArgsDecoder(args);
		
		if(argsDec.containFlag("-c","--custom")) {		//custom args
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
		} else if(argsDec.containFlag("-dd")){
			debug=System.out;
			commands=System.out;
		} else {
			try {
				System.setErr(new PrintStream(new FileOutputStream("Errors.log",true)));
			} catch (FileNotFoundException e1) {
				System.out.println("Failed to set err file");
				e1.printStackTrace();
			}
			try {
				debug=new PrintStream(new BufferedOutputStream(new FileOutputStream("Debug.log"),1024));
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
		Integer integer;
		if((integer=argsDec.getInt("-cl","--close"))!=null) {
			Thread closingThread=new Thread("CloserThread") {
				public void run() {
					try {
						Thread.sleep(60*1000*integer);
						exit();
					} catch (InterruptedException e) {}
				}
			};
			closingThread.setDaemon(true);
			closingThread.start();
		}
		if(argsDec.containFlag("-v","--verbose")) {
			Config.verbose=true;
		}
		
		if(!argsDec.isProcesed()) {														//End of args processing
			System.out.println("Program input contain un recognized parameters:");
			var unmatched=argsDec.getUnprocesed();
			for(var un:unmatched)System.out.println(un);
			System.exit(0);
		}
		
		try {
			new File("info.txt").delete();
			info=new PrintStream(new BufferedOutputStream(new FileOutputStream("Info.log"),1024));
		} catch (FileNotFoundException e1) {
			System.out.println("Failed to set info file");
			e1.printStackTrace();
		}
		
		if(Config.socksProxyHost!=null) {											//Set up before initializing orders
			System.setProperty("socksProxyHost", Config.socksProxyHost);
			System.setProperty("socksProxyPort", Integer.toString(Config.socksProxyPort));
		}
		if(Config.HTTPProxyHost!=null) {
			System.setProperty("http.proxyHost", Config.HTTPProxyHost);
			System.setProperty("http.proxyPort", Integer.toString(Config.HTTPProxyPort));
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
			System.out.println("Main data file doesn't exist. Create now y/n?");
			if (in.next().equals("y")) {
				try {
					createMainFile();
					System.out.println("File created. Please restart Shani.");
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {}
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
		
		if(commands==null) {
			try {
				commands=new PrintStream(new BufferedOutputStream(new FileOutputStream("Commands.log",true)));
			} catch (FileNotFoundException e) {
				System.out.println("Failed to set commands file");
				e.printStackTrace();
			}
		}
		
		if(LOADING_ERROR) {
			ShaniString loadingErrorMessage=ShaniString.loadString("engine.loadingErrorMessage");
			if(loadingErrorMessage!=null)loadingErrorMessage.printOut();
			else System.out.println("Error encountered during loading shani. Further info in errors.log file");
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				shutdownHook();
			}
		});
		
		commands.println("Startup time: "+(System.currentTimeMillis()-time)+"ms");
	}
	/**Runs Shani on current thread */
	public static void start() {
		if(!initialized) initialize(new String[0]);
		
		@SuppressWarnings("resource")
		Scanner in=new Scanner(System.in);
		System.out.println(helloMessage);
		while (in.hasNextLine()) {
			String str=in.nextLine().trim().toLowerCase();
			if(str.length()==0)continue;
			long time=System.nanoTime();
			ShaniString command=new ShaniString(str,false);
			Executable exec=interprete(command);
			
			if (exec==null) {
				System.out.println(notUnderstandMessage);
				commands.print('!');
			} else if(exec.isSuccesful()) lastExecuted=exec;
			commands.print(str);
			commands.println(" -> "+(System.nanoTime()-time)/1000/1000f+" ms");
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
		
		helloMessage=ShaniString.loadString("engine.helloMessage");					
		closeMessage=ShaniString.loadString("engine.closeMessage");
		notUnderstandMessage=ShaniString.loadString("engine.notUnderstandMessage");
		errorMessage=ShaniString.loadString("engine.errorMessage");                
		licenseConfirmationMessage=ShaniString.loadString("engine.licenseConfirmationMessage");
		licensesNotConfirmedMessage=ShaniString.loadString("engine.licensesNotConfirmedMessage");
		
		NodeList ordersNode = ((Element)doc.getElementsByTagName("orders").item(0)).getElementsByTagName("order");
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
					info.printf("Loading \"%s\" order%n",className);
					Order order = (Order) Class.forName(className).getDeclaredConstructor().newInstance();
					info.println("Order loaded");
					order.init(e);
					orders.add(order);
				} catch(ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
					System.out.println("Failed to parse \""+e.getAttribute("classname")+"\" order from main file.");
					ex.printStackTrace();
				}
			}
		}
		
		NodeList moduleNodes=((Element)doc.getElementsByTagName("modules").item(0)).getElementsByTagName("module");
		for(int i=0;i<moduleNodes.getLength();i++) {
			Element e=(Element)moduleNodes.item(i);
			try {
				String className=e.getAttribute("classname");
				if(className.equals("")) {
					System.out.println("Error in mainfile found");
					System.err.println("Classname of an order is not specyfied.");
					continue;
				}
				info.printf("Loading \"%s\" module%n",className);
				ShaniModule module=(ShaniModule) Class.forName(className).getDeclaredConstructor(Element.class).newInstance(e);
				
				if(module instanceof FilterModule) {
					filterModules.add((FilterModule)module);
				} else {
					System.out.println("Unrecognized module \""+className+"\".");
				}
			} catch(ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
				System.out.println("Failed to parse \""+e.getAttribute("classname")+"\" module from main file.");
				ex.printStackTrace();
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
			
			var string=new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(string));
			Scanner output=new Scanner(string.toString());
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
		System.out.flush();
		System.err.flush();
	}
	
	public static Executable interprete(String command) {
		return interprete(new ShaniString(command));
	}
	public static Executable interprete(ShaniString command) {
		if(command.isEmpty())return null;
		
		info.print(command.toFullString()+" --> ");
		for(FilterModule filter:filterModules)command=filter.filter(command);
		info.println(command.toFullString());
		
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
				if(Config.verbose)
					commands.println(exec.action.getClass().toString()+" "+exec.cost);
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
	
	/**Checks if user confirmed given license. If not send license confirmation message to user.
	 * Equivalent to {@link #getLicenseConfirmation(String,boolean) getLicenseConfirmation(name,true)}.
	 * @param name Name of license which confirmation are being checked.
	 * @return If license corfirmed
	 */
	public static boolean getLicenseConfirmation(String name) {
		return getLicenseConfirmation(name,true);
	}
	/**Checks if user confirmed given license.
	 * @param name Name of license which confirmation are being checked.
	 * @param displayConfirmation If ask user for confirmation if not already getted.
	 * @return If license corfirmed
	 */
	public static boolean getLicenseConfirmation(String name,boolean displayConfirmation) {
		String nameToSearch=name.replace('.', '-').toLowerCase();
		boolean confirmed=Storage.getUserBoolean("acceptedLicences."+nameToSearch);
		if(confirmed)return true;
		if(!displayConfirmation)return false;
		
		@SuppressWarnings("resource")
		Scanner in=new Scanner(System.in);
		System.out.printf(licenseConfirmationMessage.toString(),name);
		System.out.println();
		String nextLine;
		while((nextLine=in.nextLine()).isEmpty());
		Boolean confirmed2=isInputPositive(new ShaniString(nextLine));
		if(confirmed2==null)return false;
		if(confirmed2)Storage.writeUserData("acceptedLicences."+nameToSearch, true);
		return confirmed2;
	}
	/**Checks if input is positive response.
	 * Return true for positive/agreeding one (yes,youp),
	 * false for negative/unagriding (no,nope),
	 * or false for unrecognized one.
	 * Actual positive/negative pattern depends on value stored in config file(TODO move to mainFile)
	 * @param input
	 * @return
	 */
	public static Boolean isInputPositive(ShaniString input) {
		if(input.isEmpty())return null;
		if(Config.positiveResponeKey.equals(input))return true;
		if(Config.negativeResponeKey.equals(input))return false;
		return null;
	}
	
	public static void exit() {
		debug.println("exit\n");
		System.out.println(closeMessage);
		try {
			Thread.sleep(700);
		} catch (InterruptedException e) {}
		System.exit(0);
	}
	/**Call if any error ancountered during loading shani. Sets up LOADING_ERROR flag. If true at the end of loading message informing user about loading erros become send to System.out*/
	public static void registerLoadException() {
		LOADING_ERROR=true;
	}
	
	/**
	 * Invoced on application close
	 */
	private static void shutdownHook() {
		saveMainFile();
		flushBuffers();
	}
}