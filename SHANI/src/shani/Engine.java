package shani;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class Engine {
	public static PrintStream debug;
	public static PrintStream info;
	private static PrintStream commands;
	
	private static ShaniString helloMessage;												//Have to be initialized after loading doc.
	private static ShaniString notUnderstandMessage;
	private static ShaniString licenseConfirmationMessage;						//Sentence construction may be very not accurate.
	private static ShaniString closeMessage;
	
	public static ShaniString licensesNotConfirmedMessage;
	public static ShaniString errorMessage;
	
	public static final Scanner in=new Scanner(System.in);
	
	/**Main document of templateFile*/
	public static Document doc;
	private static ArrayList<Order> orders = new ArrayList<Order>();
	private static ArrayList<FilterModule> filterModules = new ArrayList<>();
	
	private static Executable lastExecuted;
	
	private static ShaniString lastCommand;
	
	/**When last command was executed in ms.*/
	public static long lastExecutionTime=System.currentTimeMillis();				
	
	private static boolean initialized=false;
	private static boolean LOADING_ERROR=false;
	
	public static void main(String[] args) {
		initialize(args);
		start();
	}
	public static void initialize(String[]args) {
		new ShaniString();								//FIXME because of some strange reason something go wrong and ShaniString matching don't work properly without this.
		if(initialized)
			throw new RuntimeException("Engine already initialized");					//Can make problems - Throw not visible.
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
				System.setErr(new PrintStream(new FileOutputStream(Config.errorsLogFileLocation,true)));
			} catch (FileNotFoundException e1) {
				System.out.println("Failed to set err file");
				e1.printStackTrace();
			}
			try {
				debug=new PrintStream(new BufferedOutputStream(new FileOutputStream(Config.debugLogFileLocation),1024));
			} catch (FileNotFoundException e1) {
				System.out.println("Failed to set debug file");
				e1.printStackTrace();
			}
		}
		
		Integer integer;
		if((integer=argsDec.getInt("-cl","--close"))!=null) {
			Thread closingThread=new Thread("CloserThread") {
				@Override
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
			System.out.println("Program input contain unrecognized parameters:");
			var unmatched=argsDec.getUnprocesed();
			for(var un:unmatched)System.out.println(un);
			System.exit(0);
		}
		
		try {
			Config.infoLogFileLocation.delete();
			info=new PrintStream(new BufferedOutputStream(new FileOutputStream(Config.infoLogFileLocation),1024));
		} catch (FileNotFoundException e1) {
			System.out.println("Failed to set info file");
			e1.printStackTrace();
		}
		
		if(commands==null) {
			try {
				commands=new PrintStream(new BufferedOutputStream(new FileOutputStream(Config.commandsLogFileLocation,true)));
			} catch (FileNotFoundException e) {
				System.out.println("Failed to set commands file");
				e.printStackTrace();
			}
		}
		
		if(Config.socksProxyHost!=null) {											//Set up before initializing orders
			System.setProperty("socksProxyHost", Config.socksProxyHost);
			System.setProperty("socksProxyPort", Integer.toString(Config.socksProxyPort));
		}
		if(Config.HTTPProxyHost!=null) {
			System.setProperty("http.proxyHost", Config.HTTPProxyHost);
			System.setProperty("http.proxyPort", Integer.toString(Config.HTTPProxyPort));
		}
		
		commands.println("\n<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<startup>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");				//Make startup place visible inside commands file
		commands.println();
		
		try {
			parseMainFile();
		} catch (ParserConfigurationException | SAXException | IOException | IllegalArgumentException | SecurityException | DOMException e) {
			System.out.println("Failed to parse main data file");
			e.printStackTrace();
			return;
		}
		
		if(LOADING_ERROR) {
			ShaniString loadingErrorMessage=ShaniString.loadString(doc,"engine.loadingErrorMessage");
			if(loadingErrorMessage!=null)loadingErrorMessage.printOut();
			else System.out.println("Error encountered during loading shani. Further info in errors.log file");
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				shutdownHook();
			}
		});
		
		commands.println("\nStartup time: "+(System.currentTimeMillis()-time)+"ms");
		
		commands.println("\n<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<go>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
	}
	/**Runs Shani on current thread */
	public static void start() {
		if(!initialized) initialize(new String[0]);
		
		System.out.println(helloMessage);
		while (in.hasNextLine()) {
			String str=in.nextLine().trim().toLowerCase();
			
			if(str.length()==0)continue;
			long time=System.nanoTime();
			ShaniString command=new ShaniString(str,false);
			try {
				Executable exec=interprete(command);
				
				if (exec==null) {
					System.out.println(notUnderstandMessage);
					commands.println("Can't execute");
				} else {
					commands.println("execution time = "+(System.nanoTime()-time)/1000/1000f+" ms");
					
					if(exec.isSuccesful()) lastExecuted=exec;
				}
			} catch(Exception ex) {
				ex.printStackTrace();
				
				commands.println("execution time = "+(System.nanoTime()-time)/1000/1000f+" ms. Error ocured.");
				
				errorMessage.printOut();
			}
		}
	}
	
	private static void parseMainFile() throws SAXException, IOException, ParserConfigurationException {
		doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Engine.class.getResourceAsStream("/files/ordersData/"+Config.language+".xml"));
		doc.getDocumentElement().normalize();
		
		Node engineNode=doc.getElementsByTagName("engine").item(0);
		
		helloMessage=ShaniString.loadString(engineNode, "helloMessage");
		closeMessage=ShaniString.loadString(engineNode, "closeMessage");
		notUnderstandMessage=ShaniString.loadString(engineNode, "notUnderstandMessage");
		errorMessage=ShaniString.loadString(engineNode, "errorMessage");
		licenseConfirmationMessage=ShaniString.loadString(engineNode, "licenseConfirmationMessage");
		licensesNotConfirmedMessage=ShaniString.loadString(engineNode, "licensesNotConfirmedMessage");
		
		long bigTime=System.nanoTime();
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
					long time=System.nanoTime();
					Order order = (Order) Class.forName(className).getDeclaredConstructor().newInstance();
					commands.printf("Order %-40s loaded in \t%8.3f ms.%n",className,(System.nanoTime()-time)/1000000f);
					order.init(e);									//TODO add test if loaded successfully
					orders.add(order);
				} catch(ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
					Engine.registerLoadException();
					System.err.println("Failed to parse \""+e.getAttribute("classname")+"\" order from main file.");
					ex.printStackTrace();
				}
			}
		}
		commands.printf("Orders loaded in \t%8.3f ms.%n",(System.nanoTime()-bigTime)/1000000f);
		
		bigTime=System.nanoTime();
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
				long time=System.nanoTime();
				ShaniModule module=(ShaniModule) Class.forName(className).getDeclaredConstructor(Element.class).newInstance(e);
				commands.printf("Module %-39s loaded in \t%8.3f ms.%n",className,(System.nanoTime()-time)/1000000f);
				
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
		commands.printf("Modules loaded in \t%8.3f ms.%n",(System.nanoTime()-bigTime)/1000000f);
		
		bigTime=System.nanoTime();
		moduleNodes=((Element)doc.getElementsByTagName("static").item(0)).getChildNodes();
		for(int i=0;i<moduleNodes.getLength();i++) {
			Node n=moduleNodes.item(i);
			if(n.getNodeType()!=Node.ELEMENT_NODE) continue;
			Element e=(Element)moduleNodes.item(i);
			
			try {
				String className=e.getAttribute("classname");
				if(className.equals("")) {
					System.out.println("Error in mainfile found");
					System.err.println("Classname of an static init is not specyfied.");
					continue;
				}
				
				long time=System.nanoTime();
				Class.forName(className).getMethod("staticInit", Element.class).invoke(null, e);
				commands.printf("Static initializer %-27s loaded in \t%8.3f ms.%n",className,(System.nanoTime()-time)/1000000f);
			} catch(ClassNotFoundException | IllegalArgumentException | NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException ex) {
				System.out.println("Failed to parse \""+e.getAttribute("classname")+"\" static initilizer from main file.");
				ex.printStackTrace();
			}
		}
		commands.printf("Static data loaded in \t%8.3f ms.%n", (System.nanoTime()-bigTime)/1000000f);
		
	}
	public static void saveMainFile(){
		for(var order:orders)order.save();									//Flush all orders save data
		Storage.save();														//Save data file
	}
	public static void saveDocument(Document document, File targetFile) {
		try {
			if(document==null)return;
			document.normalize();
			var transformerFactory=TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", 4);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,"yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			
			var string=new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(string));			//TODO fix xml parsing. It throws new lines everywhere. Currently cleaned after output creation
			
			try(Scanner output=new Scanner(string.toString());
					var fileOut=new OutputStreamWriter(new FileOutputStream(targetFile),StandardCharsets.UTF_8)){
				
				while(output.hasNextLine()) {
					String line=output.nextLine();
					if(!line.trim().isEmpty()) {
						fileOut.write(line);
						fileOut.write("\r\n");
					}
				}
			}
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
		
		commands.println("\n"+command.toFullString()+':');
		info.println("\n<Parsing><Parsing><Parsing><Parsing><Parsing><Parsing>"+command.toFullString()+':');
		for(FilterModule filter:filterModules)command=filter.filter(command);
		commands.println("\t"+command.toFullString());
		info.println("\t"+command.toFullString());
		
		lastCommand=command;
		Executable toExec=getExecutable(command);
		
		if(toExec!=null&&toExec.cost<=Config.sentenseCompareTreshold) {
			info.println("<Execution><Excution><Execution><Execution>");
			toExec.execute();
			info.println("<End><End><End><End><End><End><End><End><End>");
			lastExecutionTime=System.currentTimeMillis();
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
		float minCost=Short.MAX_VALUE;
		
		long time=System.nanoTime();
		
		for (Order order : orders) {
			List<Executable> execs=order.getExecutables(command);
			if(execs==null)continue;
			for(Executable exec:execs) {
				if(Config.verbose)
					commands.println(exec.action.getClass().toString()+" "+exec.cost+":"+exec.importanceBias);
				
				if(exec.cost>Config.sentenseCompareTreshold)
					continue;
				
				short cost=(short)(exec.cost-exec.importanceBias*Config.importanceBiasMultiplier);
				if(cost<minCost) {
					minCost=cost;
					Return=exec;
				}
			}
		}
		
		commands.printf("Search time: %.3f ms%n",(System.nanoTime()-time)/1000000f);
		if(Return!=null) {
			commands.println("Executing "+Return.action.getClass().toString()+" "+Return.cost+":"+Return.importanceBias);
		}
		
		return Return;
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
		Scanner in=new Scanner(System.in);													//TODO check if can use Engine.in there
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
	 * Equivalent to {@link Engine#isInputPositive(ShaniString)}.
	 * @param input value to check.
	 * @return true if positive, false if negative, null if unrecognized./.
	 */
	public static Boolean isInputPositive(String input) {
		return isInputPositive(new ShaniString(input,false));
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
	/**Call if any error encountered during loading shani. Sets up LOADING_ERROR flag. If true at the end of loading message informing user about loading errors become send to System.out*/
	public static void registerLoadException() {
		LOADING_ERROR=true;
	}
	
	/**
	 * Invoked on application close
	 */
	protected static void shutdownHook() {
		saveMainFile();
		flushBuffers();
	}
}