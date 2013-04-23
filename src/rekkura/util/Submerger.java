package rekkura.util;

import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class Submerger<U> {

	private Cache<String, U> cache = new Cache<String, U>(new Function<String, U>() {
		@Override public U apply(String s) {
			
			return fromString(s);
		}
	});
	
	private Set<U> known = Sets.newHashSet();

	public abstract U fromString(String s);
	public abstract String toString(U u);
	public abstract U process(U u);
	
	public U submerge(U original) {
		if (known.contains(original)) return original;

		String stringed = toString(original);
		U existing = cache.stored.get(stringed);
		if (existing == null) {
			existing = process(original);
			cache.stored.put(stringed, existing);
			this.known.add(existing);
		}
		
		return existing;
	}
	
	public List<U> submerge(Iterable<U> originals) {
		List<U> result = Lists.newArrayList();
		for (U original : originals) result.add(submerge(original));
		return result;
	}
}
