package rekkura.logic.model;

import java.util.Arrays;
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
	
	private Dob[] bay;
	
	private Unification(Map<Dob, Dob> source, ImmutableList<Dob> ordering) {
		this.vars = ordering;
		bay = new Dob[ordering.size()];
		for (int i = 0; i < ordering.size(); i++) {
			bay[i] = source.get(ordering.get(i));
		}
	}
	
	private Unification(ImmutableList<Dob> vars) {
		this(EMPTY_MAP, vars);
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
		if (other == null || other.bay.length != this.bay.length) return false;
		for (int i = 0; i < this.bay.length; i++) {
			if (this.bay[i] == null) this.bay[i] = other.bay[i];
			else if (other.bay[i] != null && this.bay[i] != other.bay[i]) return false;
		}
		return true;
	}
	
	public Map<Dob, Dob> toMap() {
		Map<Dob, Dob> result = Maps.newHashMap();
		for (int i = 0; i < vars.size(); i++) {
			if (bay[i] == null) continue;
			Dob var = vars.get(i);
			result.put(var, bay[i]);
		}
		return result;
	}
	
	public boolean isValid() { return Colut.noNulls(this.bay); }
	public void clear() { Colut.nullOut(this.bay); }

	@Override public String toString() { return Arrays.toString(bay); }
	
	public static final ImmutableMap<Dob, Dob> EMPTY_MAP = ImmutableMap.of();
	public static final Unification EMPTY_UNIFICATION = new Unification(ImmutableList.<Dob>of());
}
