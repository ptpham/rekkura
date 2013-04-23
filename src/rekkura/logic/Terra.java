package rekkura.logic;

import java.util.*;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.model.Unification;
import rekkura.util.Cartesian;
import rekkura.util.Colut;
import rekkura.util.NestedIterable;
import rekkura.util.OtmUtil;

import com.google.common.collect.*;

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
	
	public static Set<Dob> applyBodyExpansion(Rule rule, final ListMultimap<Atom, Dob> support, 
			Pool pool, Set<Dob> truths) {
		Set<Dob> result = Sets.newHashSet();

		// Sort the dimensions of the space so that the smallest ones come first.
		List<Atom> positives = getSortedPositives(rule, support);
		
		List<Atom> negatives = rule.getNegatives();
		List<List<Unification>> space = constructUnificationSpace(rule, support, positives);
		
		Cartesian.AdvancingIterator<Unification> iterator = Cartesian.asIterator(space);
		Unification unify = Unification.from(rule.vars);

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
				boolean failed = false;
				for (Atom atom : negatives) {
					Dob generated = pool.dobs.submerge(Unifier.replace(atom.dob, converted));
					if (truths.contains(generated)) failed = true;
				}
				if (failed) continue;
			}
			
			// If we manage to unify against all bodies, apply the substitution
			// to the head and render it. If the generated head still has variables
			// in it, then do not add it to the result.
			if (converted != null && unify.isValid()) {
				if (rule.evaluateDistinct(converted)) {
					Dob generated = pool.dobs.submerge(Unifier.replace(rule.head.dob, converted));
					result.add(generated);
				}
			} else if (failure >= 0) iterator.advance(failure);
		}
		return result;
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

	private static List<Atom> getSortedPositives(Rule rule,
			final ListMultimap<Atom, Dob> support) {
		List<Atom> positives = rule.getPositives();
		Collections.sort(positives, new Comparator<Atom>() {
			@Override public int compare(Atom first, Atom second) {
				return support.get(first).size() - support.get(second).size();
			}
		});
		return positives;
	}
	
	/**
	 * Returns a list of the possible assignments to the variables in the
	 * given rule assuming that the given dob must be applied at
	 * the given position.
	 * @param rule
	 * @param position
	 * @param ground
	 * @return
	 */
	public static List<Iterable<Dob>> getVariableSpace(Rule rule, Cachet.VarAux cachet) {
		// Add a single null for rules with no variables
		List<Iterable<Dob>> candidates = Lists.newArrayList();
		if (rule.vars.size() == 0) {
			candidates.add(Lists.newArrayList((Dob)null));
			return candidates;
		}
		
		Multimap<Dob, Dob> variables = HashMultimap.create();
		
		for (int i = 0; i < rule.body.size(); i++) {
			Atom atom = rule.body.get(i);
			if (!atom.truth) continue;
			
			// For each node in the subtree, find the set of replacements
			// in terms of the root of the subtree. Then join right
			// to rephrase in terms of variables in the rule.
			Iterable<Dob> subtree = cachet.spines.get(atom.dob);
			for (Dob node : subtree) {
				Multimap<Dob, Dob> raw = cachet.unispaces.get(node).replacements;
				Map<Dob, Dob> left = Unifier.unify(atom.dob, node);
				Multimap<Dob, Dob> replacements = HashMultimap.create();
				if (left != null && Colut.nonEmpty(left.keySet())) {
					replacements = OtmUtil.joinRight(Multimaps.forMap(left), raw);
				}
				
				for (Dob variable : rule.vars) {
					if (left == null || !left.containsKey(variable)) {
						replacements.putAll(variable, raw.get(variable));
					}
				}
				
				for (Dob variable : replacements.keySet()) {
					variables.putAll(variable, replacements.get(variable));
				}
				
				// Add stuff that was not included in the join but is 
				// still necessary for a valid unification.
				if (left == null) continue;
				for (Map.Entry<Dob, Dob> entry: left.entrySet()) {
					if (!cachet.allVars.contains(entry.getValue()))
						variables.put(entry.getKey(), entry.getValue());
				}
			}
		}
		
		for (Dob variable : rule.vars) {
			candidates.add(variables.get(variable));
		}
		
		return candidates;
	}
	
	
	/**
	 * This method attempts to apply candidates as variables. The order used
	 * is the order of the variables in the rule.
	 * @param rule
	 * @param candidates
	 * @param pool
	 * @param truths
	 * @return
	 */
	public static Map<Dob, Dob> applyVariables(Rule rule, List<Dob> candidates, 
			Pool pool, Set<Dob> truths) {
		Map<Dob, Dob> unify = Maps.newHashMap();
		List<Dob> vars = rule.vars; 
		List<Atom> body = rule.body;
		
		// Construct replacement
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
