package rekkura.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class SetPair<U> {
	public final Set<U> first = Sets.newHashSet();
	public final Set<U> second = Sets.newHashSet();
	
	interface Partitioner<U> extends Function<SetPair<U>, List<SetPair<U>>>
	{ List<SetPair<U>> partition(SetPair<U> pair); }
	
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
}
