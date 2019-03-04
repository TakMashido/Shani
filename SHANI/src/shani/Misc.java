package shani;

import java.util.ArrayList;
import java.util.List;

public class Misc {
	public static <T> List<T> createList(T object){
		var Return=new ArrayList<T>();
		Return.add(object);
		return Return;
	}
}