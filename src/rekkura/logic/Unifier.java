package rekkura.logic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.model.Dob;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * This class describes how to transform one dob to match another dob.
 * @author ptpham
 */
public class Unifier {

	public Dob replace(Dob base, Map<Dob, Dob> substitution) {
		if (substitution.containsKey(base)) return substitution.get(base);
		
		boolean changed = false;
		List<Dob> newChildren = Lists.newArrayListWithCapacity(base.size());
		for (int i = 0; i < base.size(); i++) {
			Dob child = base.at(i);
			Dob replaced = replace(child, substitution);
			if (child != replaced) changed = true;
			newChildren.add(replaced);
		}
		
		if (changed) return new Dob(newChildren);
		return base;
	}
	
	/**
	 * Attempts to unify {@code base} against {@code target}. This method will fail 
	 * (return false) if a node in base aligns with multiple distinct 
	 * nodes in the target.
	 * @param base
	 * @param target
	 * @param current
	 * @return
	 */
	private boolean unify(Dob base, Dob target, Map<Dob, Dob> current) {
		boolean mismatch = false;
		if (base.size() != target.size()) mismatch = true;
		else if(base.isTerminal() && target.isTerminal()
				&& base != target) mismatch = true;
		
		if (mismatch) {
			Dob existing = current.get(base);
			if (existing != null && existing != target) return false;
			current.put(base, target);
		} else if (base != target) {
			for (int i = 0; i < base.size(); i++) {
				if(!unify(base.at(i), target.at(i), current)) return false;
			}
		}
		
		return true;
	}

	public Map<Dob, Dob> unify(Dob base, Dob target) {
		Map<Dob, Dob> result = Maps.newHashMap();
		if (!unify(base, target, result)) return null;
		return result;
	}
	
	/**
	 * Attempts to unify {@code base} against {@code target}. In addition
	 * to the failure modes of {@code unify}, this method will fail if 
	 * some of the substitutions affect nodes in base that are not variables.
	 * @param base
	 * @param target
	 * @param vars
	 * @return
	 */
	public Map<Dob, Dob> unifyVars(Dob base, Dob target, Set<Dob> vars) {
		Map<Dob, Dob> result = unify(base, target);
		if (!vars.containsAll(result.keySet())) return null;
		return result;
	}
	
	/**
	 * Attempts to unify {@code base} against {@code target} with an 
	 * existing partial substitution.
	 * @param base
	 * @param target
	 * @param assignment will be modified
	 * @return
	 */
	public Map<Dob, Dob> unifyAssignment(Dob base, Dob target, Map<Dob, Dob> assignment) {
		if (!this.unify(base, target, assignment)) return null;
		return assignment;
	}
}
