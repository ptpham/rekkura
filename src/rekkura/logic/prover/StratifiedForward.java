package rekkura.logic.prover;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import rekkura.logic.Fortre;
import rekkura.logic.Pool;
import rekkura.logic.Ruletta;
import rekkura.logic.Topper;
import rekkura.logic.Unifier;
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
	
	private static final int VARIABLE_SPACE_MIN = 512;

	public Ruletta rta;
	
	public Topper topper;
	
	/**
	 * This holds the set of head dobs that may generate a body dob.
	 * Memory is O(R^2).
	 */
	public Multimap<Dob, Dob> headBodyDeps;
	
	/**
	 * This holds the set of rules that may generate a body dob.
	 * Memory is O(R^2).
	 */
	public Multimap<Dob, Rule> dobRuleDeps;

	/**
	 * This holds the set of rules that may generate dobs for the body
	 * of the given rule.
	 * Memory is O(R^2).
	 */
	public Multimap<Rule, Rule> ruleRuleDeps;
	
	/**
	 * This holds the set of rules that are descendants of the given
	 * rule such that the given rule may generate a negative body 
	 * in the descendant.
	 * Memory is O(R^2).
	 */
	public Multimap<Rule, Rule> ruleNegDesc;

	public final Pool pool;
	
	public final Fortre fortre;
	
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
	protected Map<Dob, DobSpace> unispaces;
	
	/**
	 * This maps ground dobs to their canonical dobs (the dob at 
	 * the end of the unify trunk).
	 */
	protected Cache<Dob, Dob> canonicalForms = 
		Cache.create(new Function<Dob, Dob>() {
			@Override public Dob apply(Dob dob) { 
				return Colut.end(StratifiedForward.this.fortre.getUnifyTrunk(dob));
			}
		});
	
	protected Cache<Dob, List<Dob>> canonicalSubtrees = 
		Cache.create(new Function<Dob, List<Dob>>() {
			@Override public List<Dob> apply(Dob dob) { 
				return Lists.newArrayList(StratifiedForward.this.fortre.getUnifySubtree(dob));
			}
		});
	
	/**
	 * This caches form subtrees for given canonical dobs.
	 */
	protected Cache<Dob, List<Dob>> subtrees = 
		Cache.create(new Function<Dob, List<Dob>>() {
			@Override public List<Dob> apply(Dob dob) 
			{ return canonicalSubtrees.get(canonicalForms.get(dob)); }
		});
	
	/**
	 * This caches the list of rules affected by each canonical form.
	 */
	protected Cache<Dob, List<Rule>> canonicalRules = 
		Cache.create(new Function<Dob, List<Rule>>() {
			@Override public List<Rule> apply(Dob dob) { 
				List<Rule> result = Lists.newArrayList(StratifiedForward.this.generateAssignments(dob));
				return result;
			}
		});
	
	protected Cache<Dob, List<Rule>> affectedRules = 
		Cache.create(new Function<Dob, List<Rule>>() {
			@Override public List<Rule> apply(Dob dob) 
			{ return canonicalRules.get(canonicalForms.get(dob)); }
		});
	
	private Set<Rule> pendingRules = Sets.newHashSet();
	private List<Rule> waitingRules = new Stack<Rule>();
	private Multiset<Rule> negDepCounter = HashMultiset.create();
	private Set<Dob> truths = Sets.newHashSet();
	private Rule next;
	
	/**
	 * This dob is used as a trigger for fully grounded rules
	 * that are entirely negative.
	 */
	private Dob vacuous = new Dob("[VACUOUS]");
	
	public StratifiedForward(Collection<Rule> rules) {
		
		Set<Rule> submerged = Sets.newHashSet();
		this.pool = new Pool();
		this.rta = new Ruletta();
		this.topper = new Topper();
		
		for (Rule rule : rules) { submerged.add(pool.submerge(rule)); }
		submerged = preprocess(submerged);
		
		rta.construct(submerged);

		for (Rule rule : rta.allRules) {
			Preconditions.checkArgument(rule.head.truth, "Rules must have positive heads!");
		}
		
		this.headBodyDeps = topper.dependencies(rta.bodyToRule.keySet(), 
				rta.headToRule.keySet(), rta.allVars);
		
		this.dobRuleDeps = OTMUtil.joinRight(this.headBodyDeps, this.rta.headToRule);
		this.ruleRuleDeps = OTMUtil.joinLeft(this.dobRuleDeps, this.rta.bodyToRule);
		
		// For each negative dob, flood out from the rules that can generate it.
		// Store a mapping from each of the rules that we saw to the rules that 
		// contain the negative dob.
		this.ruleNegDesc = HashMultimap.create();
		for (Dob neg : this.rta.negDobs) {
			Set<Rule> seen = Sets.newHashSet();
			OTMUtil.flood(this.ruleRuleDeps, this.dobRuleDeps.get(neg), seen);
			
			Collection<Rule> negRules = this.rta.bodyToRule.get(neg);
			for (Rule rule : seen) this.ruleNegDesc.putAll(rule, negRules);
		}
		
		this.fortre = new Fortre(rta.allVars, rta.bodyToRule.keySet());
		
		this.unisuccess = HashMultimap.create();
		this.unispaces = Maps.newHashMap();
		for (Dob body : Iterables.concat(Lists.newArrayList(this.fortre.root), this.rta.getAllTerms())) { 
			this.unispaces.put(body, new DobSpace(body)); 
		}
		
		clear();
	}
	
	public Unifier getUnifier() { return this.fortre.unifier; }
	
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

	public void reset(Collection<Dob> truths) {
		clear();
		this.queueTruth(vacuous);
		for (Dob truth : truths) queueTruth(truth);
	}
	
	/**
	 * Initialize private variables that track the state of the prover
	 */
	public void clear() {
		this.truths.clear();

		this.pendingRules.clear();
		this.waitingRules.clear();
		this.negDepCounter.clear();
		
		this.unisuccess.clear();
		
		this.next = null;
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
		
		Iterable<Rule> generated = this.affectedRules.get(dob);
		
		for (Rule rule : generated) {
			Collection<Rule> negDescs = this.ruleNegDesc.get(rule);
			Colut.shiftAll(negDepCounter, negDescs, 1);
		}

		// Split the rules into pendingRules vs waitingRules.
		// An assignment is pendingRules if it is ready to be expanded.
		// An assignment is waitingRules if it's rule is being blocked by a non-zero 
		// number of pendingRules/waitingRules ancestors.
		for (Rule rule : generated) {
			if (this.negDepCounter.count(rule) > 0) {
				this.waitingRules.add(rule);
			} else {
				this.pendingRules.add(rule);
			}
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
		Dob end = this.canonicalForms.get(dob);
		if (end != null) {
			unisuccess.put(end, dob);
			storeVariableReplacements(dob, end);
		}
	}

	private void storeVariableReplacements(Dob ground, Dob body) {
		Unifier unifier = this.fortre.unifier;
		Map<Dob, Dob> unify = unifier.unify(body, ground);
		DobSpace space = this.unispaces.get(body);
		OTMUtil.putAll(space.replacements, unify);
	}

	public boolean hasMore() { 
		prepareNext();
		return this.next != null;
	}
	
	private void prepareNext() {
		if (next != null) return;
		
		next = nextPending();

		// If the assignment is null here, it means we need to 
		// look in the waiting assignments
		if (next == null) {
			reconsiderWaiting();
			next = nextPending();
		}
	}
	
	public List<Dob> proveNext() {
		if (!hasMore()) throw new NoSuchElementException();
	
		Rule rule = this.next;
		this.next = null;
		
		Set<Dob> generated = expand(rule);
		
		// Inform rules with negative terms that they can proceed
		Collection<Rule> negDescs = this.ruleNegDesc.get(rule);
		Colut.shiftAll(negDepCounter, negDescs, -1);
		
		// Submerge all of the newly generated dobs
		List<Dob> result = Lists.newArrayListWithCapacity(generated.size());
		for (Dob dob : generated) {
			Dob submerged =queueTruth(dob);
			if (submerged != vacuous) result.add(submerged);
		}
		
		return result;
	}

	/**
	 * Find a pendingRules assignment on which we can actually operate.
	 * We can only operate on a rule R that doesn't have any ancestors
	 * that still might generate grounds that potentially
	 * unify with a negative body term in R.
	 * @return
	 */
	private Rule nextPending() {
		Rule assignment = null;
		while (assignment == null && this.pendingRules.size() > 0) {
			assignment = Colut.popAny(this.pendingRules);

			if (!ruleReady(assignment)) {
				this.waitingRules.add(assignment);
				assignment = null;
			}
		}
		return assignment;
	}

	private boolean ruleReady(Rule rule) {
		return this.negDepCounter.count(rule) == 0;
	}
	
	private void reconsiderWaiting() {
		List<Rule> stillWaiting = Lists.newArrayList();
		
		for (Rule rule : this.waitingRules) {
			if (ruleReady(rule)) pendingRules.add(rule);
			else stillWaiting.add(rule);
		}
		
		this.waitingRules = stillWaiting;
	}

	public Set<Dob> expand(Rule rule) {		
		Set<Dob> result = Sets.newHashSet();
		Unifier unifier = getUnifier();

		// Prepare the domains of each positive body in the rule
		List<Iterable<Dob>> assignments = getBodySpace(rule);
		int bodySpaceSize = Cartesian.size(assignments);
		
		// Decide whether to expand by terms or by variables based on the relative
		// sizes of the replacements. This test is only triggered for a sufficiently 
		// large body size because it costs more time to generate the variable space.
		boolean useVariables = false;
		if (bodySpaceSize > VARIABLE_SPACE_MIN) {
			List<Iterable<Dob>> variables = getVariableSpace(rule);
			useVariables = bodySpaceSize > Cartesian.size(variables);
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
				Dob generated = this.pool.submerge(unifier.replace(rule.head.dob, success));
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
		Unifier unifier = getUnifier();
		
		// Construct replacement
		for (int i = 0; i < vars.size(); i++) {
			unify.put(vars.get(i), candidates.get(i));
		}
		
		boolean success = true;
		for (int i = 0; i < body.size() && success; i++) {
			Atom atom = body.get(i);
		
			// Generate the ground body
			Dob ground = this.pool.submerge(unifier.replace(atom.dob, unify));
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
		Unifier unifier = getUnifier();
		
		boolean success = true;
		for (int i = 0; i < body.size() && success; i++) {
			Atom atom = body.get(i);
			
			// If the atom must be true, use the possibility provided
			// in the full assignment.
			Dob base = atom.dob;
			if (atom.truth) {
				Dob target = candidates.get(i);
				Map<Dob, Dob> unifyResult = unifier.unifyAssignment(base, target, unify);
				if (unifyResult == null || !vars.containsAll(unify.keySet())) success = false;
			// If the atom must be false, check that the state 
			// substitution applied to the dob does not yield 
			// something that is true.
			} else if (!vars.containsAll(unify.keySet())) { 
				success = false;
			} else { 
				Dob generated = this.pool.submerge(unifier.replace(base, unify));
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
		
		Unifier unifier = getUnifier();
		Multimap<Dob, Dob> variables = HashMultimap.create();
		
		for (int i = 0; i < rule.body.size(); i++) {
			Atom atom = rule.body.get(i);
			
			// For each node in the subtree, find the set of replacements
			// in terms of the root of the subtree. Then join right
			// to rephrase in terms of variables in the rule.
			Iterable<Dob> subtree = this.subtrees.get(atom.dob);
			for (Dob node : subtree) {
				Map<Dob, Collection<Dob>> raw = this.unispaces.get(node).replacements.asMap();
				Map<Dob, Dob> left = unifier.unify(atom.dob, node);
				
				Map<Dob, Collection<Dob>> replacements = raw;
				if (Colut.nonEmpty(left.keySet())) {
					replacements = Maps.newHashMap();
					for (Entry<Dob, Collection<Dob>> entry : OTMUtil.joinRight(left, raw).entries()) {
						replacements.put(entry.getKey(), entry.getValue());
					}
				}
				
				for (Dob variable : replacements.keySet()) {
					Collection<Dob> current = replacements.get(variable);
					if (variables.containsKey(variable)) {
						variables.get(variable).retainAll(current);
					} else {
						variables.putAll(variable, current);
					}
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
		Iterable<Dob> subtree = this.subtrees.get(dob);
		return new NestedIterable<Dob, Dob>(subtree) {
			@Override protected Iterator<Dob> prepareNext(Dob u) {
				return StratifiedForward.this.unisuccess.get(u).iterator();
			}
		};
	}
	
	/**
	 * This method takes a dob and returns the rules where
	 * it can be applied.
	 * @param dob
	 * @return
	 */
	private Set<Rule> generateAssignments(Dob dob) {
		// Iterate over all of the bodies we are potentially affecting.
		// This set must be a subset of the rules whose bodies are touched by the 
		// subtree of the fortre rooted at the end of the trunk.
		Set<Dob> subtree = Sets.newHashSet();
		Iterables.addAll(subtree, fortre.getCognateSplay(dob));
		return Sets.newHashSet(rta.ruleIterableFromBodyDobs(subtree));
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
