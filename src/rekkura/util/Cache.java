package rekkura.util;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * Not sure why Guava's caches suck so much.
 * @author "ptpham"
 *
 */
public class Cache<U, V> {

	public final Map<U, V> stored = Maps.newHashMap();
	private final Function<U, V> fn;
	
	protected Cache(Function<U, V> fn) { this.fn = fn; }
	
	public V get(U u) {
		V result = stored.get(u);
		if (result == null) {
			result = fn.apply(u);
			stored.put(u, result);
		}
		return result;
	}
	
	public static <U, V> Cache<U, V> create(Function<U, V> fn) {
		return new Cache<U, V>(fn);
	}
	
	public String toString() {
		return "[" + stored.toString() + " with fn " + fn.toString() + "]";
	}
}
