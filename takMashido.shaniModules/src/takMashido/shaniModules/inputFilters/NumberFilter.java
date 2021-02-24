package takMashido.shaniModules.inputFilters;

import java.util.ArrayList;
import java.util.Arrays;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.filters.IntentFilter;

/**Changes word represented numbers to it's digit represetation.
 * "One" becomes 1, "three thousand forty seven" to 3047
 * @author TakMashido
 */
public class NumberFilter extends IntentFilter {
	private ArrayList<NumberElement> elements;
	
	public NumberFilter(Element e) {
		super(e);
		elements=new ArrayList<>();
		
		NodeList nodes=e.getChildNodes();
		int length=nodes.getLength();
		for(int i=0;i<length;i++) {
			Node node=nodes.item(i);
			if(node.getNodeType()!=Node.ELEMENT_NODE)continue;
			elements.add(new NumberElement((Element)node));
		}
		elements.trimToSize();
	}
	
	@Override
	public ShaniString filter(ShaniString orginalRespond) {
		var org=orginalRespond.split(false);
		
		StringBuffer[] returnBuffer=new StringBuffer[org.length];
		
		for(int i=0;i<org.length;i++) {
			returnBuffer[i]=new StringBuffer();
			
			String[] strArray=new String[org[i].length];
			int[] intArray=new int[strArray.length];
			Arrays.fill(intArray, -1);
			
			short[] costs=new short[elements.size()];
			for(int j=0;j<strArray.length;j++) {
				Arrays.fill(costs, Short.MAX_VALUE);
				for(int k=0;k<elements.size();k++) {
					NumberElement el=elements.get(k);
					costs[k]=el.keyword.getCompareCost(org[i][j]);
					if(costs[k]==0) {
						intArray[j]=el.value;
						break;
					}
				}
				if(intArray[j]==-1) {
					short min=costs[0];
					int minIndex=0;
					for(int k=1;k<costs.length;k++)
						if(costs[k]>min) {
							min=costs[k];
							minIndex=k;
						}
					if(min<ShaniCore.getSentenceCompareThreshold()) {
						intArray[j]=elements.get(minIndex).value;
					} else
						strArray[j]=org[i][j].toString();
				}
			}
			
			int length=intArray.length-1;
			for(int j=0;j<length;j++) {
				if(intArray[j]!=-1&&intArray[j+1]!=-1) {
					if(tenPowerCheck(intArray[j+1])) {
						if(intArray[j+1]>intArray[j])
							intArray[j+1]*=intArray[j];
						else
							intArray[j+1]+=intArray[j];
					} else
						intArray[j+1]+=intArray[j];
					intArray[j]=-1;
				}
			}
			
			for(int j=0;j<intArray.length;j++) {
				if(strArray[j]!=null)returnBuffer[i].append(strArray[j]);
				else if(intArray[j]!=-1)returnBuffer[i].append(intArray[j]);
				else continue;
				returnBuffer[i].append(' ');
			}
		}
		
		String[] Return=new String[org.length];
		for(int i=0;i<org.length;i++)Return[i]=returnBuffer[i].toString();
		
		return new ShaniString(false,Return);
	}
	
	private static boolean tenPowerCheck(int number) {
		while(number!=0) {
			if(number==1)return true;
			if(number%10!=0)return false;
			number/=10;
		}
		return true;
	}
	
	private class NumberElement{
		private ShaniString keyword;
		private int value;
		
		private NumberElement(Element e) {
			keyword=new ShaniString(e.getTextContent());
			value=Integer.parseInt(e.getNodeName().substring(1));
		}
	}
	
//	public static void main(String[]args) throws SAXException, IOException, ParserConfigurationException {
//		Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("test2.xml"));
//		var node=doc.getElementsByTagName("module").item(0);
//		
//		var filter=new NumberFilter((Element)node);
//		ShaniString input=new ShaniString("odpal wiedzmina trzecia");
//		ShaniString output=filter.filter(input);
//		
//		long time=System.nanoTime();
//		System.out.println(output.toFullString());
//		System.out.println((System.nanoTime()-time)/1000);
//	}
}