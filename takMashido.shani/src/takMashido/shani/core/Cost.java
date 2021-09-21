package takMashido.shani.core;

import java.util.Objects;

/**Represents cost of operation invocation, distance between Intend and "intend template"(actually it may not exists with more sophisticated intends matching).
 * It's used to determine what matches better to parsed Intend.
 * The smaller value the better match.
 * You can consider text intend "add two and two". The smallest cost is with CalculateOrder action.
 */
public class Cost implements Comparable<Cost>{
	/**Cost of operation. Direct distance.*/
	private short distance;
	/**Importance bias of operation. The bigger the smaller final cost value.*/
	private short importanceBias;

	public Cost(short distance){
		this(distance, (short)0);
	}
	public Cost(short distance, short importanceBias){
		setDistance(distance);
		setImportanceBias(importanceBias);
	}

	/**Get distance.
	 * @return Distance between compared Intend and it's wanted representation.
	 */
	public short getDistance(){
		return distance;
	}
	/**Get importance bias.
	 * @return Look above.
	 */
	public short getImportanceBias(){
		return importanceBias;
	}

	/**Set distance value.*/
	public void setDistance(short distance){
		this.distance=distance;
	}
	/**Set importance bias.*/
	public void setImportanceBias(short importanceBias){
		this.importanceBias=importanceBias;
	}

	/**Get final value of distance for comparison.
	 * @return float representing this Cost object value.
	 */
	public float value(){
		return distance-importanceBias*Config.importanceBiasMultiplier;
	}

	/**Return if this object not exceeded threshold saying that compared objects has nothing in common: Config.sentenceCompareThreshold.
	 * @return Look above.
	 */
	public boolean isMatched(){
		return distance<Config.sentenceCompareThreshold;
	}

	@Override
	public int compareTo(Cost cost) {
		boolean valid=isMatched();
		boolean valid2=isMatched();

		if(valid&&valid2)
			return mergedCostCheck(cost);
		if(valid)
			return 1;
		if(valid2)
			return -1;
		return mergedCostCheck(cost);
	}
	/**Works like compareTo(Cost), but do not check if bought cost object represents valid distance.
	 * @param cost Cost object to compare this with.
	 * @return 1 if this is bigger, -1 if cost is bigger, 0 if there are equal. Similar to Comparable<T>.compareTo(T)
	 */
	private int mergedCostCheck(Cost cost){
		float val = value();
		float val2 = cost.value();

		if (val > val2)
			return 1;
		else if (val < val2)
			return -1;

		return 0;
	}

	/**Add two cost object together. Inplace.
	 * @param other The cost to add to.
	 * @return this object for easier operations chaining.
	 */
	public Cost add(Cost other){
		distance+=other.distance;
		importanceBias+= other.importanceBias;
		return this;
	}
	/**Subtract two cost object together. Inplace.
	 * @param other The cost to subtract to.
	 * @return this object for easier operations chaining.
	 */
	public Cost subtract(Cost other){
		distance-=other.distance;
		importanceBias-= other.importanceBias;
		return this;
	}
	/**Add two cost object together.
	 * @param first The first cost to add.
	 * @param second Second cost to add.
	 * @return New cost object being sum of arguments.
	 */
	public static Cost add(Cost first, Cost second){
		return new Cost((short)(first.distance+second.distance), (short)(first.importanceBias+second.importanceBias));
	}
	/**Subtract two cost object together.
	 * @param first Cost object to subtract from.
	 * @param second Cost object to subtract.
	 * @return New cost object with value first-second.
	 */
	public static Cost subtract(Cost first, Cost second){
		return new Cost((short)(first.distance-second.distance), (short)(first.importanceBias-second.importanceBias));
	}

	/**Create copy of this object.
	 * @return Copy of this object.
	 */
	public Cost makeCopy(){
		return new Cost(distance, importanceBias);
	}

	@Override
	public boolean equals(Object other){
		if(other instanceof Cost c)
			return distance==c.importanceBias&&importanceBias==c.importanceBias;

		return false;
	}

	@Override
	public int hashCode() {
		return distance*31+importanceBias;
	}

	@Override
	public String toString(){
		return String.format("cost:%d, importanceBias:%d", distance, importanceBias);
	}
}