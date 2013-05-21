package rekkura.logic.algorithm;

import java.util.*;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.model.Unification;
import rekkura.logic.structure.Cachet;
import rekkura.logic.structure.Pool;
import rekkura.util.Cartesian;
import rekkura.util.NestedIterable;

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
		Iterable<Dob> subtree = cachet.spines.get(dob);
		return new NestedIterable<Dob, Dob>(subtree) {
			@Override protected Iterator<Dob> prepareNext(Dob u) {
				return cachet.unisuccess.get(u).iterator();
			}
		};
	}
	
	/**
	 * Returns a list that contains the assignment domain of each body 
	 * term in the given rule assuming that we want to expand the given
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
	public static Set<Dob> applyBodyExpansion(Rule rule, final ListMultimap<Atom, Dob> support, 
			Pool pool, Set<Dob> truths) {
		Set<Dob> result = Sets.newHashSet();

		// Sort the dimensions of the space so that the smallest ones come first.
		List<Atom> positives = Atom.filterPositives(rule.body);
		sortBySupportSize(positives, support);
		
		List<Atom> negatives = Atom.filterNegatives(rule.body);
		List<List<Unification>> space = constructUnificationSpace(rule, support, positives);
		
		Cartesian.AdvancingIterator<Unification> iterator = Cartesian.asIterator(space);
		Unification unify = Unification.from(rule.vars);
		
		// This block deals with the no variables special case...
		if (rule.vars.size() == 0 && checkGroundAtoms(rule.body, truths)) {
			result.add(rule.head.dob);
			return result;
		}
		
		while (iterator.hasNext()) {
			unify.clear();

			// All positives must contribute in a non-conflicting way
			// to the unification
			int failure = -1;
			List<Unification> assignment = iterator.next();
			
			for (int i = 0; i < assignment.size() && failure < 0; i++) {
				Unification current = assignment.get(i);
				if (!unify.sloppyDirtyMergeWith(current)) failure = i;
			}
			
			// All negatives grounded with the constructed unification
			// should not exist.
			Map<Dob, Dob> converted = failure == -1 ? unify.toMap() : null;
			if (converted != null && negatives.size() > 0) {
				if (!checkNegatives(converted, negatives, truths, pool)) continue;
			}
			
			// If we manage to unify against all bodies, apply the substitution
			// to the head and render it. If the generated head still has variables
			// in it, then do not add it to the result.
			if (converted != null && unify.isValid()) {
				if (rule.evaluateDistinct(converted)) {
					Dob generated = renderHead(converted, rule, pool);
					result.add(generated);
				}
			} else if (failure >= 0) iterator.advance(failure);
		}
		return result;
	}

	public static boolean checkGroundAtoms(Iterable<Atom> body, Set<Dob> truths) {
		for (Atom atom : body) {
			boolean truth = truths.contains(atom.dob);
			if (atom.truth ^ truth) return false;
		}
		return true;
	}

	public static Dob renderHead(Map<Dob, Dob> unify, Rule rule, Pool pool) {
		return pool.dobs.submerge(Unifier.replace(rule.head.dob, unify));
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
		List<Dob> dobs = Atom.asDobList(Atom.filterPositives(rule.body));
		Map<Dob, Dob> unify = Unifier.unifyListVars(dobs, bodies, rule.vars);
		if (!checkNegatives(unify, rule.body, truths, pool)) return null;
		if (!rule.evaluateDistinct(unify)) return null;
		return renderHead(unify, rule, pool);
	}

	/**
	 * Returns true if the unification does not yield any dob
	 * contained the set of truths passed in.
	 * @param unify
	 * @param atoms
	 * @param truths
	 * @param pool
	 * @return
	 */
	public static boolean checkNegatives(Map<Dob, Dob> unify,
			List<Atom> atoms, Set<Dob> truths, Pool pool) {
		for (Atom atom : atoms) {
			if (atom.truth) continue;
			Dob generated = pool.dobs.submerge(Unifier.replace(atom.dob, unify));
			if (generated == null || truths.contains(generated)) return false;
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

	public static void sortBySupportSize(List<Atom> positives,
			final ListMultimap<Atom, Dob> support) {
		Collections.sort(positives, new Comparator<Atom>() {
			@Override public int compare(Atom first, Atom second) {
				return support.get(first).size() - support.get(second).size();
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
	public static Map<Dob, Dob> applyVariables(Rule rule, List<Dob> candidates, 
			Set<Dob> truths, Pool pool) {
		Map<Dob, Dob> unify = Maps.newHashMap();
		List<Dob> vars = rule.vars; 
		List<Atom> body = rule.body;
		
		// Construct replacement
		if (rule.vars.size() != candidates.size()) return null;
		for (int i = 0; i < vars.size(); i++) {
			unify.put(vars.get(i), candidates.get(i));
		}
		
		boolean success = true;
		for (int i = 0; i < body.size() && success; i++) {
			Atom atom = body.get(i);
		
			// Generate the ground body
			Dob ground = pool.dobs.submerge(Unifier.replace(atom.dob, unify));
			boolean truth = truths.contains(ground);
			if (truth != atom.truth) success = false;
		}
		
		if (!success) return null;
		return unify;
	}
}
