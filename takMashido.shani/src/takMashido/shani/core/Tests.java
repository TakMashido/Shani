package takMashido.shani.core;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import takMashido.shani.libraries.ChainIterator;
import takMashido.shani.libraries.Pair;
import takMashido.shani.orders.Executable;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Tests {
	private Tests(){}
	
	private static Map<String,Object> commandResults=new HashMap<>();
	private static Node expectedResults=null;
	
	private static Set<URL> usedManifests=new HashSet<>();				//Set of already use manifests, included to avoid running single one multiple times
	
	private static ChainIterator<Node> testIterator=new ChainIterator<>();
	private static Element currentTest;
	
	private static int passedTests=0;
	private static int failedTests=0;
	
	/** Add new extensions to test.
	 * @param extensionLoaders Class loaders of all extensions. Each one can contains it's own .xml test describer
	 */
	public static void addTests(List<ClassLoader> extensionLoaders) {
		if (!Config.testMode) {
			assert false : "Can't runTests if not in test mode.";
			return;
		}
		
		String namePostfix='/'+Config.language+".xml";
		for(ClassLoader loader:extensionLoaders) {
			for(String prefix:Config.testManifestLocation) {
				URL res=loader.getResource(prefix+namePostfix);
				
				if(res==null||usedManifests.contains(res))
					continue;
				usedManifests.add(res);
				
				try {
					Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(res.openStream());
					
					NodeList nodes=Storage.getNodes(doc,"shaniTests.end2end.test");
					
					if(nodes==null)
						continue;
					
					testIterator.addIterator(new NodeListIterator(nodes));
				} catch (SAXException|IOException|ParserConfigurationException e) {
					++failedTests;
					System.out.println("Failed to load test config \""+res+'"');
					e.printStackTrace();
				}
			}
		}
	}
	
	/**Run next enqueued test.
	 * @return True if test started, false if no more tests found.
	 */
	public static boolean nextTest(){
		//TODO add info when opening new test stream
		
		if(!testIterator.hasNext())
			return false;
		currentTest=(Element)testIterator.next();
		
		Element intendNode=(Element)Storage.getNode(currentTest,"intend");
		if(intendNode==null){
			System.out.printf("Test \"%s\" do not contain Intend template.\n",currentTest.getAttribute("name"));
			failedTests++;
			return nextTest();
		}
		
		expectedResults=Storage.getNode(currentTest,"responses");
		
		IntendBase intend;
		try {
			Object rawIntend=Class.forName(intendNode.getAttribute("classname")).getDeclaredConstructor(Node.class).newInstance(intendNode);
			if(!(rawIntend instanceof IntendBase)) {
				System.out.println('"' + intendNode.getAttribute("classname") + "\" is not IntendBase child class.");
				failedTests++;
				return nextTest();
			}
			
			intend=(IntendBase)rawIntend;
		} catch (InstantiationException|IllegalAccessException|InvocationTargetException|NoSuchMethodException|ClassNotFoundException e) {
			System.out.println("Can't create intent instance.");
			e.printStackTrace();
			failedTests++;
			return nextTest();
		}
		
		commandResults.clear();
		
		ShaniCore.interpret(intend);
		
		return true;
	}
	
	/**Called by engine after intend parsing is complete. */
	public static void testEnded(Executable executed){
		assert currentTest!=null;
		
		if(executed==null){
			if(Storage.getNode(currentTest,"noExecution")!=null)
				passedTests++;
			else{
				failedTests++;
				System.out.println("Test: \"" + currentTest.getAttribute("name")+" do not lead to Executable creation.");
			}
			
			return;
		}

		//If noExecution node is present executable shouldn't be created.
		try{
			Storage.getNode(currentTest,"noExecution");			//source of exception

			failedTests++;
			System.out.println("Test: \"" + currentTest.getAttribute("name")+" created Executable but shouldn't.");
			return;
		} catch(Storage.NodeNotPresentException ex){
			//Empty, just continue normal check.
		}

		commandResults.put("actionClass",executed.action.getClass().getCanonicalName());
		
		boolean passed=true;
		
		//Check execution results
		for(String key:commandResults.keySet()){
			Node template=Storage.getNode(expectedResults,key);
			
			//Check if result should exist
			if(template==null){
				if(passed){
					passed=false;
					System.out.println("\nError during running: \"" + currentTest.getAttribute("name") + '"');
				}
				
				System.out.printf("Extra key \"%s\" found with value \"%s\".\n",key,commandResults.get(key));
				continue;
			}
			
			//Check if correct value
			Pair<Boolean,?> result=compare((Element)template,commandResults.get(key));
			if(!result.first){
				if(passed){
					passed=false;
					System.out.println("\nError during running: \"" + currentTest.getAttribute("name") + '"');
				}
				
				System.out.printf("%s: expected \"%s\" got \"%s\".\n",key,result.second,commandResults.get(key));
			}
		}
		
		
		//Find any not provided result
		if(expectedResults!=null){
			NodeList templates=expectedResults.getChildNodes();
			for(int i=0; i<templates.getLength(); i++){
				Node templateNode=templates.item(i);
				if(templateNode.getNodeType()!=Node.ELEMENT_NODE)
					continue;
				
				String key=templateNode.getNodeName();
				if(!commandResults.containsKey(key)){
					if(passed){
						passed=false;
						System.out.println("\nError during running: \""+currentTest.getAttribute("name")+'"');
					}
					
					System.out.printf("Key \"%s\" not present in execution results.\n", key);
				}
			}
		}
		
		if(passed){
			passedTests++;
		} else {
			failedTests++;
		}
	}
	
	/**Compare value given by execution with value template
	 * @return Pair: first if execution successful, second: expected value
	 */
	private static Pair<Boolean,?> compare(Element template, Object value){
		String type=template.getAttribute("type");
		String val=template.getAttribute("val");
		
		switch (type){
		case "bool":
		case "boolean":
			Boolean expectedBool=Boolean.parseBoolean(val);
			return new Pair<Boolean,Boolean>(
					(Boolean)((value instanceof Boolean)&&expectedBool==value),
					expectedBool);
		case "int":
		case "integer":
			Integer expectedInt=Integer.parseInt(val);
			return new Pair<Boolean,Integer>(
					(value instanceof Integer)&& expectedInt.equals(value),
					expectedInt);
		case "float":
			float epsilon=.01f;
			
			String ep=template.getAttribute("epsilon");
			if(!ep.isEmpty())
				epsilon=Float.parseFloat(ep);
			
			Float expectedFloat=Float.parseFloat(val);
			
			return new Pair<Boolean,Float>(
					(value instanceof Float)&&Math.abs(expectedFloat-(Float)value)<epsilon,
					expectedFloat);
			case "double":
				double doubleEpsilon=.0001d;
				
				ep=template.getAttribute("epsilon");
				if(!ep.isEmpty())
					doubleEpsilon=Double.parseDouble(ep);
				
				Double expectedDouble=Double.parseDouble(val);
				
				return new Pair<Boolean,Double>(
						(value instanceof Double)&&Math.abs(expectedDouble-(Double)value)<doubleEpsilon,
						expectedDouble);
		case "":
		case "string":
			return new Pair<Boolean,String>
					((value instanceof String)&& val.equals(value),
					val);
		default:
			System.err.println("Unrecognized type in template: \""+type+'"');
			return new Pair<Boolean,Object>(false,null);
		}
	}
	
	/**Print summary of test run so far. Include number of passed and failed tests.*/
	public static void printSummary(){
		System.out.println("\n-------------------------------");
		System.out.println("Tests summary:");
		System.out.printf("Passed %d tests\n",passedTests);
		if(failedTests!=0)
			System.out.printf("Failed %d tests, look into log for more info\n",failedTests);
		else
			System.out.println("Congratulations, no tests failed.");
		System.out.println();
	}
	
	/**Add results of called test. Each entry in array is treated as separate value.
	 * @param keys Array of properties keys.
	 * @param values Array of properties values.
	 */
	public static void addResults(String[] keys,Object[] values){
		if(!Config.testMode)
			return;
		
		for(int i=0;i<keys.length;++i)
			addResults(keys[i],values[i]);
	}
	/**Add results of called test.
	 * @param key Property key
	 * @param value Property value
	 */
	public static void addResults(String key,Object value){
		if(!Config.testMode)
			return;
		
		commandResults.put(key,value);
	}
	
	static class NodeListIterator implements Iterator<Node> {
		private NodeList nodes;
		private int currentIndex=0;
		
		NodeListIterator(NodeList list){
			nodes=list;
		}
		
		@Override
		public boolean hasNext() {
			return currentIndex<nodes.getLength();
		}
		
		@Override
		public Node next() {
			return nodes.item(currentIndex++);
		}
	}
}
