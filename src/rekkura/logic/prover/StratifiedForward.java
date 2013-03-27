package rekkura.logic.prover;

import java.util.*;

import rekkura.logic.Pool;
import rekkura.logic.Ruletta;
import rekkura.logic.Unifier;
import rekkura.logic.perf.Cachet;
import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Logimos.BodyAssignment;
import rekkura.model.Logimos.DobSpace;
import rekkura.model.Rule;
import rekkura.util.Cache;
import rekkura.util.Cartesian;
import rekkura.util.Colut;
import rekkura.util.NestedIterable;
import rekkura.util.OTMUtil;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;

/**
 * The set of rules provided to this prover must satisfy the following:
 * - No negative heads
 * - Stratified negation: From a rule R and its descendants, it must 
 * not be possible to generate a grounded dob that unifies with a 
 * negative term in the body of R.
 * - Safety: Every variable that appears in a negative term must appear
 * in a positive body term.
 * @author ptpham
 *
 */
public class StratifiedForward {

	public final Ruletta rta;
	public final Cachet cachet;
	
	public final Pool pool;
	
	public int variableSpaceMin = DEFAULT_VARIABLE_SPACE_MIN;

	/**
	 * These hold the mappings from a body form B to grounds 
	 * that are known to successfully unify with B.
	 * Memory is O(FG) but it will only store the things that
	 * are true in any given proving cycle.
	 */
	protected Multimap<Dob, Dob> unisuccess;
	
	/**
	 * This holds the mapping from a body form to the sets of replacements
	 * for its various children.
	 * Memory is O(FV).
	 */
	protected Cache<Dob, DobSpace> unispaces = 
		Cache.create(new Function<Dob, DobSpace>() {
			@Override public DobSpace apply(Dob dob) { return new DobSpace(dob); }
		});

	
	private Multimap<Integer, Rule> pendingRules 
		= TreeMultimap.create(Ordering.natural(), Ordering.arbitrary());
	
	private Set<Dob> truths = Sets.newHashSet();
	
	private static final int DEFAULT_VARIABLE_SPACE_MIN = 512;
	
	/**
	 * This dob is used as a trigger for fully grounded rules
	 * that are entirely negative.
	 */
	private Dob vacuous = new Dob("[VACUOUS]");
	
	public StratifiedForward(Collection<Rule> rules) {
		Set<Rule> submerged = Sets.newHashSet();
		this.pool = new Pool();
		
		for (Rule rule : rules) { submerged.add(pool.submerge(rule)); }
		submerged = preprocess(submerged);
		
		this.rta = Ruletta.create(submerged, pool);
		for (Rule rule : rta.allRules) {
			Preconditions.checkArgument(rule.head.truth, "Rules must have positive heads!");
		}
		
		this.cachet = new Cachet(rta);
		this.unisuccess = HashMultimap.create();
		clear();
	}
	
	/**
	 * This method adds a vacuous positive term to rules that 
	 * have bodies that are entirely negative and grounded.
	 * It will also reorder rules so that negative terms come last.
	 * 
	 * This method will not ruin a submersion.
	 * @param rules
	 * @return
	 */
	private Set<Rule> preprocess(Collection<Rule> rules) {
		Set<Rule> result = Sets.newHashSet();
		
		for (Rule rule : rules) {
			List<Atom> positives = rule.getPositives();
			List<Atom> negatives = rule.getNegatives();
			
			boolean grounded = true;
			for (Atom atom : negatives) {
				if (!rule.isGrounded(atom.dob)) grounded = false;
			}
			
			if (grounded && positives.size() == 0) {
				positives.add(new Atom(this.vacuous, true));
			}
			
			rule.body.clear();
			rule.body.addAll(positives);
			rule.body.addAll(negatives);
			
			result.add(rule);
		}
		
		return result;
	}

	public void reset(Iterable<Dob> truths) {
		clear();
		this.queueTruth(vacuous);
		for (Dob truth : truths) if (truth != null) queueTruth(truth);
	}
	
	public Set<Dob> proveAll(Iterable<Dob> truths) {
		this.reset(truths);
		Set<Dob> result = Sets.newHashSet();
		while (this.hasMore()) result.addAll(this.proveNext());
		return result;
	}
	
	/**
	 * Initialize private variables that track the state of the prover
	 */
	public void clear() {
		this.truths.clear();
		this.pendingRules.clear();
		this.unisuccess.clear();
	}

	/**
	 * Add a dob that is true. The dob will be stored (attached to the last
	 * node on its unify trunk of the fortre) and potential assignments for
	 * the dob will be generated privately.
	 * 
	 * If a truth is queued by the user after the prover starts proving,
	 * there is no longer a guarantee of correctness if rules have negative
	 * body terms.
	 * @param dob
	 */
	protected Dob queueTruth(Dob dob) {
		dob = this.pool.submerge(dob);
		if (isTrue(dob)) return this.vacuous;
		
		Iterable<Rule> generated = this.cachet.affectedRules.get(dob);
		
		// Split the rules into pendingRules vs waitingRules.
		// An assignment is pendingRules if it is ready to be expanded.
		// An assignment is waitingRules if it's rule is being blocked by a non-zero 
		// number of pendingRules/waitingRules ancestors.
		for (Rule rule : generated) {
			int priority = this.rta.ruleOrder.count(rule);
			this.pendingRules.put(priority, rule);
		}
		
		storeGround(dob);

		return dob;
	}

	/**
	 * This method makes sure that the given dob is indexable from the 
	 * last node in the trunk of the fortre.
	 * @param dob
	 * @return
	 */
	private void storeGround(Dob dob) {
		truths.add(dob);

		// The root of the subtree is the end of the trunk.
		Dob end = this.cachet.canonicalForms.get(dob);
		if (end != null) {
			unisuccess.put(end, dob);
			storeVariableReplacements(dob, end);
		}
	}

	private void storeVariableReplacements(Dob ground, Dob body) {
		Map<Dob, Dob> unify = Unifier.unify(body, ground);
		DobSpace space = this.unispaces.get(body);
		OTMUtil.putAll(space.replacements, unify);
	}

	public boolean hasMore() { return this.pendingRules.size() > 0; }
	
	public List<Dob> proveNext() {
		if (!hasMore()) throw new NoSuchElementException();
		Rule rule = Colut.popAny(this.pendingRules.values());	
		Set<Dob> generated = expandRule(rule);
		
		// Submerge all of the newly generated dobs
		List<Dob> result = Lists.newArrayListWithCapacity(generated.size());
		for (Dob dob : generated) {
			Dob submerged =queueTruth(dob);
			if (submerged != vacuous) result.add(submerged);
		}
		
		return result;
	}

	public Set<Dob> expandRule(Rule rule) {		
		Set<Dob> result = Sets.newHashSet();
		
		// Prepare the domains of each positive body in the rule
		List<Iterable<Dob>> assignments = getBodySpace(rule);
		int bodySpaceSize = Cartesian.size(assignments);
		
		// Decide whether to expand by terms or by variables based on the relative
		// sizes of the replacements. This test is only triggered for a sufficiently 
		// large body size because it costs more time to generate the variable space.
		boolean useVariables = (variableSpaceMin <= 0);
		if (useVariables || bodySpaceSize > variableSpaceMin) {
			List<Iterable<Dob>> variables = getVariableSpace(rule);
			useVariables |= bodySpaceSize > Cartesian.size(variables);
			if (useVariables) assignments = variables;
		}
		
		// Iterate through the Cartesian product of possibilities
		Map<Dob, Dob> unify = Maps.newHashMap();
		
		List<List<Dob>> space = Lists.newArrayListWithCapacity(assignments.size());
		for (Iterable<Dob> iterable : assignments) { space.add(Lists.newArrayList(iterable)); }
		
		for (List<Dob> assignment : Cartesian.asIterable(space)) {
			Map<Dob, Dob> success = null;

			if (!useVariables) success = expandAsBodies(rule, assignment);
			else success = expandAsVariables(rule, assignment);
			
			// If we manage to unify against all bodies, apply the substitution
			// to the head and render it. If the generated head still has variables
			// in it, then do not add it to the result.
			if (success != null && rule.vars.size() == success.size()) {
				Dob generated = this.pool.submerge(Unifier.replace(rule.head.dob, success));
				result.add(generated);
			}
			
			unify.clear();
		}
				
		return result;
	}
	
	protected Map<Dob, Dob> expandAsVariables(Rule rule, List<Dob> candidates) {
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
			Dob ground = this.pool.submerge(Unifier.replace(atom.dob, unify));
			boolean truth = this.isTrue(ground);
			if (truth != atom.truth) success = false;
		}
		
		if (!success) return null;
		return unify;
	}

	private Map<Dob, Dob> expandAsBodies(Rule rule, List<Dob> candidates) {
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
			} else if (!vars.containsAll(unify.keySet())) { 
				success = false;
			} else { 
				Dob generated = this.pool.submerge(Unifier.replace(base, unify));
				if (isTrue(generated)) success = false;
			}
		}
		
		if (!success) return null;
		return unify;
	}

	public boolean isTrue(Dob dob) { return this.truths.contains(dob); }
	
	/**
	 * Returns a list that contains the assignment domain of each body 
	 * term in the given rule assuming that we want to expand the given
	 * dob at the given position.
	 * @param rule
	 * @param position
	 * @param dob
	 * @return
	 */
	private List<Iterable<Dob>> getBodySpace(Rule rule) {
		List<Iterable<Dob>> candidates = Lists.newArrayList(); 
		
		for (int i = 0; i < rule.body.size(); i++) {
			Atom atom = rule.body.get(i);
			
			Iterable<Dob> next;
			if (!atom.truth) next = Lists.newArrayList((Dob)null);
			else next = getGroundCandidates(atom.dob);
			
			if (Iterables.isEmpty(next)) return Lists.newArrayList();
			candidates.add(next);
		}
		return candidates;
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
	protected List<Iterable<Dob>> getVariableSpace(Rule rule) {
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
			Iterable<Dob> subtree = this.cachet.spines.get(atom.dob);
			for (Dob node : subtree) {
				Map<Dob, Collection<Dob>> raw = this.unispaces.get(node).replacements.asMap();
				Map<Dob, Dob> left = Unifier.unify(atom.dob, node);
				Map<Dob, Collection<Dob>> replacements = raw;
				if (Colut.nonEmpty(left.keySet())) {
					replacements = OTMUtil.flatten(OTMUtil.joinRight(left, raw)).asMap();
				}
				
				for (Dob variable : replacements.keySet()) {
					Collection<Dob> current = replacements.get(variable);
					if (variables.containsKey(variable)) {
						variables.get(variable).retainAll(current);
					} else {
						variables.putAll(variable, current);
					}
				}
				
				// Add stuff that was not included in the join but is 
				// still necessary for a valid unification.
				for (Map.Entry<Dob, Dob> entry: left.entrySet()) {
					if (!this.rta.fortre.allVars.contains(entry.getValue()))
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
	protected Iterable<Dob> getGroundCandidates(Dob dob) {
		Iterable<Dob> subtree = this.cachet.spines.get(dob);
		return new NestedIterable<Dob, Dob>(subtree) {
			@Override protected Iterator<Dob> prepareNext(Dob u) {
				return StratifiedForward.this.unisuccess.get(u).iterator();
			}
		};
	}
	
	public static List<BodyAssignment> generateAssignments(Rule rule, Set<Dob> forces, Dob ground) {
		List<BodyAssignment> result = Lists.newArrayList();
		for (int i = 0; i < rule.body.size(); i++) {
			Atom atom = rule.body.get(i);
			if (!atom.truth) continue;
			
			Dob body = atom.dob;
			if (forces.contains(body)) { result.add(new BodyAssignment(ground, i, rule)); }
		}
		return result;
	}
	
	public static Iterator<List<Dob>> asIterator(final StratifiedForward prover) {
		return new Iterator<List<Dob>>() {
			@Override public boolean hasNext() { return prover.hasMore(); }
			@Override public List<Dob> next() { return prover.proveNext(); }
			@Override public void remove() { throw new IllegalAccessError("Remove not allowed!"); }
		};
	}
}
