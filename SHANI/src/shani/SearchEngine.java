package shani;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class SearchEngine {
	private static final Pattern websideMainAddressPattern=Pattern.compile("^(?:http://|https://)(?:[\\w\\d\\.]*\\.)?([\\w\\d-]+\\.[\\w\\d-]+)(?:/[\\w\\d-._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;%=]*)?$");
	
	public static SearchResoults search(String query) throws IOException {
		var Return=new SearchResoults();
		
//		Engine.getLicenseConfirmation("JSOUP");
//		Engine.getLicenseConfirmation("duckduckgo.com");
		
//		var doc=Jsoup.connect("https://duckduckgo.com/html?q="+query).get();
		var doc=Jsoup.parse(new File("ducktest.html"), "UTF-8");
		
//		var out=new PrintStream(new FileOutputStream("ducktest.html"));
//		out.print(doc.html());
//		out.close();
		
		var entries=doc.getElementsByClass("result");
		for(var entry:entries)Return.resoults.add(Return.new SearchResoult(entry));
		
		return Return;
	}
	
	public static class SearchResoults{
		public final List<SearchResoult> resoults=new LinkedList<>();
		
		public void removeElementsByMainAddress(String address) {
			int length=resoults.size();
			for(int i=0;i<length;i++) {
				if(resoults.get(i).getMainAddress().equals(address)) {
					resoults.remove(i--);
					length--;
				}
			}
		}
		public void selectElementsByMainAddress(String address) {
			int length=resoults.size();
			for(int i=0;i<length;i++) {
				if(!resoults.get(i).getMainAddress().equals(address)) {
					resoults.remove(i--);
					length--;
				}
			}
		}
		
		public class SearchResoult{
			public final String url;
			public final String title;
			private String mainAddress=null;
			
			public SearchResoult(Element elem) {
				var titleElement=elem.getElementsByClass("result__a").get(0);
				title=titleElement.text();
				url=titleElement.attr("href");
			}
			
			/**Returns part of link being webside identifier. E.g. for "en.wikipedia.org/wiki/A" return "wikipedia.org"
			 * @return Look above.
			 */
			public String getMainAddress() {
				if(mainAddress!=null)return mainAddress;
				
				var matcher=websideMainAddressPattern.matcher(url);
				assert matcher.matches():"Fix websideMainAddressPattern regex Pattern in SearchEngine. It didn't match following url: \""+url+'"';
				mainAddress=matcher.group(1);
				return mainAddress;
			}
			
			public String toString() {
				return url+" :::: "+title;
			}
		}
	}
	
	public static void main(String[]args) throws IOException {
		var a=search("a");
		a.selectElementsByMainAddress("wikipedia.org");
		for(var res:a.resoults)System.out.println(res);
	}
}