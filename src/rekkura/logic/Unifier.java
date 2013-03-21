package rekkura.logic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.model.Dob;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class describes how to transform one dob to match another dob.
 * @author ptpham
 */
public class Unifier {

	public static Dob replace(Dob base, Map<Dob, Dob> substitution) {
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
	 * @param state
	 * @return
	 */
	private static boolean unify(Dob base, Dob target, Map<Dob, Dob> current) {
		if (base == null || target == null) return false;
		
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

	public static Map<Dob, Dob> unify(Dob base, Dob target) {
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
	public static Map<Dob, Dob> unifyVars(Dob base, Dob target, Set<Dob> vars) {
		Map<Dob, Dob> result = unify(base, target);
		if (isVariableUnify(result, vars)) return null;
		return result;
	}

	public static boolean isVariableUnify(Map<Dob, Dob> unify, Set<Dob> vars) {
		return unify == null || !vars.containsAll(unify.keySet());
	}
	
	/**
	 * Attempts to unify {@code base} against {@code target} with an 
	 * existing partial substitution.
	 * @param base
	 * @param target
	 * @param assignment will be modified
	 * @return
	 */
	public static Map<Dob, Dob> unifyAssignment(Dob base, Dob target, Map<Dob, Dob> assignment) {
		if (!unify(base, target, assignment)) return null;
		return assignment;
	}
	
	/**
	 * Two dobs are equivalent under a set of variables if the variable unification 
	 * in both directions exists and are of the same size.
	 * @param base
	 * @param target
	 * @param vars
	 * @return
	 */
	public static boolean equivalent(Dob first, Dob second, Set<Dob> vars) {
		Map<Dob, Dob> forward = unifyVars(first, second, vars);
		Map<Dob, Dob> backward = unifyVars(second, first, vars);
		if (forward == null || backward == null) return false;
		return forward.size() == backward.size();
	}
	
	/**
	 * Generates the unification that, when applied to the base dob that originally generated the 
	 * unification, will yield a generalization of the base dob and the target dob. If we see
	 * a variable as a value in the unify map twice, then this is not a valid symmetric unification.
	 * @param unify
	 * @param variables
	 * @param vargen supplies new variables for the generalization
	 * @return
	 */
	public static Map<Dob, Dob> symmetrizeUnification(Map<Dob, Dob> unify, Set<Dob> variables, Supplier<Dob> vargen) {
		if (unify == null) return null;
		Map<Dob, Dob> result = Maps.newHashMap();
		
		Set<Dob> targets = Sets.newHashSet();
		for (Map.Entry<Dob, Dob> entry : unify.entrySet()) {
			Dob key = entry.getKey(), value = entry.getValue();
			if (variables.contains(key)) continue;
			if (variables.contains(value)) {
				result.put(key, vargen.get());
				if (!targets.add(value)) return null;
			}
			else return null;
		}
		
		return result;
	}
	
	public static Dob getSymmetricGeneralization(Dob first, Dob second, Set<Dob> vars, Supplier<Dob> vargen) {
		Dob result = computeSymmetricGeneralization(first, second, vars, vargen);
		if (result == null) return computeSymmetricGeneralization(second, first, vars, vargen);
		else return result;
	}

	private static Dob computeSymmetricGeneralization(Dob first, Dob second,
			Set<Dob> vars, Supplier<Dob> vargen) {
		Map<Dob, Dob> unify = Unifier.unify(first, second);
		Map<Dob, Dob> symmetrizer = Unifier.symmetrizeUnification(unify, vars, vargen);
		if (symmetrizer == null) return null;
		return replace(first, symmetrizer);
	}
}
