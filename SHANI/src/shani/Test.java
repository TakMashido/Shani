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
		
		System.out.println("\nAll tests finished"+(globalErrors!=0?". Found "+globalErrors+" errros.":" successfully - no errros found."));
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
		
		String[] data=new String[] {
				"just data log",
				"must work hog",
				"temporayr sentence",
				"hog sentente",
				"witam pana genera³a pu³kownika",
				"must kolege piotra rybê hello world"};
		short[] cost=new short[] {0,0,Config.characterSwapCost,Config.diffrendCharacterCost,0,(short)(Config.nationalSimilarityCost)};
		short[] importanceBias=new short[] {
				(short) (Config.sentenceMatcherWordReturnImportanceBias+2*Config.sentenceMatcherRegexImportanceBias),
				(short) (Config.sentenceMatcherWordReturnImportanceBias+2*Config.sentenceMatcherRegexImportanceBias),
				0,
				Config.sentenceMatcherRegexImportanceBias,
				Config.sentenceMatcherWordReturnImportanceBias,
				(short) (2*Config.sentenceMatcherWordReturnImportanceBias+Config.sentenceMatcherRegexImportanceBias),
				};
		String[] name=new String[] {"1","1","or","or","anyOrder","anyOrder"};
		String[][][] dataReturn=new String[][][] {
			{{"regex","just"},{"data","data"},{"regex2","log"}},
			{{"regex","must"},{"data","work"},{"regex2","hog"}},
			{},
			{{"regex2","hog"}},
			{{"data","pu³kownika"}},
			{{"regex","must"},{"data","piotra rybê"}}
		};
		
		int length=data.length;
		if(cost.length!=length) {
			System.out.println("Corrupted test, wrong amount of costs provided.");
			return -1;
		}
		if(importanceBias.length!=length) {
			System.out.println("Corrupted test, wrong amount of importanceBias'es provided.");
			return -1;
		}
		if(name.length!=length) {
			System.out.println("Corrupted test, wrong amount of names provided.");
			return -1;
		}
		if(dataReturn.length!=length) {
			System.out.println("Corrupted test, wrong amount of dataReturn's provided.");
			return -1;
		}
		
		@SuppressWarnings("unchecked")
		HashMap<String,String>[] dataReturnMap=new HashMap[dataReturn.length];
		for(int i=0;i<dataReturn.length;i++) {
			dataReturnMap[i]=new HashMap<String,String>();
			for(int j=0;j<dataReturn[i].length;j++) {
				dataReturnMap[i].put(dataReturn[i][j][0], dataReturn[i][j][1]);
			}
		}
		
		for(int i=0;i<data.length;i++) {
			System.out.println();
			var results=matcher.process(data[i]);
			for(var res:results) System.out.println(res);
			
			var result=SentenceMatcher.getBestMatch(results);
			if(result==null) {
				System.out.printf("\"%s\": match not found.\t%s%n", data[i], notPassed);
				errors++;
				continue;
			}
			
			boolean good=result.cost==cost[i]&&result.importanceBias==importanceBias[i]&&result.name.contentEquals(name[i])&&result.data.equals(dataReturnMap[i]);
			if(!good)errors++;
			
			System.out.printf("\"%s\":\t\"%s\":%d:%d:%s\t%s%n", data[i], result.name, result.cost, result.importanceBias, result.data, good?passed:notPassed);
			System.out.printf("tests: %s %s %s %s%n",result.name.contentEquals(name[i]),result.cost==cost[i],result.importanceBias==importanceBias[i],result.data.equals(dataReturnMap[i]));
		}
		
		return errors;
	}
}