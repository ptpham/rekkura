package rekkura.util;

import java.util.Iterator;

public abstract class NestedIterable<U, V> implements Iterable<V> {
	protected Iterable<U> inner;
	protected abstract Iterator<V> prepareNext(U u);
	
	public NestedIterable(Iterable<U> inner) { this.inner = inner; }
	
	@Override public Iterator<V> iterator() {
		return new NestedIterator<U, V>(inner.iterator()) {
			@Override protected Iterator<V> prepareNext(U u) {
				return NestedIterable.this.prepareNext(u);
			}
		};
	}
}