package liblaries;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**Simple class for easier navigation in xml document without repetition in nodes names.*/
public class DOMWalker {
	/**Walks from given node.
	 * @param e root node from which start search.
	 * @param path Names of nodes to search for divided with "/" like in normal file system path, ".." and "." entries are also valid, Unix '*' wildcard is not processed.
	 * @return node found under given path.
	 * @throws DOMWalkExcetion When can't find node matching to one of the entries in path.
	 */
	public static Element walk(Node e, String path) {
		String[] dirs=path.split("[/\\\\]+");
		
		Element ret=null;
		Document doc=null;
		if(e instanceof Element)
			ret=(Element)e;
		else
			doc=(Document)e;
		
		for(String dir:dirs) {
			if(dir.equals(".."))
				ret=(Element)ret.getParentNode();
			if(dir.equals(".."))
				continue;
			
			NodeList nodes=null;
			if(ret!=null)
				nodes=ret.getElementsByTagName(dir);
			else
				nodes=doc.getElementsByTagName(dir);
			
			if(nodes.getLength()==0)
				throw new DOMWalkExcetion(path,dir);
			ret=(Element)nodes.item(0);
		}
		
		return ret;
	}
	
	public static class DOMWalkExcetion extends RuntimeException{
		private static final long serialVersionUID = -3315611951478845674L;
		
		public final String fullPath;
		public final String missingDirectory;
		
		protected DOMWalkExcetion(String fullPath, String missingDirectory) {
			super("Can't find \""+missingDirectory+"\" directory inside path \""+fullPath+"\".");
			
			this.fullPath=fullPath;
			this.missingDirectory=missingDirectory;
		}
	}
}
