package rekkura.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class creates an iterator over elements that are generated
 * from those of another iterator.
 * @author ptpham
 *
 * @param <U> elements of the original iterator
 * @param <V> elements of the new iterator
 */
public abstract class NestedIterator<U, V> implements Iterator<V> {
	protected Iterator<U> inner;
	protected Iterator<V> outer;
	protected abstract Iterator<V> prepareNext(U u);
	
	public NestedIterator(Iterator<U> inner) { this.inner = inner; }
	private boolean outerHasNext() { return (outer != null && outer.hasNext()); }
	
	@Override
	public boolean hasNext() {
		while (!outerHasNext()) {
			if (!inner.hasNext()) return false;
			outer = prepareNext(inner.next());
		}
		return outerHasNext();
	}

	@Override public V next() {
		if (!hasNext()) throw new NoSuchElementException();
		return outer.next();
	}

	@Override public void remove() {
		if (this.inner != null) this.inner.remove();
		else throw new IllegalStateException();
	}
}