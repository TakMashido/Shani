package takMashido.shani.intedParsers;

import takMashido.shani.orders.Action;

/**
 * Interface marking that object is able to provide Action, or it's subtype
 * @param <T> Kind of Action subtype this interface is retuning.
 */
public interface ActionGetter<T extends Action>{
	/**Get empty not initialized Action.
	 * @return New Action object instance.
	 */
	T getAction();
}
