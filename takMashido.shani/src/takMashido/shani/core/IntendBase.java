package takMashido.shani.core;

import org.w3c.dom.Element;

/**Interface for classes being intended to use as {@link Intend}
 * Every class implementing this, also has to have constructor taking XML Element as an argument.
 * @Author TakMashido
 */
public interface IntendBase {
    /**Performs deep copy of object.
     * @return Deep copy of this object.*/
    IntendBase copy();

    /**Load IntendBase from xml Element.
     * @return new IntendBase instance loaded from XML element.
     */
    IntendBase loadNew(Element e);
    /**Save this instance of IntendBase to given XML element.
     * @param e XML element which is target of data. It can be empty or be recycled from previous save method usage on same IntendBase object type.
     */
    void save(Element e);
}
