package takMashido.shaniModules.orders;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.w3c.dom.Node;
import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.text.SentenceGenerator;
import takMashido.shani.core.text.SentenceMatcher;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.orders.SentenceMatcherOrder;
import takMashido.shani.tools.InputCleaners;
import takMashido.shani.tools.SearchEngine;
import takMashido.shani.tools.SearchEngine.SearchResoults.SearchResoult;

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

public class WeatherOrder extends SentenceMatcherOrder {
	private static ShaniString notKnowLocationMessage;
	private static ShaniString cannotProcessDayMessage;
	private static ShaniString cannotParseDayNumberMessage;
	private static ShaniString noForecastFoundMessage;
	private static ShaniString whichCityMessage;
	private static ShaniString cityOutOfBoundsMessage;
	private static ShaniString unmatchingCityMessage;
	
	private ShaniString connectionFailedMessage;
	
	private SentenceGenerator weatherSentence;
	
	private SentenceMatcher dayChooser;
	
	private static final DateFormat websiteDateFormat=new SimpleDateFormat("MMM dd",Locale.ENGLISH);
	
	private int badWeatherTemp;
	private int badWeatherWind;
	private String[] badWeatherTypes;
	
	public WeatherOrder(org.w3c.dom.Element e) {
		super(e);
		
		notKnowLocationMessage=ShaniString.loadString(e,"notKnowLocationMessage");          
		cannotProcessDayMessage=ShaniString.loadString(e,"cannotProcessDayMessage");        
		cannotParseDayNumberMessage=ShaniString.loadString(e,"cannotParseDayNumberMessage");
		noForecastFoundMessage=ShaniString.loadString(e,"noForecastFoundMessage");          
		whichCityMessage=ShaniString.loadString(e,"whichCityMessage");                      
		cityOutOfBoundsMessage=ShaniString.loadString(e,"cityOutOfBoundsMessage");          
		unmatchingCityMessage=ShaniString.loadString(e,"unmatchingCityMessage");            
		
		connectionFailedMessage=ShaniString.loadString(e,"connectionFailedMessage");
		
		weatherSentence=new SentenceGenerator(e.getElementsByTagName("weathersentence").item(0));
		dayChooser=new SentenceMatcher(e.getElementsByTagName("daychooser").item(0));
		
		var elems=e.getElementsByTagName("weatherResponses").item(0).getChildNodes();			//Initialize weather identifiers in Weather class
		for(int i=0;i<elems.getLength();i++) {
			var node=elems.item(i);
			if(node.getNodeType()==Node.ELEMENT_NODE) {
				Weather.responses.put(node.getNodeName().replaceAll("_", " ").replaceAll("-", "/"), new ShaniString(node.getTextContent()));
			}
		}
		
		elems=e.getChildNodes();
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
	}
	
	private static class Weather{
		private final String identifier; 
		
		private Weather(String identifier) {
			this.identifier=identifier;
			
			if(!responses.containsKey(identifier))System.err.println("Unknow weather identifier: "+identifier.toString());			//Make this in assert block???
		}
		
		private static final HashMap<String,ShaniString> responses=new HashMap<>();			//Data putted inside initialize method
		
		public ShaniString toShaniString() {
			return responses.get(identifier);
		}
		@Override
		public String toString() {
			ShaniString Return=toShaniString();
			if(Return==null)return identifier;
			return Return.toString();
		}
	}
	
	@Override
	protected SentenceMatcherAction actionFactory(String sentenceName, HashMap<String, String> returnValues) {
		return new WeatherAction();
	}
	
	private static ArrayList<DayWeather> getWeather(String where) throws IOException {					//TODO Cache site-city connection.
		var sr=SearchEngine.search('"'+where+"\" forecast site:weather.com/weather/tenday/l");
		
//		var sr=SearchEngine.search(where+" forecast site:weather.com/weather/tenday/l");

//		var sr=SearchEngine.search("weather.com 10 day weather "+where);
//		sr.selectElementsByDomain("weather.com").selectElementsWithTitleContaining("10-Day Weather Forecast");
		
		if(sr.isEmpty()) return null;
		
		var Return=new ArrayList<DayWeather>();
		
		sr.selectElementsWithTitleContaining(where,false);
		
		SearchResoult resoult;
		if(sr.size()>1) {
			whichCityMessage.printOut();
			
			String[] locations=new String[sr.size()];
			for(int i=0;i<locations.length;i++) {
				SearchResoult entry=sr.get(i);
				locations[i]=entry.title.substring(0,entry.title.indexOf("10-Day"));
				System.out.println(locations[i]);
			}
			
			int index=-1;
			String response=ShaniCore.getIntend(ShaniString.class).value.toString();
			try {
				index=Integer.parseInt(response);
				if(index<0||index>=sr.size()) {
					cityOutOfBoundsMessage.printOut();
					return null;
				}
				
				index++;
			} catch(NumberFormatException ex) {
				ShaniString res=new ShaniString(response,false);
				short minCost=Short.MAX_VALUE;
				int minIndex=-1;
				for(int i=0;i<locations.length;i++) {
					short cost=new ShaniString(locations[i]).getMatcher().apply(res).getMatchedCost();
					if(cost<minCost) {
						minCost=cost;
						minIndex=i;
					}
				}
				
				if(minCost>=ShaniCore.getSentenceCompareThreshold()){
					unmatchingCityMessage.printOut();
					return null;
				}
				index=minIndex;
			}
			
			resoult=sr.get(index);
		} else {
			resoult=sr.get(0);
		}
		System.out.println(resoult.title);
		
		Document doc=Jsoup.connect(resoult.url).get();
		
		var elems=doc.getElementsByClass("forecast-fiveday");
		if(elems.size()==0)
			return null;
		
		elems=elems.get(0).getElementsByClass("clickable");
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
		@Override
		protected boolean execute(String sentenceName, HashMap<String, String> returnValues) {
			if(!ShaniCore.getLicenseConfirmation("weather.com")) {
				return false;
			}
			
			String where=returnValues.get("where");
			if(where==null) {									//Add getting location (by IP address??/user home location from mainFile+(add Class for getting user info))
				notKnowLocationMessage.printOut();
				return false;
			}
			where=InputCleaners.stem(where);
			
			try {
				ArrayList<DayWeather> weather=getWeather(where);
				
				if(weather==null) {
					noForecastFoundMessage.printOut();
					return true;
				}
				
				int dayIndex=0;
				String when;
				if((when=returnValues.get("when"))!=null) {
					var res=dayChooser.process(when);
					if(res.length==0) {
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
				
				System.out.println(sentenceName);
				
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
				connectionFailedMessage.printOut();
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
		
		@Override
		public String toString() {
			return getParamsMap().toString();
		}
	}
}