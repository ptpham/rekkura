package rekkura.logic.algorithm;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.model.Unification;
import rekkura.logic.structure.Cachet;
import rekkura.logic.structure.Pool;
import rekkura.util.Cartesian;
import rekkura.util.Cartesian.AdvancingIterator;
import rekkura.util.Colut;
import rekkura.util.RankedCarry;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class holds a collection of utilities for generating
 * and using groundings.
 * @author ptpham
 *
 */
public class Terra {
	
	/**
	 * This method returns an iterable over all exhausted ground dobs 
	 * that potentially unify with the given body term.
	 * @param dob
	 * @return
	 */
	public static Iterable<Dob> getGroundCandidates(Dob dob, final Cachet cachet) {
		Dob canonical = cachet.canonicalForms.get(dob);
		return cachet.formToGrounds.get(canonical);
	}
	
	/**
	 * Returns a list that contains the assignment domain of each positive
	 * body term in the given rule assuming that we want to expand the given
	 * dob at the given position.
	 * @param rule
	 * @param position
	 * @param dob
	 * @return
	 */
	public static ListMultimap<Atom, Dob> getBodySpace(Rule rule, Cachet cachet) {
		ListMultimap<Atom, Dob> candidates = ArrayListMultimap.create();
		
		for (int i = 0; i < rule.body.size(); i++) {
			Atom atom = rule.body.get(i);
			if (!atom.truth) continue;
			
			Iterable<Dob> grounds = getGroundCandidates(atom.dob, cachet);
			List<Dob> next = Lists.newArrayList(grounds);
			candidates.putAll(atom, next);
		}
		return candidates;
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
	public static AdvancingIterator<Unification> applyBodyExpansion(Rule rule,
		List<Atom> expanders, ListMultimap<Atom, Dob> support, Set<Dob> truths) {
		if (rule.vars.size() == 0) return Cartesian.emptyIterator();
		if (expanders == null) return Cartesian.emptyIterator();

		// Construct iterator and expand
		List<List<Unification>> space = constructUnificationSpace(rule, support, expanders);
		return Cartesian.asIterator(space);
	}
	
	public static List<Map<Dob,Dob>> applyExpansion(Rule rule, 
		AdvancingIterator<Unification> iterator, List<Atom> check, Pool pool, Set<Dob> truths) {
		List<Map<Dob,Dob>> result = Lists.newArrayList();
		
		// This block deals with the vacuous rule special case ...
		Dob varless = applyVarless(rule, truths);
		if (varless != null) {
			result.add(Maps.<Dob,Dob>newHashMap());
			return result;
		}
		
		return expandBodyAssignments(rule, check, iterator, pool, truths);
	}

	public static List<Atom> getGreedyExpanders(Rule rule,
			final ListMultimap<Atom, Dob> support) {
		// Sort the dimensions of the space so that the smallest ones come first.
		List<Atom> positives = Atom.filterPositives(rule.body);
		sortBySupportSize(positives, support);
		
		// Then greedily find a variable cover and resort for the final support
		List<Atom> expanders = Terra.greedyVarCover(positives, rule.vars);
		if (expanders == null) return null;
		sortBySupportSize(expanders, support);
		return expanders;
	}

	public static List<Map<Dob, Dob>> expandBodyAssignments(Rule rule, List<Atom> check,
		Cartesian.AdvancingIterator<Unification> iterator, Pool pool, Set<Dob> truths) {
		
		List<Map<Dob,Dob>> result = Lists.newArrayList();
		Unification unify = Unification.from(rule.vars);
		
		while (iterator.hasNext()) {
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
	public static List<Atom> greedyVarCover(Iterable<Atom> atoms, Iterable<Dob> vars) {
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

	// TODO: Clean this up
	public static Dob renderHead(Map<Dob, Dob> unify, Rule rule, Pool pool) {
		return pool.dobs.submerge(Unifier.replace(rule.head.dob, unify));
	}
	
	public static Dob renderGround(Dob dob, Map<Dob,Dob> unify, Pool pool) {
		return pool.dobs.submerge(Unifier.replace(dob, unify));
	}
	
	public static Set<Dob> renderHeads(Iterable<Map<Dob,Dob>> unifies, Rule rule, Pool pool) {
		Set<Dob> result = Sets.newHashSet();
		for (Map<Dob,Dob> unify : unifies) result.add(renderHead(unify, rule, pool));
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
		return renderHead(unify, rule, pool);
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
	
	private static List<List<Unification>> constructUnificationSpace(Rule rule,
			final ListMultimap<Atom, Dob> support, List<Atom> positives) {
		List<List<Unification>> space = Lists.newArrayList();
		for (Atom atom : positives) {
			List<Dob> grounds = support.get(atom);
			List<Unification> unifies = Lists.newArrayList();
			for (Dob ground : grounds) {
				Map<Dob, Dob> unify = Unifier.unifyVars(atom.dob, ground, rule.vars);
				Unification wrapped = unify == null ? null : Unification.from(unify, rule.vars);
				unifies.add(wrapped); 
			}
			space.add(unifies);
		}
		return space;
	}

	/**
	 * Sorts in increasing order of support size except for a support size
	 * of zero. These are placed at the end.
	 * @param positives
	 * @param support
	 */
	public static void sortBySupportSize(List<Atom> positives,
			final ListMultimap<Atom, Dob> support) {
		Collections.sort(positives, new Comparator<Atom>() {
			@Override public int compare(Atom first, Atom second) {
				int firstSize = support.get(first).size();
				int secondSize = support.get(second).size();
				if (firstSize == 0 && secondSize == 0) return 0;
				if (firstSize == 0) return 1;
				return firstSize - secondSize;
			}
		});
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
		return unify;
	}

}
