package shani.modules.templates;

import org.w3c.dom.Element;

public abstract class ShaniModule {
	protected final Element moduleFile;
	public ShaniModule(Element e) {
		moduleFile=e;
	}
}