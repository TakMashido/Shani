package shani;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import shani.SearchEngine.SearchResoults.SearchResoult;

/**Class automating web search.
 * @author TakMashiddo
 */
public class SearchEngine {
	private static final Pattern websideDomainPattern=Pattern.compile("^(?:http://|https://)(?:[\\w\\d\\.-]*\\.)?([\\w\\d\\-]+\\.[\\w\\d]+)(?:/[\\w\\d\\-._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;%=]*)?$");
	
	/**Returned by {@link SearchEngine#search(String)} if one of required licenses are not confimed,
	 * save way to pass information about it wchich do not requaire additional handling.
	 * 
	 */
	public static final SearchResoults LICENSENOTCONFIRMED=new SearchResoults();
	public static final SearchResoults EMPTYSEACHRESOULTS = new SearchResoults();
	
	/**Private constructor to don't allow to create instances.*/
	private SearchEngine() {}
	
	/**Search with given query in default search engine.
	 * @param query Query to search for.
	 * @return {@link SearchResoults} object containing search resoult. Or empty set {@link #LICENSENOTCONFIRMED} if one ore more of required licenses are not confirmed.
	 * @throws IOException If failed to connect to search engine site.
	 */
	public static SearchResoults search(String query) throws IOException {
		var Return=new SearchResoults();
		
		if(!Engine.getLicenseConfirmation("JSOUP")
		  ||!Engine.getLicenseConfirmation("duckduckgo.com"))
			return LICENSENOTCONFIRMED;
		
		
		query.replaceAll(":", "%3A");
		var doc=Jsoup.connect("https://duckduckgo.com/html?q="+query).get();
		
		File file=new File("Test.html");
		file.createNewFile();
		PrintWriter writer=new PrintWriter(file);
		writer.print(doc.html());
		writer.close();
		
		var entries=doc.getElementsByClass("result");
		for(var entry:entries) {
			try {
				Return.add(Return.new SearchResoult(entry));
			} catch(IndexOutOfBoundsException ex) {
				System.err.printf("Failed to parse element for query %s",query);
			}
		}
		
		return Return;
	}
	
	public static boolean wasLicensesConfirmed(SearchResoults sr) {
		return sr!=LICENSENOTCONFIRMED;
	}
	
	/**Contain {@link SearchResoult} representing found resoults.*/
	public static class SearchResoults extends ArrayList<SearchResoult>{
		private static final long serialVersionUID = 3867440709404455784L;
		
		/**Delete all elements which are from other domains.
		 * @param address Domain to seach for.
		 * @return this
		 */
		public SearchResoults selectElementsByDomain(String address) {
			int length=this.size();
			for(int i=0;i<length;i++) {
				if(!this.get(i).getDomain().equals(address)) {
					this.remove(i--);
					length--;
				}
			}
			return this;
		}
		/**Delete all elements which are from given domain.
		 * @param address Domain to seach for.
		 * @return this
		 */
		public SearchResoults removeElementsByDomain(String address) {
			int length=this.size();
			for(int i=0;i<length;i++) {
				if(this.get(i).getDomain().equals(address)) {
					this.remove(i--);
					length--;
				}
			}
			return this;
		}
		
		/**Deletes all elements not containig given string in title.
		 * @param s String to seach for.
		 * @return this
		 */
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
		/**Deletes all elements containig given string in title.
		 * @param s String to seach for.
		 * @return this
		 */
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
		
		/**Represent found sites.*/
		public class SearchResoult{
			/**Url of entry.*/
			public final String url;
			/**Title of entry*/
			public final String title;
			/**Description of entry*/
			public final String description;
			private String domain=null;
			
			public SearchResoult(Element elem) {
				var titleElement=elem.getElementsByClass("result__a").get(0);
				title=titleElement.text();
				url=titleElement.attr("href");
				description=elem.getElementsByClass("result__snippet").get(0).text();
			}
			
			/**Returns domain of website. E.g. for "en.wikipedia.org/wiki/A" return "wikipedia.org"
			 * @return Look above.
			 */
			public String getDomain() {
				if(domain!=null)return domain;
				
				var matcher=websideDomainPattern.matcher(url);
				if(!matcher.matches()) {
					System.err.println("Fix websideDomainPattern regex Pattern in SearchEngine. It didn't match following url: \""+url+'"');
					return "ERROR_DOMAIN_NOT_FOUND";
				}
				domain=matcher.group(1);
				return domain;
			}
			
			public String toString() {
				return url+" :::: "+title;
			}
		}
	}
}