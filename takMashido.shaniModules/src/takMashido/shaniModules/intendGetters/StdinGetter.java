package takMashido.shaniModules.intendGetters;

import org.w3c.dom.Element;
import takMashido.shani.core.IntendGetter;
import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.text.ShaniString;

import java.util.Scanner;

public class StdinGetter extends IntendGetter {
    public StdinGetter(Element e) {
        super(e);
    }

    @Override
    public void run() {
        Scanner in=new Scanner(System.in);

        while(true){
            String str=in.nextLine().trim().toLowerCase();
            if(str.isEmpty()) continue;

            ShaniCore.interpret(new ShaniString(str,false));
        }
    }
}
