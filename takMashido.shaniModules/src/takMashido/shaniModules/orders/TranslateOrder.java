package takMashido.shaniModules.orders;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.orders.KeywordOrder;

public class TranslateOrder extends KeywordOrder{
	private int maxLineSize;
	private ShaniString translationSuccessMessage;
	private ShaniString translationUnsuccessMessage;
	
	private ShaniString connectionTimeoutMessage;
	private ShaniString connectionFailedMessage;
	
	public TranslateOrder(org.w3c.dom.Element e) {
		super(e);
		
		maxLineSize=Integer.parseInt(ShaniString.loadString(e,"maxLineSize").toString());
		translationSuccessMessage=ShaniString.loadString(e, "translationSuccessMessage");
		translationUnsuccessMessage=ShaniString.loadString(e,"translationUnsuccessMessage");
		
		connectionTimeoutMessage=ShaniString.loadString(e,"connectionTimeoutMessage");
		connectionFailedMessage=ShaniString.loadString(e,"connectionFailedMessage");
	}
	
	public KeywordActionNG actionFactory(org.w3c.dom.Element element) {return null;}
	
	public UnmatchedActionNG getUnmatchedAction() {
		return new TranslationAction();
	}
	
	private class TranslationAction extends UnmatchedActionNG{
		@Override
		public boolean connectAction(String action) {
			assert false:"Can't connect action to shani.orders.TranslatorOrder";
			System.err.println("Can't connect action to shani.orders.TranslatorOrder");
			return false;
		}

		@Override
		public boolean execute() {
			if(!(ShaniCore.getLicenseConfirmation("JSOUP")&&ShaniCore.getLicenseConfirmation("translatica.pl"))){
				return false;
			}
			var entries=getEntries(unmatched.toString());
			
			if(entries==null)return true;
			if(entries.size()>0) {
				System.out.printf(translationSuccessMessage.toString(),unmatched.toString());
				System.out.println();
			} else {
				System.out.printf(translationUnsuccessMessage.toString(),unmatched.toString());
				System.out.println();
				return true;
			}
			for(Entry entry:entries) {
				System.out.println(entry.originLanguage.substring(0,1).toUpperCase()+entry.originLanguage.substring(1)+" "+entry.targetLanguage+"e");
				StringBuffer buffer=new StringBuffer();
				buffer.append(entry.translations.get(0)).append(", ");
				for(int i=1;i<entry.translations.size();i++) {
					String word=entry.translations.get(i);
					if(buffer.length()+word.length()+2>=maxLineSize) {
						System.out.println(buffer.toString());
						buffer.delete(0, buffer.length());
					}
					buffer.append(entry.translations.get(i)).append(", ");
				}
				System.out.println(buffer.substring(0, buffer.length()-2).toString());
				System.out.println();
			}
			
			return true;
		}
	}
	
	private static class Entry{
		private String originLanguage;
		private String targetLanguage;
		private List<String> translations=new ArrayList<String>();
	}
	public List<Entry> getEntries(String wordToTranslate) {
		try {
			Document doc=Jsoup.connect("https://translatica.pl/szukaj/"+wordToTranslate).get();
			
			ArrayList<Entry> Return=new ArrayList<>();
			
			Elements datas = doc.getElementsByClass("tra-api-wyniki");
			for(Element data:datas) {
				Entry entry=createEntry(data);
				if(entry!=null)Return.add(entry);
			}
			
			return Return;
		} catch (SocketTimeoutException e) {
			connectionTimeoutMessage.printOut();
		} catch (IOException e) {
			connectionFailedMessage.printOut();
		}
		return null;
	}
	private static Entry createEntry(Element data) {
		Entry Return=new Entry();
		Element el=data.select("div.base-entry-hint").first();
		if(el==null)return null;
		
		String language=el.text().substring(22);					//Can give problems when language title change. Now "Translatica, kierunek %language%"
		int divider=language.indexOf('-');
		Return.originLanguage=language.substring(0, divider);
		Return.targetLanguage=language.substring(divider+1);
		
		var trans=data.select("div.tra-api-translation");
		for(Element word:trans) {
			Return.translations.add(word.text().replaceAll(",", "").trim());
		}
		
		return Return;
	}
	
	@Override
	protected String getDataLocation() {
		return null;
	}
}