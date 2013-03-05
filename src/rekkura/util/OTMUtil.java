package rekkura.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.base.Function;
import com.google.common.collect.*;

/**
 * (One-to-Many Utilities)
 * @author ptpham
 *
 */
public class OTMUtil {
	
	public static <U, V> Iterator<V> valueIterator(final Multimap<U, V> map, Iterator<U> keys) {
		if (map == null) return Iterators.emptyIterator();
		return new NestedIterator<U, V>(keys) {
			@Override protected Iterator<V> prepareNext(U u) {
				return map.get(u).iterator();
			}
		};
	}
	
	public static <U, V> Iterator<V> valueIterator(Multimap<U, V> map) {
		return valueIterator(map, map.keySet().iterator());
	}
	
	public static <U, V> Iterable<V> valueIterable(final Multimap<U, V> map, final Iterable<U> keys) {
		return new Iterable<V>() {
			@Override public Iterator<V> iterator() {
				return valueIterator(map, keys.iterator());
			}
		};
	}
	
	public static <U, V> Iterable<V> valueIterable(Multimap<U, V> map) {
		return valueIterable(map, map.keySet());
	}

	public static <U> Set<U> flood(Map<U, Set<U>> deps, U root) {
		Set<U> result = Sets.newHashSet();
		Queue<U> remaining = Queues.newLinkedBlockingQueue();
		remaining.add(root);
		
		while (!remaining.isEmpty()) {
			U next = remaining.poll();
			result.add(next);
			remaining.addAll(deps.get(next));
		}
		
		return result;
	}
	
	public static <T, U, V> Multimap<U, T> expandRight(Multimap<U, V> map, Function<V, Collection<T>> fn) {
		Multimap<U, T> result = HashMultimap.create();
		
		for (U u : map.keySet()) {
			Set<T> values = Sets.newHashSet();
			for (V v : map.get(u)) {
				Collection<T> expansion = fn.apply(v);
				if (expansion == null) continue;
				values.addAll(expansion); 
			}
			if (Colut.empty(values)) continue;
			result.putAll(u, values);
		}
		
		return result;
	}
	

	public static <T, U, V> Multimap<T, V> expandLeft(Multimap<U, V> map, Function<U, Collection<T>> fn) {
		if (map == null) return null;
		Multimap<T, V> result = HashMultimap.create();
		
		for (U u : map.keySet()) {
			Collection<V> values = map.get(u);
			if (Colut.empty(values)) continue;
			Collection<T> col = fn.apply(u);
			if (col == null) continue;
			for (T t : col) { result.putAll(t, values); }
		}
		
		return result;
	}
	
	public static <T, U, V> Multimap<U, T> joinRight(Multimap<U, V> map, final Multimap<V, T> other) {
		if (map == null) return null;
		Function<V, Collection<T>> joiner = new Function<V, Collection<T>>() {
			@Override public Collection<T> apply(V v) { return other.get(v); }
		};
		
		return expandRight(map, joiner);
	}
	
	public static <T, U, V> Multimap<T, V> joinLeft(Multimap<U, V> map, final Multimap<U, T> other) {
		if (map == null) return null;
		
		Function<U, Collection<T>> joiner = new Function<U, Collection<T>>() {
			@Override public Collection<T> apply(U u) { return other.get(u); }
		};
		
		return expandLeft(map, joiner);
	}
}
