package rekkura.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class NestedIterator<U, V> implements Iterator<V> {
	protected Iterator<U> inner;
	protected Iterator<V> outer;
	protected abstract Iterator<V> prepareNext(U u);
	
	public NestedIterator(Iterator<U> inner) { this.inner = inner; }
	private boolean outerHasNext() { return (outer != null && outer.hasNext()); }
	
	@Override
	public boolean hasNext() {
		if (!outerHasNext()) {
			if (!inner.hasNext()) return false;
			outer = prepareNext(inner.next());
		}
		return outerHasNext();
	}

	@Override public V next() {
		if (!hasNext()) throw new NoSuchElementException();
		return outer.next();
	}

	@Override public void remove() { }
}