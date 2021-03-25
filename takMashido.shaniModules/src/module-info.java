module takMashido.shaniModules {
    requires takMashido.shani;
    requires org.jsoup;
    
    opens shani.coreInit;
    
    exports takMashido.shaniModules.orders;
    exports takMashido.shaniModules.inputFilters;
    exports takMashido.shaniModules.intendGetters;
}