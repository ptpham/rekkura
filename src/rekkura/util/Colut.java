package rekkura.util;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

/**
 * (Collection Utilities)
 * @author ptpham
 *
 */
public class Colut {
	
	public static <T> void addAll(Collection<T> target, Collection<T> other) {
		if (other == null) return;
		target.addAll(other);
	}
	
	public static <T> boolean contains(Collection<T> s, T t) {
		return (s != null && s.contains(t));
	}
	
	public static <T> boolean containsAny(Iterable<T> source, Collection<T> targets) {
		if (source == null) return false;
		for (T s : source) if (contains(targets, s)) return true;
		return false;
	}
	
	public static <T> boolean containsNone(Iterable<T> source, Collection<T> targets) {
		return !containsAny(source, targets);
	}
	
	public static <T> boolean nonEmpty(Collection<T> s) {
		return s != null && s.size() > 0;
	}
	
	public static <T> boolean empty(Collection<T> s) {
		return !nonEmpty(s);
	}
	
	public static <T> T any(Collection<T> s) {
		if (empty(s)) return null;
		return s.iterator().next();
	}
	
	public static <T> T popAny(Collection<T> s) {
		T result = any(s);
		remove(s, result);
		return result;
	}
	
	public static <T> boolean remove(Collection<T> s, T t){
		if (s == null) return false;
		return s.remove(t);
	}
	
	public static <T> T end(List<T> list) {
		if (nonEmpty(list)) return list.get(list.size() - 1);
		return null;
	}
	
	public static Character end(String s) {
		if (s == null || s.length() == 0) return null;
		return s.charAt(s.length() - 1);
	}
	
	public static <U> void shiftAll(Multiset<U> counter, Iterable<U> keys, int shift) {
		for (U u : keys) {
			int newValue = counter.count(u) + shift;
			counter.setCount(u, newValue);
		}
	}
	
	public static <U, V> Multimap<U, V> indexBy(Collection<V> collection, Function<V, U> fn) {
		Multimap<U, V> result = HashMultimap.create();
		for (V v : collection) result.put(fn.apply(v), v);
		return result;
	}
}
