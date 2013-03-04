package rekkura.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

/**
 * (One-to-Many Utilities)
 * @author ptpham
 *
 */
public class OTMUtil {
	
	public static <U, V> boolean contains(Map<U, Set<V>> map, U key, V val) {
		Set<V> set = map.get(key);
		if (set == null) return false;
		return set.contains(val);
	}
	
	public static <U, V> void put(Map<U, Set<V>> map, U key, V val) {
		Set<V> set = map.get(key);
		if (set == null) {
			set = Sets.newHashSet();
			map.put(key, set);
		}
		set.add(val);
	}
	
	public static <U, V> Set<V> get(Map<U, Set<V>> deps, U next) {
		if (deps == null) return Sets.newHashSet();
		Set<V> result = deps.get(next);
		if (result == null) return Sets.newHashSet();
		return result; 
	}
	
	public static <U, V> void remove(Map<U, Set<V>> map, U key, V val) {
		Set<V> set = map.get(key);
		if (set == null) return;
		set.remove(val);
	}

	public static <U, V> void merge(Map<U, Set<V>> dst, Map<U, Set<V>> src) {
		for (U u : src.keySet()) {
			Set<V> dstSet = dst.get(u);
			Set<V> srcSet = src.get(u);
			
			if (srcSet == null) continue;
			if (dstSet == null) { dstSet = srcSet; } 
			else { dstSet.addAll(srcSet); }
		}
	}

	public static <U, V> Map<V, Set<U>> invert(Map<U, Set<V>> map) {
		if (map == null) return null;
		
		Map<V, Set<U>> result = Maps.newHashMap();
		for (U u : map.keySet()) {
			for (V v : get(map, u)) {
				put(result, v, u);
			}
		}
		return result;
	}
	
	public static <U, V> Iterator<V> valueIterator(final Map<U, Set<V>> map, Iterator<U> keys) {
		if (map == null) return Iterators.emptyIterator();
		return new NestedIterator<U, V>(keys) {
			@Override protected Iterator<V> prepareNext(U u) {
				return map.get(u).iterator();
			}
		};
	}
	
	public static <U, V> Iterator<V> valueIterator(final Map<U, Set<V>> map) {
		return valueIterator(map, map.keySet().iterator());
	}
	
	public static <U, V> Iterable<V> valueIterable(final Map<U, Set<V>> map, final Iterable<U> keys) {
		return new Iterable<V>() {
			@Override public Iterator<V> iterator() {
				return valueIterator(map, keys.iterator());
			}
		};
	}
	
	public static <U, V> Iterable<V> valueIterable(final Map<U, Set<V>> map) {
		return valueIterable(map, map.keySet());
	}

	public static <U> Set<U> flood(Map<U, Set<U>> deps, U root) {
		Set<U> result = Sets.newHashSet();
		Queue<U> remaining = Queues.newLinkedBlockingQueue();
		remaining.add(root);
		
		while (!remaining.isEmpty()) {
			U next = remaining.poll();
			result.add(next);
			remaining.addAll(OTMUtil.get(deps, next));
		}
		
		return result;
	}
}
