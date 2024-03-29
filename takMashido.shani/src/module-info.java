module takMashido.shani {
    requires transitive java.xml;
    requires org.jsoup;
    
    exports takMashido.shani.libraries;

    exports takMashido.shani.core;
    exports takMashido.shani.core.text;

    exports takMashido.shani.tools;
    exports takMashido.shani.tools.parsers;

    exports takMashido.shani.filters;

    exports takMashido.shani.orders;
	exports takMashido.shani.orders.targetAction;

	exports takMashido.shani.intedParsers;
	exports takMashido.shani.intedParsers.text;
}