package rekkura.util;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

public class MapUtils {
	
	public static <U, V> void safePut(Map<U, Set<V>> map, U key, V val) {
		Set<V> set = map.get(key);
		if (set == null) {
			set = Sets.newHashSet();
			map.put(key, set);
		}
		set.add(val);
	}
	
	public static <U, V> void safeRemove(Map<U, Set<V>> map, U key, V val) {
		Set<V> set = map.get(key);
		if (set == null) return;
		set.remove(val);
	}
}
