package rekkura.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class Submerger<U> {
	Map<String, U> cache = Maps.newHashMap();
	private Set<U> known = Sets.newHashSet();

	public abstract U fromString(String s);
	public abstract String toString(U u);
	public abstract U process(U u);
	
	public void clear() {
		this.cache.clear();
		this.known.clear();
	}
	
	public U submerge(U original) {
		if (known.contains(original)) return original;
		String stringed = toString(original);
		return submerge(original, stringed);
	}
	
	public U submerge(String stringed) { return submerge(null, stringed); }
	private U submerge(U original, String stringed) {
		U existing = cache.get(stringed);
		if (existing == null) {
			if (original == null) original = fromString(stringed);
			existing = process(original);
			
			// This block deals with the possibility that process will
			// changed the stringed representation of the object
			stringed = toString(existing);
			U reattempt = cache.get(stringed);
			if (reattempt != null) return reattempt;
			
			cache.put(stringed, existing);
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
