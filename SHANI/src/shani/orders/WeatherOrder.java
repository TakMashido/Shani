package shani.orders;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
	
	@Override
	protected boolean initialize() {
		weatherSentence=new SentenceGenerator(orderFile.getElementsByTagName("weathersentence").item(0));
		dayChooser=new SentenceMatcher(orderFile.getElementsByTagName("daychooser").item(0));
		
		return true;
	}
	
	private enum Weather{																		//Make this more complecs. Some identifiers can have other meaning for other day time and enum doesn't rocognize it
		//To add: Showers,PM Showers,AM Showers,Scattered Thunderstorms,Partly Cloudy/Wind,PM Thunderstorms,AM Thunderstorms
		sunny,mostlySunny,partlySunny,clear,mostlyClear,partlyCloudy,mostlyCloudy,cloudy,lightRain,rain,none;
		
		private static final String sunnyIdentifier			= "Sunny";					//no sense to move to main file, depends on weather website. If's get changed other things on webside also will change and HTML Scraping part have to rewriten.
		private static final String mostlySunnyIdentifier	= "Mostly Sunny";
		private static final String partlySunnyIdentifier 	= "Partly Sunny";
		private static final String clearIdentifier			= "Clear";
		private static final String mostlyClearIdentifier 	= "Mostly Clear";
		private static final String partlyCloudyIdentifier 	= "Partly Cloudy";
		private static final String mostlyCloudyIdentifier 	= "Mostly Cloudy";
		private static final String cloudyIdentifier 		= "Cloudy";
		private static final String lightRainIdentifier 	= "Light Rain";
		private static final String rainIdentifier 			= "Rain";
		
		private static final ShaniString sunnyRespond			= new ShaniString("s³onecznie");
		private static final ShaniString mostlySunnyRespond		= new ShaniString("w wiêkszoœci s³onecznie");
		private static final ShaniString partlySunnyRespond		= new ShaniString("lekko s³onecznie");
		private static final ShaniString clearRespond			= new ShaniString("czyste niebo");
		private static final ShaniString mostlyClearRespond		= new ShaniString("g³ównie czyste niebo");
		private static final ShaniString partlyCloudyRespond	= new ShaniString("czêœciowo zachmurzone");
		private static final ShaniString mostlyCloudyRespond	= new ShaniString("glównie zachmurzenie");
		private static final ShaniString rainRespond			= new ShaniString("deszcz");
		private static final ShaniString cloudyRespond			= new ShaniString("pochmurnie");
		private static final ShaniString lightRainRespond		= new ShaniString("lekki deszcz");
		private static final ShaniString noneRespond			= new ShaniString("nieznana pogoda");
		
		public static Weather getWeather(String repWord) {
			switch(repWord) {
			case sunnyIdentifier		: return sunny;
			case mostlySunnyIdentifier	: return mostlySunny;
			case partlySunnyIdentifier 	: return partlySunny;
			case clearIdentifier		: return clear;
			case mostlyClearIdentifier	: return mostlyClear;
			case partlyCloudyIdentifier	: return partlyCloudy;
			case mostlyCloudyIdentifier : return mostlyCloudy;
			case cloudyIdentifier 		: return cloudy;
			case lightRainIdentifier 	: return lightRain;
			case rainIdentifier 		: return rain;
			default						: System.err.println("Unknow weather identifier: "+repWord.toString());
										  return none;
			}
		}
		
		public String toString() {
			switch(this) {
			case sunny			: return sunnyRespond		.toString();
			case mostlySunny	: return mostlySunnyRespond	.toString();
			case partlySunny	: return partlySunnyRespond	.toString();
			case clear			: return clearRespond		.toString();
			case mostlyClear	: return mostlyClearRespond	.toString();
			case partlyCloudy	: return partlyCloudyRespond.toString();
			case mostlyCloudy	: return mostlyCloudyRespond.toString();
			case cloudy			: return cloudyRespond		.toString();
			case lightRain		: return lightRainRespond	.toString();
			case rain			: return rainRespond		.toString();
			case none			: return noneRespond		.toString();
			default				: assert false:"Unknow label in WeatherOrder.Weather";
								 return null;
			}
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
				
				weatherSentence.printOut(sentenceName,addParameters);
				
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return true;
		}
	}
	
	private static class DayWeather{								//have also day sunrise and sunset hours left to proccess in constructor.
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
			
			weather=Weather.getWeather(e.getElementsByClass("description").get(0).text());
			
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