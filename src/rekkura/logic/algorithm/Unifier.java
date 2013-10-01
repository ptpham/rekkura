package rekkura.logic.algorithm;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.model.Rule.Distinct;
import rekkura.logic.model.Unification;
import rekkura.logic.structure.Pool;
import rekkura.util.Colut;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

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
		if (!isVariableUnify(result, vars)) return null;
		return result;
	}
	

	public static boolean isVariableUnify(Map<Dob, Dob> unify, Collection<Dob> vars) {
		return unify == null || vars.containsAll(unify.keySet());
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
	 * Returns true if the src was successfully merged into the dst
	 * unification and false otherwise.
	 * @param dst
	 * @param src
	 * @return
	 */
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
	
	public static Map<Dob, Dob> unifyList(List<Dob> bases, List<Dob> targets) {
		if (bases == null || targets == null) return null;
		if (bases.size() != targets.size()) return null;
		
		Map<Dob, Dob> unify = Maps.newHashMap();
		for (int i = 0; i < bases.size(); i++) {
			Dob base = bases.get(i);
			Dob target = targets.get(i);
			Map<Dob, Dob> current = Unifier.unify(base, target);
			if (!Unifier.mergeUnifications(unify, current)) return null;
		}
		return unify;
	}
	
	public static Map<Dob, Dob> unifyListVars(List<Dob> bodies,
			List<Dob> dobs, Collection<Dob> vars) {
		Map<Dob, Dob> unify = Unifier.unifyList(bodies, dobs);
		if (!Colut.containsAll(Colut.keySet(unify), vars)) return null;
		return unify;
	}
	
	/**
	 * Computes for each target dob the set of source dobs that unify with it.
	 * @param dobs
	 * @param vars
	 * @param fortre 
	 * @return
	 */
	public static Multimap<Dob, Dob> nonConflicting(Iterable<Dob> targetDobs, 
			Iterable<Dob> sourceDobs, Pool pool) {
		Multimap<Dob, Dob> result = HashMultimap.create();
		
		for (Dob target : targetDobs) {
			for (Dob source : sourceDobs) {
				if (!nonConflicting(target, source, pool)
					|| !nonConflicting(source, target, pool)) continue;
				result.put(target, source);
			}
		}
		
		return result;
	}

	public static Dob symmetrize(Dob first, Dob second, Dob var, Pool pool) {
		Dob deepFirst = first.deepCopy(), deepSecond = second.deepCopy();
		Map<Dob,Dob> raw = unify(deepFirst, deepSecond);
		if (raw == null) return null;
		
		Map<Dob,Dob> unify = Maps.newHashMap();
		for (Map.Entry<Dob,Dob> entry : raw.entrySet()) {
			Dob left = pool.dobs.submerge(entry.getKey());
			Dob right = pool.dobs.submerge(entry.getValue());
			
			if (left == right) continue;
			boolean leftVar = pool.allVars.contains(left);
			boolean rightVar = pool.allVars.contains(right);
			if (!leftVar && !rightVar) return null;
			
			unify.put(entry.getKey(), var);
		}
		
		return Unifier.replace(deepFirst, unify);
	}
	
	public static Map<Dob,Dob> homogenizer(Dob dob, Dob var, Set<Dob> vars) {
		Map<Dob,Dob> result = Maps.newHashMap();
		for (Dob child : dob.fullIterable()) {
			if (Colut.contains(vars, child)) result.put(child, var);
		}
		return result;
	}
	
	public static Dob homogenize(Dob dob, Dob var, Pool pool) {
		if (var == null) return pool.dobs.submerge(dob);
		return pool.dobs.submerge(replace(dob, homogenizer(dob, var, pool.allVars)));
	}
	
	public static List<Dob> homogenize(Iterable<Dob> dobs, Dob var, Pool pool) {
		List<Dob> result = Lists.newArrayList();
		for (Dob dob : dobs) result.add(Unifier.homogenize(dob, var, pool));
		return result;
	}
	
	private static boolean conflictCheck(Dob first, Dob second, Pool pool, boolean checkSecond) {
		Dob deepFirst = first.deepCopy(), deepSecond = second.deepCopy();
		Map<Dob,Dob> unify = unify(deepFirst, deepSecond);
		for (Map.Entry<Dob,Dob> entry : unify.entrySet()) {
			Dob left = pool.dobs.submerge(entry.getKey());
			Dob right = pool.dobs.submerge(entry.getValue());
			if (left == right) continue;
			
			boolean varLeft = pool.allVars.contains(left);
			boolean varRight = pool.allVars.contains(right);
			if (!varLeft && (!checkSecond || !varRight)) return false;
		}
		
		return true;
	}

	
	public static boolean homogenousSubset(Dob base, Dob target, Pool pool) {
		return conflictCheck(base, target, pool, false);
	}
	
	public static boolean nonConflicting(Dob first, Dob second, Pool pool) {
		return conflictCheck(first, second, pool, true);
	}
	
}
