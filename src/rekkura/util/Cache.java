package rekkura.util;

import java.util.Map;

import com.google.common.base.Function;

/**
 * Not sure why Guava's caches suck so much.
 * @author "ptpham"
 *
 */
public class Cache<U, V> {

	public final Map<U, V> stored = Synchron.newHashmap();
	private Function<V, Boolean> checker;
	private Function<U, V> fn;
	
	private Cache() { this(null, null); }
	protected Cache(Function<U, V> fn) { this(fn, null); }
	protected Cache(Function<U, V> fn, Function<V, Boolean> checker) {
		this.checker = checker;
		this.fn = fn;
	}
	
	public synchronized V get(U u) {
		V result = validate(stored.get(u));
		if (result == null) {
			result = fn.apply(u);
			if (result == null) return null;
			stored.put(u, result);
		}
		return result;
	}

	public synchronized V propose(U u, V v) {
		V result = validate(stored.get(u));
		if (result == null && v != null) {
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
	
	public static <U> Cache<U,Integer> createCounter() {
		Cache<U,Integer> result = new Cache<U,Integer>();
		result.fn = new Function<U,Integer>() {
			int next = 0;
			@Override public Integer apply(U u)
			{ return next++; }
		};
		return result;
	}
	
	public String toString() {
		return "[" + stored.toString() + " with fn " + fn.toString() + "]";
	}
}
