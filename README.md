# SHANI
Simple Heurestic Action Names Interpreter.  
This is simple pseudo intelligent assistant. For queries parsing it's using Damerau–Levenshtein algorith to match string on words level and RegEx like metalanguage on sentence Level.

It have module based structure so it's quite simple to add new features. All Orders and Input Filters are loaded dynamically based on data from initization xml file. It also defines all commands templates and respone strings so it's possible to make it use just by proving file valid for given language(For now only polish language is done).

Curretly there is no dedicated user interface for it. Run it with cmd(java -jar Shani.jar) or provided .bat file.

Currenttly supported actions:  
Launch program, open file, execute batch script, or local url based actions(eg. steam://rungameid/335300)),  
Stopwatch,  
Dictionary(Polish, English German. Taken from translatica.pl webside),  
Calculator,  
Weather forecast printer(taken from Weather.com website),  
execution of cmd commands inside shani consoles.  
