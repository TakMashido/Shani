package shani.modules;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import shani.Config;
import shani.ShaniString;
import shani.modules.templates.FilterModule;

public class WordTrashFilter extends FilterModule {
	private ShaniString words;
	
	public WordTrashFilter(Element e) {
		super(e);
		
		words=new ShaniString();
		var childs=e.getChildNodes();
		for(int i=0;i<childs.getLength();i++) {
			var node=childs.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				words.add(node.getTextContent());
			}
		}
	}

	@Override
	public ShaniString filter(ShaniString orginalRespond) {
		ShaniString[] wor=orginalRespond.split(false)[0];
		
		boolean changed=false;							//In most cases no word will match, just return original ShaniString then.
		StringBuffer ret=null;
		for(int i=0;i<wor.length;i++) {
			ShaniString word=wor[i];
			
			if(word.getCompareCost(words)<Config.wordCompareTreshold) {
				if(!changed) {
					changed=true;
					ret=new StringBuffer();
					
					for(int j=0;j<i;j++)
						ret.append(' ').append(wor[j].toString());						
				}
			}else if(changed) {
				ret.append(' ').append(word.toString());
			}
		}
		
		return changed?new ShaniString(ret.toString(),false):orginalRespond;
	}
	
/*	public static void main(String[]args) throws SAXException, IOException, ParserConfigurationException {
		Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("test.xml"));
		var node=doc.getElementsByTagName("module").item(0);
		
		var filter=new WordTrashFilter((Element)node);
		ShaniString input=new ShaniString("odpal wiedzmina trzeciego prosz�");
		
		ShaniString output=filter.filter(input);
		System.out.println(output.toFullString());
		
		long time=System.nanoTime();										//Initial run involves string stemming so not fully representative
		filter.filter(input);
		System.out.println((System.nanoTime()-time)/1000/1000f+"ms");
	}*/
}
