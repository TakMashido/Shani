package shani.orders;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.w3c.dom.Node;

import shani.Engine;
import shani.SearchEngine;
import shani.SentenceGenerator;
import shani.SentenceMatcher;
import shani.ShaniString;
import shani.orders.templates.SentenceMatcherOrder;

public class WeatherOrder extends SentenceMatcherOrder {
	private ShaniString notKnowLocationMessage=new ShaniString("Nie wiem dla jakiego miejsca podaæ pogodê.");
	private ShaniString cannotProcessDayMessage=new ShaniString("Nie wiem o jaki dzieñ ci chodzi");
	private ShaniString cannotParseDayNumberMessage=new ShaniString("Aktualnie potrafiê zrozumieæ tylko liczbê podan¹ liczebnie nie s³ownie.");
	
	private SentenceGenerator weatherSentence;
	
	private SentenceMatcher dayChooser;
	
	private static final DateFormat websiteDateFormat=new SimpleDateFormat("MMM dd",Locale.ENGLISH);
	
	private int badWeatherTemp; 
	private int badWeatherWind;
	private String[] badWeatherTypes;
	
	@Override
	protected boolean initialize() {
		weatherSentence=new SentenceGenerator(orderFile.getElementsByTagName("weathersentence").item(0));
		dayChooser=new SentenceMatcher(orderFile.getElementsByTagName("daychooser").item(0));
		
		var elems=orderFile.getElementsByTagName("weatherResponses").item(0).getChildNodes();			//Initialize weather identifiers in Weather class
		for(int i=0;i<elems.getLength();i++) {
			var node=elems.item(i);
			if(node.getNodeType()==Node.ELEMENT_NODE) {
				Weather.responses.put(node.getNodeName().replaceAll("_", " ").replaceAll("-", "/"), new ShaniString(node.getTextContent()));
			}
		}
		
		elems=orderFile.getChildNodes();
		for(int i=0;i<elems.getLength();i++) {
			var node=elems.item(i);
			if(node.getNodeName().equals("badWeather")) {
				var elem=(org.w3c.dom.Element)node;
				badWeatherTemp=Integer.parseInt(elem.getElementsByTagName("temperature").item(0).getTextContent());
				badWeatherWind=Integer.parseInt(elem.getElementsByTagName("windSpeed").item(0).getTextContent());
				badWeatherTypes=new ShaniString(elem.getElementsByTagName("weatherTypes").item(0).getTextContent()).getArray();
				Arrays.sort(badWeatherTypes);
				break;
			}
		}
		
		return true;
	}
	
	private static class Weather{																		//Make this more complecs. Some identifiers can have other meaning for other day time and enum doesn't rocognize it
		//To add: PM Showers,AM Showers,Scattered Thunderstorms,Partly Cloudy/Wind,PM Thunderstorms,AM Thunderstorms
		
		private final String identifier; 
		
		private Weather(String identifier) {
			this.identifier=identifier;
			
			if(!responses.containsKey(identifier))System.err.println("Unknow weather identifier: "+identifier.toString());			//Make this in assert block???
		}
		
		private static final HashMap<String,ShaniString> responses=new HashMap<>();			//Data putted inside initialize method
		
		public ShaniString toShaniString() {
			return responses.get(identifier);
		}
		public String toString() {
			ShaniString Return=toShaniString();
			if(Return==null)return identifier;
			return Return.toString();
		}
	}
	
	protected SentenceMatcherAction actionFactory(String sentenceName, HashMap<String, String> returnValues) {
		return new WeatherAction();
	}
	
	private static ArrayList<DayWeather> getWeather(String where) throws IOException {
		var sr=SearchEngine.search("weather.com 10 day weather "+where);
		sr.selectElementsByDomain("weather.com").selectElementsWithTitleContaining("10-Day Weather Forecast");
		
		var Return=new ArrayList<DayWeather>();
		
		System.out.println("Fix city chosing."); 							//Now it gets first found city, and throw IndexOutOfBoundsException if no forecast found.
		Document doc=Jsoup.connect(sr.get(0).url).get();
		
		for(var res:sr){
			System.out.println(res.url+" "+res.title);
		}
		
		var elems=doc.getElementsByClass("forecast-fiveday").get(0).getElementsByClass("clickable");
		for(var elem:elems) {
			Return.add(new DayWeather(elem));
		}
		
		return Return;
	}
	
	private static final int fahrenheitToCelsius(int temp) {
		return (temp-32)*5/9;
	}
	private static final int mphToKmph(int speed) {
		return (int)(speed*1.609344);
	}
	
	private class WeatherAction extends SentenceMatcherAction{
		protected boolean execute(String sentenceName, HashMap<String, String> returnValues) {
			Engine.getLicenseConfirmation("weather.com");
			
			String where=returnValues.get("where");
			if(where==null) {									//Add getting location by IP address??
				notKnowLocationMessage.printOut();
				return false;
			}
			
			try {
				ArrayList<DayWeather> weather=getWeather(where);
				
				int dayIndex=0;
				String when;
				if((when=returnValues.get("when"))!=null) {
					var res=dayChooser.process(when);
					if(res.length<0) {
						System.out.println(cannotProcessDayMessage);
						return false;
					}
					if((when=res[0].getName()).equals("n")) {
						dayIndex=Integer.parseInt(res[0].data.get("number"));
					} else {
						try {
						dayIndex=Integer.parseInt(when);
						}catch(NumberFormatException ex) {
							System.out.println(cannotParseDayNumberMessage);
							return false;
						}
					}
				}
				
				var dayWeather=weather.get(dayIndex);
				var addParameters=dayWeather.getParamsMap();
				
				boolean badWeather=false;
				switch(sentenceName) {
				case "badWeather":
					badWeather=true;
				case "goodWeather":
					short score=0;
					if(Arrays.binarySearch(badWeatherTypes, dayWeather.weather.identifier)<0) score+=2;
					if(dayWeather.windSpeed>=badWeatherWind)score+=1;
					if(dayWeather.tempMin!=null) {
						if(dayWeather.tempMin<=badWeatherTemp)score+=1;
					} else if(dayWeather.tempMax!=null&&dayWeather.tempMax<=badWeatherTemp)score+=1;
					
					if(!badWeather&&score>=2)score=0;
					if(score>=2)weatherSentence.printOut("negativeRespond",addParameters);
					else weatherSentence.printOut("positiveRespond",addParameters);
					
					break;
				default:weatherSentence.printOut(sentenceName,addParameters);
				}
				
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				ShaniString.loadString("misc.connection.connectionFailedMessage").printOut();
			}
			
			return true;
		}
	}
	
	private static class DayWeather{								//Website provide also sunrise and sunset hours, proccess this in constructor.
		private Date date;
		private Weather weather;
		
		private Integer tempMax;				//null for unknown, in Celsius degrees
		private Integer tempMin;
		
		private Integer windSpeed;
		private String windDirection;
		
		private Integer humidity;				//in %
		
		private Integer precipitationChance;	//in %		
		
		private DayWeather(Element e) {
			String day=e.getElementsByClass("day-detail").text();
			try {
				date=websiteDateFormat.parse(day);
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
			
			weather=new Weather(e.getElementsByClass("description").get(0).text());
			
			var elems=e.getElementsByClass("temp").get(0).getElementsByTag("span");			//min and max temp
			try {																			//side can give "--" in this place.
				String temp=elems.get(0).text();
				tempMax=fahrenheitToCelsius(Integer.parseInt(temp.substring(0, temp.length()-1)));
			} catch(NumberFormatException ex) {}
			try {
				String temp=elems.get(2).text();
				tempMin=fahrenheitToCelsius(Integer.parseInt(temp.substring(0, temp.length()-1)));
			} catch(NumberFormatException ex) {System.out.println("b");}
			
			@SuppressWarnings("resource")
			Scanner wind=new Scanner(e.getElementsByClass("wind").text());
			windDirection=wind.next();
			windSpeed=mphToKmph(wind.nextInt());
			
			String temp=e.getElementsByClass("humidity").get(0).text();
			humidity=Integer.parseInt(temp.substring(0,temp.length()-1));
			
			temp=e.getElementsByClass("precip").get(0).text();
			precipitationChance=Integer.parseInt(temp.substring(0,temp.length()-1));
		}

		public Map<String,Object> getParamsMap() {
			Map<String,Object> Return=new HashMap<>();
			
			Return.put("weather", weather);
			Return.put("tempMax", tempMax);
			Return.put("tempMin", tempMin);
			Return.put("windSpeed",windSpeed);
			Return.put("windDirection",windDirection);
			Return.put("humidity", humidity);
			Return.put("precipitationChance", precipitationChance);
			
			return Return;
		}
	}
}