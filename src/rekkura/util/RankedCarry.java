package rekkura.util;

import java.util.Comparator;

import com.google.common.collect.Ordering;

/**
 * This holds some of that gross logic that happens when 
 * you try to take the max over f(x) and you want "carry"
 * the value of x with you.
 * @author ptpham
 *
 * @param <U>
 * @param <V>
 */
public class RankedCarry<U extends Comparable<U>, V> {
	private U u;
	private V v;
	private Comparator<U> comp;
	
	private RankedCarry(U u, V v) {
		this(u, v, Ordering.<U>natural());
	}
	
	private RankedCarry(U u, V v, Comparator<U> comp) {
		this.u = u;
		this.v = v;
		this.comp = comp;
	}
	
	public U getRanker() { return u; }
	public V getCarry() { return v; }
	
	public boolean consider(U ranker, V carry) {
		if (comp.compare(u, ranker) > 0) {
			this.u = ranker;
			this.v = carry;
			return true;
		}
		return false;
	}
	
	public static <U extends Comparable<U>, V> RankedCarry<U, V> createNatural(U u, V v) {
		return new RankedCarry<U, V>(u, v);
	}
	
	public static <U extends Comparable<U>, V> RankedCarry<U, V> createReverseNatural(U u, V v) {
		return new RankedCarry<U, V>(u, v, Ordering.<U>natural().<U>reverse().<U>nullsLast());
	}
}
