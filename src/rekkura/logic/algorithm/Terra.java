package rekkura.logic.algorithm;

import java.util.*;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.model.Unification;
import rekkura.logic.structure.Pool;
import rekkura.util.Cartesian;
import rekkura.util.Cartesian.AdvancingIterator;
import rekkura.util.Colut;

import com.google.common.collect.*;

/**
 * This class holds a collection of utilities for generating
 * and using groundings. In general, a "support" is a Multimap
 * that maps from Atoms in the body of a {@link Rule} to the 
 * groundings that might unify with that atom.
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
	
	/**
	 * Returns a comparator that sorts in increasing order of
	 * overlap with vars. 
	 * @param vars
	 * @param other
	 * @return
	 */
	public static Comparator<Atom> getOverlapComparator(final Collection<Dob> vars) {
		return new Comparator<Atom>() {
			@Override public int compare(Atom first, Atom second) {
				int left = Colut.countIn(first.dob.fullIterable(), vars);
				int right = Colut.countIn(second.dob.fullIterable(), vars);
				return left - right;
			}
		};
	}

	/**
	 * Selects a subset of the atoms in the body of a rule for expansion based
	 * on the minimum cost. Stops once all variables are covered.
	 * @param rule
	 * @param costs
	 * @return
	 */
	public static List<Atom> getGreedyVarCover(Rule rule, Map<Atom,Integer> costs) {
		// Sort the dimensions of the space so that the smallest ones come first.
		List<Atom> positives = Atom.filterPositives(rule.body);
		Colut.sortByMap(positives, costs, 0);
		
		// Then greedily find a variable cover and resort for the final support
		List<Atom> expanders = Terra.getVarCover(positives, rule.vars);
		if (expanders == null) return null;
		
		prioritizeExpanders(rule, expanders);
		return expanders;
	}

	protected static void prioritizeExpanders(Rule rule, List<Atom> expanders) {
		// Prioritize variables in the head
		List<Comparator<Atom>> comparators = Lists.newArrayList();
		comparators.add(getPresenceComparator(Lists.newArrayList(rule.head.dob.fullIterable())));
		
		// Prioritize distincts
		if (rule.distinct.size() > 0) {
			List<Dob> vars = Colut.intersect(Rule.dobIterableFromDistincts(rule.distinct), rule.vars);
			comparators.add(getPresenceComparator(vars));
		}
		
		Collections.sort(expanders, Ordering.compound(comparators));
	}
	
	/**
	 * Compares such that atoms that contain the given dobs come before
	 * the onese that do not.
	 * @param targets
	 * @return
	 */
	public static Comparator<Atom> getPresenceComparator(final Collection<Dob> targets) {
		return new Comparator<Atom>() {
			@Override public int compare(Atom left, Atom right) {
				boolean first = Colut.containsAny(left.dob.fullIterable(), targets);
				boolean second = Colut.containsAny(right.dob.fullIterable(), targets);
				if (first == second) return 0;
				if (first) return -1;
				return 1;
			}
		};
	}
	
	/**
	 * This method can be used to handle the vacuous/varless rule special case.
	 * @param rule
	 * @param truths
	 * @return
	 */
	public static boolean applyVarless(Rule rule, Set<Dob> truths, List<Map<Dob, Dob>> result) {
		if (rule.vars.size() == 0) {
			if(checkGroundAtoms(rule.body, truths)) {
				result.add(Maps.<Dob,Dob>newHashMap());
				return true;
			}
		}
		return false;
	}

	public static Dob applyVarless(Rule rule, Set<Dob> truths, Pool pool) {
		List<Map<Dob,Dob>> result = Lists.newArrayList();
		if (applyVarless(rule, truths, result)) return pool.render(rule.head.dob, Colut.any(result));
		return null;
	}
	
	public static List<Atom> getVarCover(Iterable<Atom> atoms, Iterable<Dob> vars) {
		List<Dob> remaining = Lists.newArrayList(vars);
		List<Atom> result = Lists.newArrayList();
		
		for (Atom atom : atoms) {
			if (remaining.size() == 0) break;
			if (Colut.removeAll(remaining, atom.dob.fullIterable())) {
				result.add(atom);
			}
		}
		
		if (remaining.size() > 0) return null;
		return result;
	}
	
	/**
	 * Generates a variable cover that greedily selects in each iteration
	 * the atom that covers first the most already covered variables and
	 * then the least uncovered variables.
	 * @param atoms
	 * @param vars
	 * @return
	 */
	public static List<Atom> getChainingCover(Iterable<Atom> atoms, Collection<Dob> vars) {
		List<Atom> available = Lists.newArrayList(atoms);
		Set<Dob> covered = Sets.newHashSet();
		List<Atom> result = Lists.newArrayList();

		List<Comparator<Atom>> comparators = Lists.newArrayList();		
		comparators.add(getOverlapComparator(covered));
		comparators.add(Collections.reverseOrder(getOverlapComparator(vars)));
		Comparator<Atom> comparator = Ordering.compound(comparators);

		while (covered.size() < vars.size() && available.size() > 0) {
			Atom next = Collections.max(available, comparator);
			
			available.remove(next);
			if (covered.addAll(Colut.intersect(next.dob.fullIterable(), vars))) {
				result.add(next);
			}
		}
		
		if (covered.size() < vars.size()) return null;
		return result;
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
		Dob varless = applyVarless(rule, truths, pool);
		if (varless != null) return varless;
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
	
	/**
	 * Converts a basic support to a more performant representation.
	 * Each inner list at position i corresponds to the unifications
	 * that succeeded with the atom at position i in the body of the rule.
	 * @param rule
	 * @param support
	 * @param positives the list of atoms we actually want to keep from the
	 * support
	 * @return
	 */
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
}
