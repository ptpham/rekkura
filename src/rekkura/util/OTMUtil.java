package rekkura.util;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * (One-to-many utilities)
 * @author ptpham
 *
 */
public class OTMUtil {
	
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
	
	public static <U, V> void merge(Map<U, Set<V>> dst, Map<U, Set<V>> src) {
		for (U u : src.keySet()) {
			Set<V> dstSet = dst.get(u);
			Set<V> srcSet = src.get(u);
			
			if (srcSet == null) continue;
			if (dstSet == null) { dstSet = srcSet; } 
			else { dstSet.addAll(srcSet); }
		}
	}
}
