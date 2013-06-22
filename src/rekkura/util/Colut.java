package rekkura.util;

import java.util.*;

import com.google.common.base.Function;
import com.google.common.collect.*;

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
		if (other == null || target == null) return;
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
        if (source == null) return false;
		for (T s : source) if (!targets.contains(s)) return false;
		return true;
	}
	
	public static <T> boolean nonEmpty(Collection<T> s) {
		return s != null && s.size() > 0;
	}
	
	public static <T> boolean empty(Collection<T> s) {
		return !nonEmpty(s);
	}
	
	public static boolean empty(String s) {
		return s == null || s.isEmpty();
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
	
	public static <U, V> V get(Map<U, V> map, U val, V def) {
		if (map == null) return def;
		if (!map.containsKey(val)) return def;
		return map.get(val);
	}
	
	public static <U, V> V get(Map<U, V> map, U val) {
		return get(map, val, null);
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
	
	public static <U> List<U> filterAdjacentRefeq(List<U> elems) {
		List<U> result = Lists.newArrayList();
		
		U last = null;
		for (int i = 0; i < elems.size(); i++) {
			U elem = elems.get(i);
			if (i == 0 || last != elem) {
				last = elem;
				result.add(last);
			}
		}
		
		return result;
	}
	
	public static <U> boolean equivalent(Collection<? extends Collection<U>> first,
		Collection<? extends Collection<U>> second) {
		Set<Set<U>> firstSets = setify(first);
		Set<Set<U>> secondSets = setify(second);
		return Colut.containsSame(firstSets, secondSets);
	}

	public static <U> Set<Set<U>> setify(Collection<? extends Collection<U>> outer) {
		Set<Set<U>> setified = Sets.newHashSet();
		for (Collection<U> inner : outer) setified.add(Sets.newHashSet(inner));
		return setified;
	}

	public static <U> boolean allNulls(List<U> collection) {
		if (empty(collection)) return true;
		for (U u : collection) if (u != null) return false;
		return true;
	}
	
	public static <U> void clear(Collection<U> collection) {
		if (collection != null) collection.clear();
	}
	
	public static <U, V> Set<U> keySet(Map<U, V> map) {
		if (map == null) return null;
		return map.keySet();
	}
	
	public static <U, V> Collection<V> values(Map<U, V> map) {
		if (map == null) return null;
		return map.values();
	}
	
	public static <U> Set<U> union(Iterable<U> first, Iterable<U> second) {
		Set<U> result = Sets.newHashSet(first);
		Iterables.addAll(result, second);
		return result;
	}
	
	public static <U> Iterable<U> prepend(U elem, Iterable<U> rest) {
		List<U> dummy = Lists.newArrayList();
		dummy.add(elem);
		return Iterables.concat(dummy, rest);
	}
	
	public static <U> U set(List<U> list, int pos, U elem) {
		if (list == null || pos < 0 || list.size() <= pos) return null;
		return list.set(pos, elem);
	}
	
	public static <U> List<U> flatten(Collection<? extends Collection<U>> data) {
		List<U> result = Lists.newArrayList();
		if (data == null) return result;
		for (Collection<U> collection : data) Colut.addAll(result, collection);
		return result;
	}
	
	public static <U> int countIn(Iterable<U> source, Collection<U> target) {
		int i = 0; for (U u : source) if (target.contains(u)) i++;
		return i;
	}
	
	public static <U> boolean removeAll(Iterable<U> source, Collection<U> target) {
		boolean result = false;
		for (U u : source) result |= target.remove(u);
		return result;
	}
	
	public static <U> Comparator<U> getAsStringComparator() {
		return new Comparator<U>() {
			@Override public int compare(U o1, U o2) {
				return o1.toString().compareTo(o2.toString());
			}
		};
	}
	
	public static <U> List<U> sortAsStrings(Collection<U> original) {
		List<U> result = Lists.newArrayList(original);
		Collections.sort(result, getAsStringComparator());
		return result;
	}
}
