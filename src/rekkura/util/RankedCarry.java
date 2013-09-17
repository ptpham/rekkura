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
	public U ranker;
	public V carry;
	private Comparator<U> comp;
	
	private RankedCarry(U ranker, V carry) {
		this(ranker, carry, Ordering.<U>natural());
	}
	
	private RankedCarry(U ranker, V carry, Comparator<U> comp) {
		this.ranker = ranker;
		this.carry = carry;
		this.comp = comp;
	}
	
	public boolean consider(U newRanker, V newCarry) {
		if (comp.compare(ranker, newRanker) > 0) {
			this.ranker = newRanker;
			this.carry = newCarry;
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
