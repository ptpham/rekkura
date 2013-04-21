package rekkura.util;

import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * A DualMultimap represents a graph over objects U, V
 * such that there can be edges between objects of different
 * types but not between objects of the same type.
 * @author ptpham
 *
 * @param <U>
 * @param <V>
 */
public class DualMultimap<U, V> {

	public final Multimap<U, V> forward = HashMultimap.create();
	public final Multimap<V, U> backward = HashMultimap.create();

	public void removeForward(U u) {
		forward.removeAll(u);
		removeValueFrom(u, backward.entries().iterator());
	}

	public void removeBackward(V v) {
		backward.removeAll(v);
		removeValueFrom(v, forward.entries().iterator());
	}
	
	private static <U, V> void removeValueFrom(U u, Iterator<Map.Entry<V, U>> iterator) {
		while (iterator.hasNext()) {
			U value = iterator.next().getValue();
			if (u.equals(value)) iterator.remove();
		}
	}

	private DualMultimap() { }

	public static <U, V> DualMultimap<U, V> create() {
		return new DualMultimap<U, V>();
	}
}
