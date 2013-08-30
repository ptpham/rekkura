package rekkura.logic.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import rekkura.util.Colut;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * In general, unifications are best represented as {@code Map<Dob, Dob>}
 * However, this representation will provide must faster merges. 
 * @author ptpham
 *
 */
public class Unification {
	public final ImmutableList<Dob> vars;
	public final Dob[] assigned;
	
	private Unification(Map<Dob, Dob> source, ImmutableList<Dob> ordering) {
		this.vars = ordering;
		assigned = new Dob[ordering.size()];
		for (int i = 0; i < ordering.size(); i++) {
			assigned[i] = source.get(ordering.get(i));
		}
	}
	
	private Unification(ImmutableList<Dob> vars) {
		this(EMPTY_MAP, vars);
	}
	
	private Unification(Dob[] assigned, ImmutableList<Dob> vars) {
		this.vars = vars;
		this.assigned = assigned;
	}
	
	public Unification copy() {
		Dob[] copy = Arrays.copyOf(this.assigned, this.assigned.length);
		Unification result = new Unification(copy, vars);
		return result;
	}
	
	public static Unification from(Map<Dob, Dob> source, ImmutableList<Dob> ordering) {
		return new Unification(source, ordering);
	}
	
	public static Unification from(ImmutableList<Dob> vars) {
		return new Unification(vars);
	}

	/**
	 * This merge may alter the state of this unification even on failure.
	 * @param other
	 * @return
	 */
	public boolean sloppyDirtyMergeWith(Unification other) {
		if (other == null || other.assigned.length != this.assigned.length) return false;
		for (int i = 0; i < this.assigned.length; i++) {
			if (this.assigned[i] == null) this.assigned[i] = other.assigned[i];
			else if (other.assigned[i] != null && this.assigned[i] != other.assigned[i]) return false;
		}
		return true;
	}
	
	public Map<Dob, Dob> toMap() {
		Map<Dob, Dob> result = Maps.newHashMap();
		for (int i = 0; i < vars.size(); i++) {
			if (assigned[i] == null) continue;
			Dob var = vars.get(i);
			result.put(var, assigned[i]);
		}
		return result;
	}

	public boolean isValid() { return Colut.noNulls(this.assigned); }
	public void clear() { Colut.nullOut(this.assigned); }

	@Override public String toString() { return Arrays.toString(assigned); }
	
	public static class Distinct {
		public int posFirst = -1, posSecond = -1;
		public Dob failFirst, failSecond;
		
		public boolean evaluate(Unification unify) {
			Dob first = Colut.get(unify.assigned, posFirst);
			Dob second = Colut.get(unify.assigned, posSecond);
			if (first != null && second != null) return first != second;
			if (first == null && second == null) return failFirst != failSecond;
			if (first != null) return first != failFirst;
			if (second != null) return second != failSecond;
			return true;
		}
	}
	
	public static Distinct convert(Rule.Distinct orig, List<Dob> vars) {
		Distinct result = new Distinct();
		result.posFirst = vars.indexOf(orig.first);
		result.posSecond = vars.indexOf(orig.second);
		if (result.posFirst == -1) result.failSecond = orig.first;
		if (result.posSecond == -1) result.failFirst = orig.second;
		return result;
	}
	
	public boolean evaluateDistinct(Iterable<Distinct> distincts) {
		boolean result = true;
		for (Distinct distinct : distincts) result &= distinct.evaluate(this);
		return result;
	}
	
	public static final ImmutableMap<Dob, Dob> EMPTY_MAP = ImmutableMap.of();
	public static final Unification EMPTY_UNIFICATION = new Unification(ImmutableList.<Dob>of());

}
