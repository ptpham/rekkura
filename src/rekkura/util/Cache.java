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
	private final Function<V, Boolean> checker;
	
	protected Cache(Function<U, V> fn) { this(fn, null); }
	protected Cache(Function<U, V> fn, Function<V, Boolean> checker) {
		this.checker = checker;
		this.fn = fn;
	}
	
	public V get(U u) {
		V result = validate(stored.get(u));
		if (result == null) {
			result = fn.apply(u);
			stored.put(u, result);
		}
		return result;
	}

	public V propose(U u, V v) {
		V result = validate(stored.get(u));
		if (result == null) {
			stored.put(u, v);
			return v;
		}
		return result;
	}
	
	private V validate(V v) {
		if (v == null || checker == null || checker.apply(v)) return v;
		return null;
	}
	
	public static <U, V> Cache<U, V> create(Function<U, V> fn) {
		return new Cache<U, V>(fn);
	}
	
	public static <U, V> Cache<U, V> create(Function<U, V> fn,
		Function<V, Boolean> checker) {
		return new Cache<U, V>(fn, checker);
	}
	
	public String toString() {
		return "[" + stored.toString() + " with fn " + fn.toString() + "]";
	}
}
