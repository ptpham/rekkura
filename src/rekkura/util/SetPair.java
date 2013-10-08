package rekkura.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.*;

public class SetPair<U> implements Iterable<U> {
	public final Set<U> first = Sets.newHashSet();
	public final Set<U> second = Sets.newHashSet();
	
	public interface Partitioner<U> extends Function<SetPair<U>, List<SetPair<U>>> { }
	
	public SetPair() { }
	public SetPair(Iterable<U> first, Iterable<U> second) {
		Iterables.addAll(this.first, first);
		Iterables.addAll(this.second, second);
	}

	@Override
	public Iterator<U> iterator() {
		return Iterators.concat(first.iterator(), second.iterator());
	}
	
	@Override
	public String toString() {
		return "<" + first.toString() + ", " + second.toString() + ">";
	}
	
	@Override
	public int hashCode() {
		Object[] temp = { first, second };
		return Arrays.hashCode(temp);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (!other.getClass().equals(this.getClass())) return false;
		SetPair<?> cast = (SetPair<?>)other;
		return cast.first.equals(this.first) && cast.second.equals(this.second);
	}
	
	public static <U> List<SetPair<U>> apply(SetPair<U> pair, Partitioner<U> partitioner) {
		List<SetPair<U>> temp = Lists.newArrayList(); temp.add(pair);
		return apply(temp, ImmutableList.of(partitioner));
	}
	
	public static <U> List<SetPair<U>> apply(Iterable<SetPair<U>> pairs,
		Partitioner<U> partitioner) {
		return apply(pairs, ImmutableList.of(partitioner));
	}
	
	public static <U> List<SetPair<U>> apply(Iterable<SetPair<U>> pairs,
		Iterable<Partitioner<U>> partitioners) {
		List<SetPair<U>> result = Lists.newArrayList(pairs);
		for (Partitioner<U> partitioner : partitioners) {
			List<SetPair<U>> replacement = Lists.newArrayList();
			for (SetPair<U> pair : result) replacement.addAll(partitioner.apply(pair));
			result = replacement;
		}
		return result;
	}

	public static <U> Map<U,U> extractKnown(Iterable<SetPair<U>> pairs) {
		Map<U,U> result = Maps.newHashMap();
		for (SetPair<U> pair : pairs) {
			if (pair.first.size() != 1) continue;
			if (pair.second.size() != 1) continue;
			result.put(Colut.any(pair.first), Colut.any(pair.second));
		}
		
		return result;
	}
	
	public static <U> SetPair<U> extractFailed(Iterable<SetPair<U>> pairs) {
		SetPair<U> result = new SetPair<U>();
		for (SetPair<U> pair : pairs) {
			if (pair.first.size() == 0 || pair.second.size() == 0) {
				result.first.addAll(pair.first);
				result.second.addAll(pair.second);
			}
		}
		return result;
	}
	
	public static <U, V> List<SetPair<V>> joinBase(Multimap<U,V> first, Multimap<U,V> second) {
		List<SetPair<V>> result = Lists.newArrayList();
		for (U key : first.keySet()) {
			if (!second.containsKey(key)) continue;
			result.add(new SetPair<V>(first.get(key), second.get(key)));
		}
		return result;
	}
	
	public static <U,V> List<SetPair<V>> convertDown(Iterable<SetPair<U>> rules, Function<U, ? extends Iterable<V>> fn) {
		List<SetPair<V>> result = Lists.newArrayList();
		for (SetPair<U> rule : rules) {
			SetPair<V> child = new SetPair<>();
			for (U first : rule.first) Iterables.addAll(child.first, fn.apply(first));
			for (U second : rule.second) Iterables.addAll(child.second, fn.apply(second));
			result.add(child);
		}
		return result;
	}
}
