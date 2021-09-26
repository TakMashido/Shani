package takMashido.shani;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import takMashido.shani.core.Config;
import takMashido.shani.core.Intend;
import takMashido.shani.core.IntendGetter;
import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.Storage;
import takMashido.shani.core.Tests;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.filters.IntendFilter;
import takMashido.shani.libraries.Logger;
import takMashido.shani.orders.Executable;
import takMashido.shani.orders.Order;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

/**Main class of shani. Responsible for all data loading and intends flow.
 * For service functions it contains only core of implementation, all helper code and methods with different args are in {@link takMashido.shani.core.ShaniCore} class.
 * @author TakMashido
 */
public class Engine {
	private static PrintStream info;
	private static PrintStream debug;
	private static PrintStream commands;
	
	private static ShaniString helloMessage;												//Have to be initialized after loading doc.
	private static ShaniString notUnderstandMessage;
	private static ShaniString licenseConfirmationMessage;						//Sentence construction may be very not accurate.
	private static ShaniString closeMessage;
	
	public static ShaniString licensesNotConfirmedMessage;
	public static ShaniString errorMessage;
	
	//Contain loaders of all loaded extensions.
	private static ArrayList<ClassLoader> extensionLoaders =new ArrayList<>();
	
	public static void staticInit(Element e){
		helloMessage = ShaniString.loadString(e, "helloMessage");
		closeMessage = ShaniString.loadString(e, "closeMessage");
		notUnderstandMessage = ShaniString.loadString(e, "notUnderstandMessage");
		errorMessage = ShaniString.loadString(e, "errorMessage");
		licenseConfirmationMessage = ShaniString.loadString(e, "licenseConfirmationMessage");
		licensesNotConfirmedMessage = ShaniString.loadString(e, "licensesNotConfirmedMessage");
	}
	
	/**Freshly registered intends.*/
	private static BlockingQueue<Intend> intends=new LinkedBlockingQueue<>();
	/**Filtered intends ready to execution or to give to currently executed order as data.*/
	private static BlockingQueue<Intend> filteredIntends=new LinkedBlockingQueue<>();

	/**Main document of templateFile*/
	public static Document doc;
	private static ThreadGroup inputGetters=new ThreadGroup("inputGetters");
	private static ArrayList<Order> orders = new ArrayList<Order>();
	private static ArrayList<IntendFilter> inputFilters = new ArrayList<>();
	
	private static Executable lastExecuted;
	
	/**When last command was executed in ms.*/
	public static long lastExecutionTime=System.currentTimeMillis();				
	
	private static boolean initialized=false;
	private static boolean LOADING_ERROR=false;
	
	public static void initialize() {
		new ShaniString();								//FIXME because of some strange reason something go wrong and ShaniString matching don't work properly without this.
		if(initialized)
			throw new RuntimeException("Engine already initialized");					//Can make problems - Throw not visible.
//			return;
		long time=System.currentTimeMillis();
		
		initialized=true;
		
		commands=Logger.getStream("commands");
		debug=Logger.getStream("debug");
		info=Logger.getStream("info");
		
		commands.println("\n<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<startup>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");				//Make startup place visible inside commands file
		commands.println();
		
		try {							//Load main init file
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Engine.class.getResourceAsStream("/takMashido/shani/files/init/" + Config.language + ".xml"));
			doc.getDocumentElement().normalize();
			
			parseModules(doc,Engine.class.getClassLoader());				//Master modules
			
			loadExtension(Engine.class.getClassLoader());					//Any other compiled together with shani engine
		} catch (ParserConfigurationException | SAXException | IOException | IllegalArgumentException | SecurityException | DOMException e) {
			System.out.println("Failed to parse main data file");
			e.printStackTrace();
			return;
		}
		
		loadExtensions(Config.extensionsDirectory);
		
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
	/**Start shani threads.*/
	public static void start() {
		if(!initialized) throw new RuntimeException("Engine not initialized");
		
		helloMessage.printOut();

		Thread shaniFiltering=new Thread(Engine::shaniFiltering);
		shaniFiltering.setName("shaniFiltering");
		shaniFiltering.setDaemon(true);
		shaniFiltering.start();

		Thread shaniInterpreter=new Thread(Engine::shaniInterpreter);
		shaniInterpreter.setName("shaniInterpreter");
		shaniInterpreter.setDaemon(false);
		shaniInterpreter.start();

		System.out.println(Config.testMode);
		if(Config.testMode) {
			InnerTest.run();
			
			System.out.println("\n------------------------------");
			Tests.addTests(extensionLoaders);
			if(!Tests.nextTest()){
				System.out.println("No end2end test run. You probably misconfigured something.");
				Engine.exit();
			}
		}
	}
	/**Method for shaniFiltering thread.
	 * It's responsible for filtering raw {@link Intend} objects using {@link IntendFilter} preparing them for execution.
	 */
	private static void shaniFiltering(){
		try{
			while(true){
				Intend intend=intends.take();

				info.println("Filtering \""+intend.rawValue+'"');
				long time=System.currentTimeMillis();

				for(IntendFilter filter:inputFilters) {
					Intend newIntend = filter.filter(intend);
					if(newIntend !=null) {
						intend = newIntend;
					}
				}

				filteredIntends.put(intend);
				info.printf("\"%s\" filtered in %.2f ms.%n",intend.toString(),(System.currentTimeMillis()-time)/1000f);
			}
		} catch(InterruptedException ignored){}
	}
	/**Method for shaniInterpreter thread.
	 * It's responsible for finding Executables and executing their Actions.
	 */
	private static void shaniInterpreter(){
		try {
			while (true) {
				Intend intend = filteredIntends.take();

				long time = System.nanoTime();
				try {
					Executable exec = interpret(intend);

					if(Config.testMode)
						Tests.testEnded(exec);

					if (exec == null) {
						System.out.println(notUnderstandMessage);
						commands.println("Can't execute");
					} else {
						commands.println("execution time = " + (System.nanoTime() - time) / 1000 / 1000f + " ms");

						if (exec.isSuccessful()) lastExecuted = exec;
					}
				} catch (Exception ex) {
					ex.printStackTrace();

					commands.println("execution time = " + (System.nanoTime() - time) / 1000 / 1000f + " ms. Error occurred.");

					errorMessage.printOut();
				}

				if(Config.testMode) {
					if(!Tests.nextTest()){
						Tests.printSummary();
						Engine.exit();
					}
				}
			}
		} catch (InterruptedException ignored) {}
	}

	/**
	 * Parse modules from given file. If it's directory every .jar file in it will be treated as extension to load.
	 * @param file .jar file or directory from which load modules.
	 */
	private static void loadExtensions(File file) {
		if(file.isDirectory()){
			info.println("Loading extensions dir \""+file.getAbsolutePath()+'"');
			for(File f:file.listFiles()){
				if(!f.getName().endsWith(".jar")||
						f.isDirectory()) {
					continue;
				}
				
				loadExtensions(f);
			}
			return;
		}
		
		info.println("Loading extension \""+file.getAbsolutePath()+'"');
		
		if(!file.getName().endsWith(".jar")) {
			System.err.println('"'+file.getName()+"\" is not valid extensions. Only .jar files can be provided.");
			Engine.registerLoadException();
			return;
		}
		
		try {
			String fileName=file.getName();
			commands.println("\nLoading extension \""+fileName.substring(0,fileName.length()-4)+'"');
			long time=System.nanoTime();
			
			URLClassLoader loader=new URLClassLoader(new URL[]{file.toURI().toURL()}, Engine.class.getClassLoader());
			loadExtension(loader);
		} catch (MalformedURLException e) {
			System.err.println("Failed to read \"" + file.getName() + "\" extension file.");
			e.printStackTrace();
			Engine.registerLoadException();
		}
	}
	
	/**Load extension from given class loader.
	 * @param loader Loader to use to load extension
	 */
	private static void loadExtension(ClassLoader loader){
		extensionLoaders.add(loader);
		
		/*Finds possible init files directories using given class loader. Every directory with name starting with "init" is considered as init files location.
		 * @param l Class loader used to get resources directories.
		 */
		Function<ClassLoader,List<URL>> getPossibleLocations=(ClassLoader l)->{
			if(l==null)
				return null;
			
			List<URL> ret=new ArrayList<>();
			
			String nameBase=Config.language+".xml";
			for(String loc:Config.initFileLocation){
				URL res=l.getResource(loc+'/'+nameBase);
				
				if(res!=null)
					ret.add(res);
			}
			
			return ret;
		};
		
		var possible=getPossibleLocations.apply(loader);
		var fromParent=getPossibleLocations.apply(loader.getParent());
		
		if(possible==null){
			System.err.println("Can't list files in module \""+loader.getName()+"\"to find init files.");
			return;
		}
		
		if(fromParent!=null)						//Class loader gives results from itself and parent. We want only ones from this specific class loader
			possible.removeAll(fromParent);
		
		for(URL url:possible){
			try {
				Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url.openStream());

				parseModules(doc,loader);
			} catch (SAXException | IOException | ParserConfigurationException e) {
				System.err.println("Failed to load init file \""+url+"\" from extension \""+loader.getName()+'"');
				e.printStackTrace();
			}
		}
	}
	/**Parse modules from given document using given class loader
	 * @param doc XML document containing modules description.
	 * @param loader Class loader to use to obtain Modules classes. Usually ClassLoader loading from same jar file as doc data are stored.
	 * */
	private static void parseModules(Document doc, ClassLoader loader){
		readModules(doc,"static","Static",loader,true);
		inputFilters.addAll(readModules(doc,"intendFilters","Intend filter",loader,false));
		orders.addAll(readModules(doc,"orders","Order",loader,false));

		List<IntendGetter> getters=readModules(doc,"intendGetters","Intend getter",loader,false);
		for(IntendGetter getterTemplate:getters){
			Thread getter=new Thread(inputGetters,getterTemplate,getterTemplate.getterName);
			getter.setDaemon(true);
			getter.start();
		}
	}
	/**Initialize modules by xml node and return their instances.
	 * @param where Document containing initializers.
	 * @param groupName Name of parent node containing subnodes of modules to initialize.
	 * @param printableName Name to print in logs.
	 * @param loader Class loader to use to get modules classes
	 * @param staticInit If these modules all initialized on static way. If true static method "staticInit" is called, constructor is called. Both take one parameter - xml Element
	 * @param <T> Type of modules to return. Ignored if staticInit is true.
	 * @return List of initialized modules instances. It's always empty if staticInit is true.
	 */
	private static <T> List<T> readModules(Document where,String groupName, String printableName, ClassLoader loader, boolean staticInit){
		List<T> ret=new LinkedList<T>();

		String timePrintTemplate=printableName+" %-"+(45-printableName.length())+"s loaded in \t%8.3f ms.%n";
		//for "order" noduleName it should be "Order %-40s loaded in \t%8.3f ms.%n"

		long bigTime=System.nanoTime();
		
		Node modulesNode=where.getElementsByTagName(groupName).item(0);
		if(modulesNode==null)
			return ret;
		NodeList ordersNode = ((Element)modulesNode).getChildNodes();
		
		for (int i = 0; i < ordersNode.getLength(); i++) {
			Node node = ordersNode.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) node;
				try {
					String className=e.getAttribute("classname");
					if(className.equals("")) {
						registerLoadException();
						System.err.println("Classname of an "+printableName+" is not specified.");
						continue;
					}
					long time=System.nanoTime();
					
					if(staticInit){
						Class.forName(className, true, loader).getMethod("staticInit", Element.class).invoke(null, e);
					} else {
						T module = (T) loader.loadClass(className).getDeclaredConstructor(Element.class).newInstance(e);
//						T module = (T) Class.forName(className, true, loader).getDeclaredConstructor(Element.class).newInstance(e);
						ret.add(module);
					}

					commands.printf(timePrintTemplate,className,(System.nanoTime()-time)/1000000f);
				} catch(ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
					Engine.registerLoadException();
					System.err.println("Failed to parse \""+e.getAttribute("classname")+"\" "+printableName+" from main file.");
					ex.printStackTrace();
				}
			}
		}

		commands.printf(printableName+" loaded in \t%8.3f ms.%n",(System.nanoTime()-bigTime)/1000000f);

		return ret;
	}
	public static void saveMainFile(){
		for(var order:orders)order.save();									//Flush all orders save data
		
		if(!Config.testMode)
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
		Logger.flush();
		System.out.flush();
		System.err.flush();
	}

	/**Interpret given intend.
	 * @param intend Intend to interpret.
	 * @return executed Executable matching this Intend.
	 */
	private static Executable interpret(Intend intend){
//		if(intend.isEmpty())return null;

		commands.println("\n"+ intend.rawValue+':');
		info.println("\n<Parsing><Parsing><Parsing><Parsing><Parsing><Parsing>"+ intend.rawValue+':');

		commands.println("\t"+ intend);
		info.println("\t"+ intend);

		Executable toExec=getExecutable(intend);

		if(toExec!=null&&toExec.cost.isMatched()) {
			info.println("<Execution><Execution><Execution><Execution>");
			toExec.execute();
			info.println("<End><End><End><End><End><End><End><End><End>");
			lastExecutionTime=System.currentTimeMillis();
			return toExec;
		} else {
			Engine.debug.println("cannot execute: "+ intend.value);
			return null;
		}
	}
	/**Get executable matching given Intend.
	 * @param intend Intend used to search for executable.
	 * @return Executable with best match to given Intend.
	 */
	public static Executable getExecutable(Intend intend) {
		Executable Return=null;
		float minCost=Short.MAX_VALUE;
		
		long time=System.nanoTime();

		info.println(intend+":");
		for (Order order : orders) {
			List<Executable> execs=order.getExecutables(intend);

			if(execs==null||execs.isEmpty())continue;
			for(Executable exec:execs)
				info.println("\t"+exec.action.getClass().toString()+" "+exec.cost.toString());

			Executable best=Collections.min(execs, Comparator.comparing(a -> a.cost));
			if(Return==null||best.cost.compareTo(Return.cost)<0)
				Return=best;
		}
		
		commands.printf("Search time: %.3f ms%n",(System.nanoTime()-time)/1000000f);
		if(Return!=null) {
			commands.println("Executing "+Return.action.getClass().toString()+" "+Return.cost);
		}
		
		return Return;
	}

	/**Registers new intend.
	 * @param intend Intend to register.
	 */
	public static void registerIntend(Intend intend){
		intends.add(intend);
	}
	/**Get filtered intend. It's blocking method, if no intend available wait's until it comes.
	 * @return The oldest registered intend.
	 * @throws InterruptedException If thread is interrupted during waiting for new Intend.
	 */
	public static Intend getIntend() throws InterruptedException {
		return filteredIntends.take();
	}

	/**Return previously executed Executable.
	 * @return Look above.
	 */
	public static Executable getLastExecuted() {
		return lastExecuted;
	}

	/**Checks if user confirmed given license.
	 * @param name Name of license which confirmation are being checked.
	 * @param displayConfirmation If ask user for confirmation if not already got.
	 * @return If license confirmed.
	 */
	public static boolean getLicenseConfirmation(String name,boolean displayConfirmation) {
		String nameToSearch=name.replace('.', '-').toLowerCase();
		boolean confirmed=Storage.getBool(Storage.userData,"acceptedLicences."+nameToSearch);
		if(confirmed)return true;
		if(!displayConfirmation)return false;
		
		System.out.printf(licenseConfirmationMessage.toString()+"%n",name);

		Boolean confirmed2=isInputPositive((ShaniString) ShaniCore.getIntend(ShaniString.class).value);
		if(confirmed2==null)return false;
		if(confirmed2)Storage.writeBool(Storage.userData, "acceptedLicences."+nameToSearch, true);
		return confirmed2;
	}
	/**Checks if input is positive response.
	 * Return true for positive/agreeing one (yes,yup),
	 * false for negative/disagreeing (no,nope),
	 * or false for unrecognized one.
	 * Actual positive/negative pattern depends on value stored in config file(TODO move to mainFile)
	 * @param input ShaniString to check if is positive response.
	 * @return If given input indicates positive response (return true), negative (returns false) or is unknown(null).
	 */
	public static Boolean isInputPositive(ShaniString input) {
		if(input.isEmpty())return null;
		if(Config.positiveResponseKey.equals(input))return true;
		if(Config.negativeResponseKey.equals(input))return false;
		return null;
	}

	/**Safely close shani.*/
	public static void exit() {
		debug.println("exit\n");
		System.out.println(closeMessage);

		inputGetters.interrupt();

		try {
			Thread.sleep(700);
		} catch (InterruptedException ignored) {}
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
		inputGetters.interrupt();
		flushBuffers();
		saveMainFile();
	}
}