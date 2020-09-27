package liblaries;

public class Pair<K,V> {
	public final K first;
	public final V second;
	
	public Pair(K first) {
		this(first,null);
	}
	
	/**
	 * @param first value of {@link #first} variable
	 * @param singleEntry if true assign first into {@link #second}.
	 * @throws ClassCastException if K and V types do not match.
	 */
	public Pair(K first, boolean singleEntry) {
		this.first=first;
		if(singleEntry)
			this.second=(V) first;
		else
			this.second=null;
	}
	
	public Pair(K first, V second) {
		this.first=first;
		this.second=second;
	}
}