package rekkura.logic;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Colut;
import rekkura.util.NestedIterable;
import rekkura.util.OtmUtil;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * This class holds a collection of utilities for generating
 * and using groundings.
 * @author ptpham
 *
 */
public class Terra {
		/**
	 * Returns a list of the possible assignments to the variables in the
	 * given rule assuming that the given dob must be applied at
	 * the given position.
	 * @param rule
	 * @param position
	 * @param ground
	 * @return
	 */
	public static List<Iterable<Dob>> getVariableSpace(Rule rule, Cachet cachet) {
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
					if (!cachet.rta.fortre.allVars.contains(entry.getValue()))
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
	public static List<Iterable<Dob>> getBodySpace(Rule rule, Cachet cachet) {
		List<Iterable<Dob>> candidates = Lists.newArrayList(); 
		
		for (int i = 0; i < rule.body.size(); i++) {
			Atom atom = rule.body.get(i);
			
			Iterable<Dob> next;
			if (!atom.truth) next = Lists.newArrayList((Dob)null);
			else next = getGroundCandidates(atom.dob, cachet);
			
			if (Iterables.isEmpty(next)) return Lists.newArrayList();
			candidates.add(next);
		}
		return candidates;
	}
	
	/**
	 * This method attempts to apply the candidates in order along the 
	 * body of the rule. If the application is successful, then a unification
	 * for the head of the rule is returned.
	 * @param rule
	 * @param candidates
	 * @param pool
	 * @param truths
	 * @return
	 */
	public static Map<Dob, Dob> applyBodies(Rule rule, List<Dob> candidates, 
			Pool pool, Set<Dob> truths) {
		
		Map<Dob, Dob> unify = Maps.newHashMap();
		List<Dob> vars = rule.vars;
		List<Atom> body = rule.body;
		
		boolean success = true;
		for (int i = 0; i < body.size() && success; i++) {
			Atom atom = body.get(i);

			// If the atom must be true, use the possibility provided
			// in the full assignment.
			Dob base = atom.dob;
			if (atom.truth) {
				Dob target = candidates.get(i);
				Map<Dob, Dob> unifyResult = Unifier.unifyAssignment(base, target, unify);
				if (unifyResult == null || !vars.containsAll(unify.keySet())) success = false;
			// If the atom must be false, check that the state 
			// substitution applied to the dob does not yield 
			// something that is true.
			} else {
				Dob generated = pool.submerge(Unifier.replace(base, unify));
				if (truths.contains(generated)) success = false;
			}
		}
		
		if (!success) return null;
		return unify;
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
			Dob ground = pool.submerge(Unifier.replace(atom.dob, unify));
			boolean truth = truths.contains(ground);
			if (truth != atom.truth) success = false;
		}
		
		if (!success) return null;
		return unify;
	}
}
