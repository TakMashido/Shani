package takMashido.shani.orders.core;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import takMashido.shani.Engine;
import takMashido.shani.core.Config;
import takMashido.shani.core.Cost;
import takMashido.shani.core.Intend;
import takMashido.shani.core.IntendBase;
import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.Storage;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.orders.Action;
import takMashido.shani.orders.Executable;
import takMashido.shani.orders.IntendParserAction;
import takMashido.shani.orders.IntendParserOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**Order responsible for merging actions into executable chains.
 * It's done almost independently of Action implementations. The requirement is to implement {@link Action#hashString()} function.
 * {@link Action#canMerge()} method can also be used it hashString() is implemented but you still do not want to connect actions.
 */
public class MergeOrder extends IntendParserOrder<ShaniString> {
	private final ShaniString cantConnectMessage;
	private final ShaniString connectSuccessfulMessage;

	private Map<String, List<IntendBase>> merged = new HashMap<>();

	private Action lastExecuted = null;

	public MergeOrder(Element e) throws Storage.ReflectionLoadException {
		super(e);

		cantConnectMessage = ShaniString.loadString(e, "cantConnectMessage");
		connectSuccessfulMessage = ShaniString.loadString(e, "connectSuccessfulMessage");

		try {
			Element data = (Element) Storage.getOrderData(this);
			Element mergedElement = (Element) Storage.getNode(data, "merged");

			NodeList nodes = mergedElement.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				if (nodes.item(i).getNodeType() != Node.ELEMENT_NODE)
					continue;

				Element merge = (Element) nodes.item(i);

				String connectionHash = merge.getAttribute("hash");
				IntendBase intend = Storage.getIntendBase(merge, "intend");

				List<IntendBase> mergedList=merged.get(connectionHash);
				if(mergedList==null){
					mergedList=new ArrayList<IntendBase>();
					merged.put(connectionHash,mergedList);
				}
				mergedList.add(intend);
			}
		} catch (Storage.NodeNotPresentException unused) {}                    //No merges to load, not nee to handle.

		Engine.registerExecuteEndListener(this::executionEndListener);
	}

	private int mergedCalls=0;								//Amount of merged calls in progress. Used to properly set lsatExecute variable.
	private void executionEndListener(Intend intend, Executable executable) {
		String hash = getHashCode(executable.action);

		if (merged.containsKey(hash)){
			mergedCalls+=merged.get(hash).size();
			merged.get(hash).forEach(ShaniCore::interpret);                //No need for nested calling, it's done Engine, it's calling this function after successful execution
		}
		mergedCalls--;
		if(mergedCalls<0){
			mergedCalls=0;
			lastExecuted = executable.action;
		}
	}

	@Override
	public IntendParserAction<ShaniString> getAction() {
		return new MergeAction();
	}

	/**
	 * Register new connection between actions.
	 * It's adding it to merged map, and saving it to describing XML node.
	 *
	 * @param lastHash  Hash of previous Action.
	 * @param toConnect Intend to connect to.
	 */
	private void registerNewConnection(String lastHash, IntendBase toConnect) {
		List<IntendBase> intends=merged.get(lastHash);
		if(intends==null){
			intends=new ArrayList<>();
			merged.put(lastHash, intends);
		}
		intends.add(toConnect);

		Element data = (Element) Storage.getOrderData(this);
		data = (Element) Storage.getNode(data, "merged", true);

		Element merge = Storage.createElement(data, "merge");
		merge.setAttribute("hash", lastHash);

		Storage.writeIntendBase(merge, "intend", toConnect);
	}

	private class MergeAction extends IntendParserAction<ShaniString> {
		private Executable toMerge;
		private Cost costBias;

		@Override
		public void init(String name, Map<String, ? extends ShaniString> parameters) {
			super.init(name, parameters);

			toMerge = Engine.getExecutable(new Intend(parameters.get("unmatched")));

			Cost toMergeCost=toMerge!=null?toMerge.cost:Cost.FREE;

			int words = 0;
			String wordsArr = parameters.get("unmatched").toString().trim();
			for (int i = 0; i < wordsArr.length(); i++) {
				if (Character.isWhitespace(wordsArr.charAt(i)))
					words++;
			}

			costBias=toMergeCost.makeCopy();
			costBias.addImportanceBias((short) (-words * Config.wordReturnImportanceBias));
		}

		@Override
		public boolean execute() {
			if(toMerge==null) {
			    cantConnectMessage.printOut();
				return false;
			}

			Engine.execute(toMerge);
			boolean ret = toMerge.isSuccessful();

			if (ret) {
				if(lastExecuted==null){
					cantConnectMessage.printOut();
				    return false;
				}

			    String connectionHash=getHashCode(lastExecuted);
				if (connectionHash.isEmpty()) {
					cantConnectMessage.printOut();
					return false;
				}
				registerNewConnection(connectionHash, parameters.get("unmatched"));
			}

			return ret;
		}

		@Override
		public Cost getCostBias(){
			return costBias;
		}
	}

	/**Hash code getting shortcut function.
	 * @param action Action of which hash to get.
	 * @return "ActionClasspath:action.hashString()", or "" if action hashString is also "".
	 */
	private static String getHashCode(Action action){
		String hash=action.hashString();
		if(hash.isEmpty())
			return hash;
		return action.getClass().getCanonicalName()+":"+hash;
	}
}