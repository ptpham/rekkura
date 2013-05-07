package rekkura.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

/**
 * (Collection Utilities) These methods are supposed to be
 * null tolerant and are supposed to make your code much, much
 * more succinct. =P
 * @author ptpham
 *
 */
public class Colut {
	
	public static <T> void addAt(List<T> target, int i, T t) {
		if (target == null) return;
		while (target.size() <= i) target.add(null);
		target.set(i, t);
	}
	
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
	
	public static <T> boolean containsAll(Iterable<T> source, Collection<T> targets) {
		for (T s : source) if (!targets.contains(s)) return false;
		return true;
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
		if (s == null) return null;
		Iterator<T> iterator = s.iterator();
		if (!iterator.hasNext()) return null;
		T result = iterator.next();
		iterator.remove();
		return result;
	}
	
	public static <T> boolean remove(Collection<T> s, T t){
		if (s == null) return false;
		return s.remove(t);
	}
	
	public static <T> T removeEnd(List<T> list) {
		if (list == null || list.size() == 0) return null;
		return list.remove(list.size() - 1);
	}
	
	public static <T> T end(List<T> list) {
		if (nonEmpty(list)) return list.get(list.size() - 1);
		return null;
	}
	
	public static <T> T fromEnd(List<T> list, int i) {
		if (list == null) return null;
		return get(list, list.size() - i - 1);
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
	
	public static <U> List<U> collapseAsList(Collection<? extends Collection<U>> all) {
		List<U> result = Lists.newArrayList();
		for (Collection<U> collection : all) { result.addAll(collection); }
		return result;
	}

	public static <U> U get(List<U> list, int i) {
		if (list == null || i < 0 || i >= list.size()) return null;
		return list.get(i);
	}
	
	public static <U> Set<U> intersection(Collection<U> first, Collection<U> second) {
		Set<U> result = Sets.newHashSet(first);
		result.retainAll(second);
		return result;
	}
	
	public static <U> Set<U> difference(Collection<U> first, Collection<U> second) {
		Set<U> result = Sets.newHashSet(first);
		result.removeAll(second);
		return result;
	}

	public static <U> Iterator<U> firstK(final Iterator<U> raw, final int k) {
		return new Iterator<U>() {
			int current = 0;
			@Override public boolean hasNext() { return current++ < k && raw.hasNext(); }
			@Override public U next() { return raw.next(); }
			@Override public void remove() { raw.remove(); }
		};
	}
	
	public static <U> Iterable<U> firstK(final Iterable<U> raw, final int k) {
		return new Iterable<U>() {
			@Override public Iterator<U> iterator() { return firstK(raw.iterator(), k); }
		};
	}

	public static <U> List<U> slice(List<U> list, int begin, int end) {
		List<U> result = Lists.newArrayList();
		for (int i = begin; i < end && i < list.size(); i++) {
			result.add(list.get(i));
		}
		return result;
	}
	
	public static int parseInt(String s) {
		int result = 0;
		try { result = Integer.parseInt(s); } catch (Exception e) { }
		return result;
	}
	
	public static <U> U randomSelection(List<U> vals, Random rand) {
		int index = rand.nextInt(vals.size());
		return vals.get(index);
	}
	
	public static <U> List<U> newArrayListOfNulls(int num) {
		List<U> result = Lists.newArrayList();
		for (int i = 0; i < num; i++) { result.add(null); }
		return result;
	}
	
	public static <U> boolean noNulls(U[] arr) {
		for (U u : arr) if (u == null) return false;
		return true;
	}
	
	public static <U> void nullOut(U[] arr) {
		for (int i = 0; i < arr.length; i++) arr[i] = null;
	}

	public static <U> U first(List<U> list) {
		return Colut.get(list, 0);
	}

	public static <U> List<U> filterNulls(List<U> list) {
		List<U> result = Lists.newArrayListWithCapacity(list.size());
		for (U u : list) if (u != null) result.add(u);
		return result;
	}

	public static <U> List<U> filterAt(List<U> list, int position) {
		List<U> result = Lists.newArrayListWithCapacity(list.size());
		for (int i = 0; i < list.size(); i++) if (i != position) result.add(list.get(i));
		return result;
	}

	public static <U> List<U> resize(List<U> list, int size) {
		return resizeWith(list, size, null);
	}
	
	public static <U> List<U> resizeWith(List<U> list, int size, U elem) {
		if (list == null) list = Lists.newArrayList();
		while (list.size() > size) removeEnd(list);
		while (list.size() < size) list.add(elem);
		return list;
	}
	
	public static <U> boolean containsSame(Collection<U> first, Collection<U> second) {
		if (first.size() != second.size()) return false;
		for (U u : first) if (!second.contains(u)) return false;
		for (U u : second) if (!first.contains(u)) return false;
		return true;
	}
}
