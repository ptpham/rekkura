package rekkura.logic;

import java.util.*;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
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
	
	
	private static class ApplyBodiesResult {
		public final Map<Dob, Dob> unification;
		public final int failurePoint;
		
		public ApplyBodiesResult(Map<Dob, Dob> unification) {
			this.unification = unification;
			this.failurePoint = -1;
		}
		
		public ApplyBodiesResult(int failurePoint) {
			this.unification = null;
			this.failurePoint = failurePoint;
		}
		
		public boolean wasSuccessful() { return this.unification != null; }
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
	private static ApplyBodiesResult applyBodies(Rule rule, List<Dob> candidates, 
			Pool pool, Set<Dob> truths) {
		
		Map<Dob, Dob> unify = Maps.newHashMap();
		List<Dob> vars = rule.vars;
		List<Atom> body = rule.body;
		
		Integer failurePoint = null;
		for (int i = 0; i < body.size() && failurePoint == null; i++) {
			Atom atom = body.get(i);

			// If the atom must be true, use the possibility provided
			// in the full assignment.
			Dob base = atom.dob;
			if (atom.truth) {
				Dob target = candidates.get(i);
				Map<Dob, Dob> unifyResult = Unifier.unifyAssignment(base, target, unify);
				if (unifyResult == null || !vars.containsAll(unify.keySet())) failurePoint = i;
			// If the atom must be false, check that the state 
			// substitution applied to the dob does not yield 
			// something that is true.
			} else {
				Dob generated = pool.submerge(Unifier.replace(base, unify));
				if (truths.contains(generated)) failurePoint = i;
			}
		}
		
		if (failurePoint != null) return new ApplyBodiesResult(failurePoint);
		return new ApplyBodiesResult(unify);
	}
	
	public static Set<Dob> applyBodyExpansion(Rule rule, final ListMultimap<Atom, Dob> support, 
			Pool pool, Set<Dob> truths) {
		Set<Dob> result = Sets.newHashSet();
		
		// Sort the dimensions of the space so that the smallest ones come first.
		List<Atom> positives = rule.getPositives();
		Collections.sort(positives, new Comparator<Atom>() {
			@Override public int compare(Atom first, Atom second) {
				return support.get(first).size() - support.get(second).size();
			}
		});
		
		// Construct a reordered rule for our use.
		List<Atom> negatives = rule.getNegatives();
		Rule reordered = new Rule(rule);
		reordered.body.clear();
		reordered.body.addAll(positives);
		reordered.body.addAll(negatives);
		
		// Construct the space
		List<List<Dob>> space = Lists.newArrayList();
		for (Atom atom : positives) { space.add(support.get(atom)); }
		for (int i = 0; i < negatives.size(); i++) { 
			space.add(Lists.<Dob>newArrayList((Dob)null)); 
		}

		Cartesian.AdvancingIterator<Dob> iterator = Cartesian.asIterator(space);
		while (iterator.hasNext()) {
			List<Dob> assignment = iterator.next();
			ApplyBodiesResult application = Terra.applyBodies(reordered, assignment, pool, truths);
			
			// If we manage to unify against all bodies, apply the substitution
			// to the head and render it. If the generated head still has variables
			// in it, then do not add it to the result.
			Map<Dob, Dob> unify = application.unification;
			if (application.wasSuccessful() && rule.vars.size() == unify.size() 
					&& rule.evaluateDistinct(unify)) {
				Dob generated = pool.submerge(Unifier.replace(rule.head.dob, unify));
				result.add(generated);
			} else iterator.advance(application.failurePoint);
		}
		
		return result;
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
