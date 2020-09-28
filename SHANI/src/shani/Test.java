package shani;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import liblaries.DOMWalker;

public class Test {
	public static final String passed="OK";
	public static final String notPassed="Failed";
	
	@SuppressWarnings("unused")
	public static void main(String[]args) throws SAXException, IOException, ParserConfigurationException {
		int globalErrors=0;
		int errors=0;
		
		if(false) {
			errors=shaniStringComparingTest();
			System.out.println("Test finished."+(errors!=0?" Found "+errors+" errros.":""));
			globalErrors+=errors;
		}
		if(true) {
			errors=sentenceMatcherTest();
			System.out.println("Test finished."+(errors!=0?" Found "+errors+" errros.":""));
			globalErrors+=errors;
		}
		
		System.out.println("All tests finished"+(globalErrors!=0?". Found "+globalErrors+" errros.":" successfully - no errros found."));
	}
	
	public static int shaniStringComparingTest() {
		System.out.println("Testing single word ShaniString comapritions...");
		int errors=0;
		
		Engine.initialize(new String[] {"-d"});
		
		var str=new ShaniString("w³¹cz");
		
		String[] data=new String[]{"w³¹cz","³w¹cz","lw¹cz","w¹³cz"};
		short[] expected=new short[] {
			0,
			Config.characterSwapCost,
			(short)(Config.characterSwapCost+Config.nationalSimilarityCost),
			Config.characterSwapCost
		};
		
		System.out.println(str.toFullString()+':');
		for(int i=0;i<data.length;i++) {
			short cost=str.getCompareCost(data[i]);
			
			if(cost!=expected[i]) {
				errors++;
			}
			
			System.out.printf("\t%s:%d\t%s%n", data[i], cost, cost==expected[i]?passed:notPassed);
		}
		
		return errors;
	}
	
	public static int sentenceMatcherTest() throws SAXException, IOException, ParserConfigurationException {
		System.out.println("Testing sentence matcher...");
		int errors=0;
		
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("test.xml"));
		doc.getDocumentElement().normalize();
		
		var matcher=new SentenceMatcher(DOMWalker.walk(doc, "tests/sentenceMatcher/test"));
		
		String[] data=new String[] {"just word","must work"};
		short[] cost=new short[] {0,0};
		short[] importanceBias=new short[] {Config.sentenceMatcherWordReturnImportanceBias,Config.sentenceMatcherWordReturnImportanceBias};
		String[] name=new String[] {"1","1"};
		String[][][] dataReturn=new String[][][] {
			{{"regex","just"},{"data","word"}},
			{{"regex","must"},{"data","work"}}
		};
		
		HashMap<String,String>[] dataReturnMap=new HashMap[dataReturn.length];
		for(int i=0;i<dataReturn.length;i++) {
			dataReturnMap[i]=new HashMap<String,String>();
			for(int j=0;j<dataReturn[i].length;j++) {
				dataReturnMap[i].put(dataReturn[i][j][0], dataReturn[i][j][1]);
			}
		}
		
		for(int i=0;i<data.length;i++) {
			var result=matcher.processBest(data[i]);
			
			boolean good=result.cost==cost[i]&&result.importanceBias==importanceBias[i]&&result.name.contentEquals(name[i])&&result.data.equals(dataReturnMap[i]);
			if(!good)errors++;
			
			System.out.printf("%s:\t\"%s\":%d:%d:%s\t%s%n", data[i], result.name, result.cost, result.importanceBias, result.data, good?passed:notPassed);
		}
		
		return errors;
	}
}