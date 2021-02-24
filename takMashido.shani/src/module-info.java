module takMashido.shani {
    requires transitive java.xml;
    requires org.jsoup;

    exports takMashido.shani.core;
    exports takMashido.shani.core.text;
    exports takMashido.shani.tools;
    exports takMashido.shani.tools.parsers;
    exports takMashido.shani.filters;
    exports takMashido.shani.orders;
}