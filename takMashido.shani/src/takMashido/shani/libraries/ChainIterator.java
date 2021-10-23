package takMashido.shani.libraries;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

public class ChainIterator<T> implements Iterator<T> {
	private Queue<Iterator<T>> iterators=new LinkedList<>();
	
	public ChainIterator(){}
	@SafeVarargs
	public ChainIterator(Iterator<T>... iterator){
		addIterator(iterator);
	}
	
	@SafeVarargs
	public final void addIterator(Iterator<T>... iterator){
		Collections.addAll(iterators, iterator);
	}
	
	@Override
	public boolean hasNext() {
		if(iterators.size()==0)
			return false;
		return iterators.peek().hasNext();
	}
	
	@Override
	public T next() {
		if(iterators.size()==0)
			throw new NoSuchElementException();
		
		T ret=iterators.peek().next();
		
		while(!iterators.peek().hasNext()) {
			iterators.poll();
			
			if(iterators.size()==0)
				return ret;
		}
		
		return ret;
	}
}
