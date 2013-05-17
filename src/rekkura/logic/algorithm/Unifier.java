package rekkura.logic.algorithm;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.model.*;
import rekkura.logic.model.Rule.Distinct;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
			if (replaced != null) newChildren.add(replaced);
		}
		
		if (changed) return new Dob(newChildren);
		return base;
	}
	
	public static List<Dob> replaceDobs(Iterable<Dob> dobs, Map<Dob, Dob> substitution) {
		List<Dob> result = Lists.newArrayList();
		for(Dob dob : dobs) result.add(replace(dob, substitution));
		return result;
	}
	
	public static Atom replace(Atom base, Map<Dob, Dob> substitution) {
		Dob dob = replace(base.dob, substitution);
		if (dob == base.dob) return base;
		return new Atom(dob, base.truth);
	}
	
	public static List<Atom> replaceAtoms(Iterable<Atom> terms, Map<Dob, Dob> substitution) {
		List<Atom> result = Lists.newArrayList();
		for (Atom term : terms) result.add(replace(term, substitution));
		return result;
	}
	
	/**
	 * @param base
	 * @param substitution
	 * @param dstVars the set of variables to keep after performing the substitution
	 * @return
	 */
	public static Rule replace(Rule base, Map<Dob, Dob> substitution, Collection<Dob> dstVars) {
		Atom head = replace(base.head, substitution);
		List<Atom> body = replaceAtoms(base.body, substitution);
		List<Rule.Distinct> distincts = replaceDistincts(base.distinct, substitution);

		List<Dob> vars = Lists.newArrayList();
		for (Dob var : base.vars) {
			Dob replacement = replace(var, substitution);
			if (!dstVars.contains(replacement)) continue;
			vars.add(replacement);
		}
		
		Rule result = new Rule(head, body, vars, distincts);
		if (Rule.orderedRefeq(base, result)) return base;
		return result;
	}
	
	public static Rule.Distinct replace(Rule.Distinct base, Map<Dob, Dob> substitution) {
		Dob first = replace(base.first, substitution);
		Dob second = replace(base.second, substitution);
		if (first == base.first && second == base.second) return base;
		return new Rule.Distinct(first, second);
	}

	private static List<Distinct>
	replaceDistincts(Iterable<Distinct> distincts, Map<Dob, Dob> substitution) {
		List<Rule.Distinct> result = Lists.newArrayList();
		for (Rule.Distinct distinct : distincts) result.add(replace(distinct, substitution));
		return result;
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
	 * @param conflicts
	 * @param vargen supplies new variables for the generalization
	 * @return
	 */
	public static Map<Dob, Dob> symmetrize(Map<Dob, Dob> unify, 
		Set<Dob> conflicts, Vars.Context context) {
		
		if (unify == null) return null;
		Map<Dob, Dob> result = Maps.newHashMap();
		
		Collection<Dob> vars = context.getAll();
		for (Map.Entry<Dob, Dob> entry : unify.entrySet()) {
			Dob key = entry.getKey(), value = entry.getValue();
			if (!vars.contains(key) && !vars.contains(value)) return null;
			
			Dob request = Vars.request(conflicts, context);
			result.put(key, request);
			conflicts.add(request);
		}
		
		return result;
	}
	
	public static boolean isSymmetricPair(Dob first, Dob second, Vars.Context context) {
		return getSymmetrizer(first, second, context) != null;
	}
	
	public static Dob symmetrizeBothSides(Dob first, Dob second, Vars.Context context) {
		Dob result = symmetrize(first, second, context);
		if (result == null) result = symmetrize(second, first, context);
		return result;
	}
	
	public static Dob symmetrize(Dob first, Dob second, Vars.Context context) {
		Map<Dob, Dob> symmetrizer = getSymmetrizer(first, second, context);
		return replace(first, symmetrizer);
	}
	
	private static Map<Dob, Dob> getSymmetrizer(Dob first, Dob second, Vars.Context context) {
		Map<Dob, Dob> unification = Unifier.unify(first, second);
		
		Set<Dob> allVars = context.getAll();
		Set<Dob> conflicts = Sets.newHashSet(first.fullIterable());
		Iterables.addAll(conflicts, second.fullIterable());
		conflicts.retainAll(allVars);
		
		Map<Dob, Dob> symmetrizer = Unifier.symmetrize(unification, conflicts, context);
		return symmetrizer;
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
