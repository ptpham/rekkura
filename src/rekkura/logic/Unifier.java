package rekkura.logic;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.model.Dob;
import rekkura.model.Unification;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * This class describes how to transform one dob to match another dob.
 * Such a transformation is called a unification. In general, we 
 * want to represent unifications as {@code Map<Dob, Dob>} for ease 
 * of use. However, there is the high merge performance representation 
 * called {@link Unification}.
 * @author ptpham
 */
public class Unifier {

	public static Dob replace(Dob base, Map<Dob, Dob> substitution) {
		if (substitution == null) return null;
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
	public static Map<Dob, Dob> unifyVars(Dob base, Dob target, Collection<Dob> vars) {
		Map<Dob, Dob> result = unify(base, target);
		if (isVariableUnify(result, vars)) return null;
		return result;
	}
	

	public static boolean isVariableUnify(Map<Dob, Dob> unify, Collection<Dob> vars) {
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
	public static boolean unifyAssignment(Dob base, Dob target, Map<Dob, Dob> assignment) {
		return unify(base, target, assignment);
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
		
		for (Map.Entry<Dob, Dob> entry : unify.entrySet()) {
			Dob key = entry.getKey(), value = entry.getValue();
			if (!variables.contains(key) && !variables.contains(value)) return null;
			result.put(key, vargen.get());
		}
		
		return result;
	}
	
	public static boolean isSymmetricPair(Dob first, Dob second, Set<Dob> vars) {
		Dob.PrefixedSupplier vargen = new Dob.PrefixedSupplier("tmp");
		Map<Dob, Dob> symmetrizer = oneSidedSymmetrizer(first, second, vars, vargen);
		return symmetrizer != null;
	}

	private static Map<Dob, Dob> oneSidedSymmetrizer(Dob first, Dob second,
			Set<Dob> vars, Supplier<Dob> vargen) {
		Map<Dob, Dob> unification = Unifier.unify(first, second);
		Map<Dob, Dob> symmetrizer = Unifier.symmetrizeUnification(unification, vars, vargen);
		return symmetrizer;
	}
	
	public static Dob computeSymmetricGeneralization(Dob first, Dob second,
			Set<Dob> vars, Supplier<Dob> vargen) {
		Dob result = oneSidedSymmetricGeneralization(first, second, vars, vargen);
		if (result == null) result = oneSidedSymmetricGeneralization(second, first, vars, vargen);
		return result;
	}
	
	public static Dob oneSidedSymmetricGeneralization(Dob first, Dob second,
			Set<Dob> vars, Supplier<Dob> vargen) {
		Map<Dob, Dob> symmetrizer = oneSidedSymmetrizer(first, second, vars, vargen);
		return replace(first, symmetrizer);
	}

	public static boolean mergeUnifications(Map<Dob, Dob> dst, Map<Dob, Dob> src) {
		if (src == null) return false;
		for (Map.Entry<Dob, Dob> pair : src.entrySet()) {
			Dob key = pair.getKey();
			Dob value = pair.getValue();
			
			if (dst.containsKey(key) && dst.get(key) != value) return false;
			dst.put(key, value);
		}
		return true;
	}
	
	public static List<Dob> retainSuccesses(Dob query, Iterable<Dob> targets, Set<Dob> allVars) {
		List<Dob> result = Lists.newArrayList();
		
		for (Dob target : targets) {
			if (unifyVars(query, target, allVars) != null) {
				result.add(target);
			}
		}
		
		return result;
	}
}
