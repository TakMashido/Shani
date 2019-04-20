package shani;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import shani.SearchEngine.SearchResoults.SearchResoult;

public class SearchEngine {
	private static final Pattern websideMainAddressPattern=Pattern.compile("^(?:http://|https://)(?:[\\w\\d\\.]*\\.)?([\\w\\d\\-]+\\.[\\w\\d]+)(?:/[\\w\\d\\-._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;%=]*)?$");
	
	public static SearchResoults search(String query) throws IOException {
		var Return=new SearchResoults();
		
		Engine.getLicenseConfirmation("JSOUP");
		Engine.getLicenseConfirmation("duckduckgo.com");
		
		var doc=Jsoup.connect("https://duckduckgo.com/html?q="+query).get();
		
		var entries=doc.getElementsByClass("result");
		for(var entry:entries)Return.add(Return.new SearchResoult(entry));
		
		return Return;
	}
	
	public static class SearchResoults extends ArrayList<SearchResoult>{
		private static final long serialVersionUID = 3867440709404455784L;
		
		public SearchResoults selectElementsByDomain(String address) {
			int length=this.size();
			for(int i=0;i<length;i++) {
				if(!this.get(i).getMainAddress().equals(address)) {
					this.remove(i--);
					length--;
				}
			}
			return this;
		}
		public SearchResoults removeElementsByDomain(String address) {
			int length=this.size();
			for(int i=0;i<length;i++) {
				if(this.get(i).getMainAddress().equals(address)) {
					this.remove(i--);
					length--;
				}
			}
			return this;
		}
		
		public SearchResoults selectElementsWithTitleContaining(String s) {
			int length=this.size();
			for(int i=0;i<length;i++) {
				if(!this.get(i).title.contains(s)) {
					this.remove(i--);
					length--;
				}
			}
			return this;
		}
		public SearchResoults removeElementsWithTitleContaining(String s) {
			int length=this.size();
			for(int i=0;i<length;i++) {
				if(this.get(i).title.contains(s)) {
					this.remove(i--);
					length--;
				}
			}
			return this;
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
}