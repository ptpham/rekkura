package rekkura.logic.algorithm;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.model.Unification;
import rekkura.logic.structure.Pool;
import rekkura.util.Cartesian;
import rekkura.util.Cartesian.AdvancingIterator;
import rekkura.util.Colut;
import rekkura.util.RankedCarry;

import com.google.common.collect.*;

/**
 * This class holds a collection of utilities for generating
 * and using groundings.
 * @author ptpham
 *
 */
public class Terra {
	public static AdvancingIterator<Unification> getUnificationIterator(Rule rule,
		List<Atom> expanders, Multimap<Atom, Dob> support, Set<Dob> truths) {
		if (rule.vars.size() == 0) return Cartesian.emptyIterator();
		if (expanders == null) return Cartesian.emptyIterator();

		// Construct iterator and expand
		List<List<Unification>> space = getUnificationSpace(rule, support, expanders);
		return Cartesian.asIterator(space);
	}

	public static List<Atom> getGreedyVarCoverExpanders(Rule rule, Map<Atom,Integer> costs) {
		// Sort the dimensions of the space so that the smallest ones come first.
		List<Atom> positives = Atom.filterPositives(rule.body);
		Colut.sortByMap(positives, costs, 0);
		
		// Then greedily find a variable cover and resort for the final support
		List<Atom> expanders = Terra.getGreedyVarCover(positives, rule.vars);
		if (expanders == null) return null;
		Colut.sortByMap(expanders, costs, 0);
		return expanders;
	}
		
	/**
	 * This method exposes an efficient rendering process for a collection of ground dobs.
	 * If you want to apply a single assignment in a vaccuum, consider applyBodies.
	 * To generate the support for this function, consider using getBodySpace.
	 * @param rule
	 * @param support
	 * @param pool
	 * @param truths
	 * @return
	 */
	public static List<Map<Dob,Dob>> applyUnifications(Rule rule, 
		AdvancingIterator<Unification> iterator, List<Atom> check, Pool pool, Set<Dob> truths) {
		List<Map<Dob,Dob>> result = Lists.newArrayList();
		
		// This block deals with the vacuous rule special case ...
		Dob varless = applyVarless(rule, truths);
		if (varless != null) {
			result.add(Maps.<Dob,Dob>newHashMap());
			return result;
		}
		
		return expandUnifications(rule, check, iterator, pool, truths);
	}

	public static List<Map<Dob, Dob>> expandUnifications(Rule rule, List<Atom> check,
		Cartesian.AdvancingIterator<Unification> iterator, Pool pool, Set<Dob> truths) {
		
		List<Map<Dob,Dob>> result = Lists.newArrayList();
		Unification unify = Unification.from(rule.vars);
		
		while (iterator.hasNext()) {
			if (Thread.interrupted()) throw new RuntimeException();
			unify.clear();

			// Dobs in the variable cover must contribute in a
			// non conflicting way to the unification.
			int failure = -1;
			List<Unification> assignment = iterator.next();
			
			for (int i = 0; i < assignment.size() && failure < 0; i++) {
				Unification current = assignment.get(i);
				if (!unify.sloppyDirtyMergeWith(current)) failure = i;
			}
			
			// Verify that the atoms that did not participate in the unification
			// have their truth values satisfied.
			Map<Dob, Dob> converted = failure == -1 ? unify.toMap() : null;
			if (converted != null && check.size() > 0) {
				if (!checkAtoms(converted, check, truths, pool)) continue;
			}
			
			// Final check for distincts before rendering head
			if (converted != null && unify.isValid()) {
				if (rule.evaluateDistinct(converted)) result.add(converted);
			} else if (failure >= 0) iterator.advance(failure);
		}
		return result;
	}

	/**
	 * Returns a cover of the variables obtained by greedily selecting atoms 
	 * that cover the most uncovered variables.
	 * @param atoms
	 * @param vars
	 * @return
	 */
	public static List<Atom> getGreedyVarCover(Iterable<Atom> atoms, Iterable<Dob> vars) {
		List<Dob> remaining = Lists.newArrayList(vars);
		List<Atom> result = Lists.newArrayList();
		
		while (remaining.size() > 0) {
			RankedCarry<Integer, Atom> carry = RankedCarry.createReverseNatural(0, null);
			for (Atom candidate : atoms) {
				int count = Colut.countIn(candidate.dob.fullIterable(), remaining);
				carry.consider(count, candidate);
			}
			
			Atom atom = carry.getCarry();
			if (atom == null) return null;
			
			Colut.removeAll(atom.dob.fullIterable(), remaining);
			result.add(atom);
		}
		
		return result;
	}
	
	/**
	 * This method can be used to handle the vacuous/varless rule special case.
	 * @param rule
	 * @param truths
	 * @return
	 */
	public static Dob applyVarless(Rule rule, Set<Dob> truths) {
		if (rule.vars.size() == 0 && checkGroundAtoms(rule.body, truths)) {
			return rule.head.dob;
		} else return null;
	}

	public static boolean checkGroundAtoms(Iterable<Atom> body, Set<Dob> truths) {
		for (Atom atom : body) {
			boolean truth = truths.contains(atom.dob);
			if (atom.truth ^ truth) return false;
		}
		return true;
	}
	
	public static Set<Dob> renderHeads(Iterable<Map<Dob,Dob>> unifies, Rule rule, Pool pool) {
		Set<Dob> result = Sets.newHashSet();
		for (Map<Dob,Dob> unify : unifies) result.add(pool.render(rule.head.dob, unify));
		return result;
	}
	
	/**
	 * Attempts to generate a rule's head with the given assignment to the rule's body
	 * and the given set of things that are currently true.
	 * @param rule
	 * @param bodies these should be in order of the positive atoms in the rule
	 * @param truths
	 * @param pool
	 * @return
	 */
	public static Dob applyBodies(Rule rule, List<Dob> bodies, Set<Dob> truths, Pool pool) {
		Dob varless = applyVarless(rule, truths);
		if (varless != null) return pool.dobs.submerge(varless);
		List<Dob> dobs = Atom.asDobList(Atom.filterPositives(rule.body));
		Map<Dob, Dob> unify = Unifier.unifyListVars(dobs, bodies, rule.vars);
		if (!checkAtoms(unify, Atom.filterNegatives(rule.body), truths, pool)) return null;
		if (!rule.evaluateDistinct(unify)) return null;
		return pool.render(rule.head.dob, unify);
	}

	/**
	 * Returns true if the unification satisfies the atoms that
	 * need to be checked.
	 * @param unify
	 * @param atoms
	 * @param truths
	 * @param pool
	 * @return
	 */
	public static boolean checkAtoms(Map<Dob, Dob> unify,
			List<Atom> atoms, Set<Dob> truths, Pool pool) {
		for (Atom atom : atoms) {
			Dob generated = pool.dobs.submerge(Unifier.replace(atom.dob, unify));
			if (generated == null || truths.contains(generated) != atom.truth) return false;
		}
		return true;
	}
	
	public static List<List<Unification>> getUnificationSpace(Rule rule,
			final Multimap<Atom, Dob> support, List<Atom> positives) {
		List<List<Unification>> result = Lists.newArrayList();
 		for (Atom atom : positives) {
			Collection<Dob> grounds = support.get(atom);
			List<Unification> unifies = Lists.newArrayList();
			for (Dob ground : grounds) {
				Map<Dob, Dob> unify = Unifier.unifyVars(atom.dob, ground, rule.vars);
				Unification wrapped = unify == null ? null : Unification.from(unify, rule.vars);
				if (wrapped != null) unifies.add(wrapped); 
			}
			result.add(unifies);
		}
		return result;
	}
	
	/**
	 * This method attempts to apply candidates as variables. The order used
	 * is the order of the variables in the rule.
	 * @param rule
	 * @param candidates
	 * @param truths
	 * @param pool
	 * @return
	 */
	public static Map<Dob, Dob> applyVars(Rule rule, List<Dob> candidates, 
			Set<Dob> truths, Pool pool) {
		Map<Dob, Dob> unify = Maps.newHashMap();
		List<Dob> vars = rule.vars; 
		List<Atom> body = rule.body;
		
		// Construct replacement
		if (rule.vars.size() != candidates.size()) return null;
		for (int i = 0; i < vars.size(); i++) {
			unify.put(vars.get(i), candidates.get(i));
		}
		
		if (!checkAtoms(unify, body, truths, pool)) return null;
		if (!rule.evaluateDistinct(unify)) return null;
		return unify;
	}

	public static HashMultimap<Dob,Dob> indexBy(Iterable<Dob> dobs, Collection<Dob> targets) {
		HashMultimap<Dob,Dob> result = HashMultimap.create();
		for (Dob dob : dobs) {
			for (Dob child : dob.fullIterable()) {
				if (targets.contains(child)) result.put(child, dob);
			}
		}
		return result;
	}
	
	public static ArrayListMultimap<Dob,Unification> indexBy(Iterable<Unification> slice, int pos) {
		ArrayListMultimap<Dob,Unification> result = ArrayListMultimap.create();
		for (Unification unify : slice) result.put(unify.assigned[pos], unify);
		return result;
	}
	
	/**
	 * This method returns variables that are shared between at least two of the
	 * given terms. They are sorted in decreasing order of overlap over terms and
	 * variables with no overlap are not returned.
	 * @param terms
	 * @param vars
	 * @return
	 */
	public static List<Dob> getPartitionCandidates(Iterable<Dob> terms, Collection<Dob> vars) {
		Multiset<Dob> counts = HashMultiset.create();
		for (Dob dob : terms) {
			Set<Dob> all = Sets.newHashSet(dob.fullIterable());
			for (Dob var : vars) if (all.contains(var)) counts.add(var);
		}
		
		List<Dob> result = Lists.reverse(Colut.sortByCount(counts));
		for (int i = result.size() - 1; i > 0; i--) {
			if (counts.count(result.get(i)) < 2) result.remove(i);
		}
		return result;
	}
}
