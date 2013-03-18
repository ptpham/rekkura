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
	
	private static final int VARIABLE_SPACE_MIN = 32;

	public Ruletta rta;
	
	public Topper topper;
	
	/**
	 * This holds the set of head dobs that may generate a body dob.
	 */
	public Multimap<Dob, Dob> headBodyDeps;
	
	/**
	 * This holds the set of rules that may generate a body dob.
	 */
	public Multimap<Dob, Rule> dobRuleDeps;

	/**
	 * This holds the set of rules that may generate dobs for the body
	 * of the given rule.
	 */
	public Multimap<Rule, Rule> ruleRuleDeps;
	
	/**
	 * This holds the set of rules that are descendants of the given
	 * rule such that the given rule may generate a negative body 
	 * in the descendant.
	 */
	public Multimap<Rule, Rule> ruleNegDesc;

	public final Pool pool;
	
	public final Fortre fortre;
	
	/**
	 * These hold the mappings from a body term B in a rule to grounds 
	 * that are known to successfully unify with B.
	 */
	protected Multimap<Dob, Dob> unisuccess;
	
	/**
	 * This holds the mapping from a body term to the sets of replacements
	 * for its various children.
	 */
	protected Map<Dob, DobSpace> unispaces;
	
	protected Cache<Dob, List<Dob>> trunks = 
		Cache.create(new Function<Dob, List<Dob>>() {
			@Override public List<Dob> apply(Dob dob) { 
				return StratifiedForward.this.fortre.getUnifyTrunk(dob);
			}
		});
	
	private Stack<BodyAssignment> pendingAssignments = new Stack<BodyAssignment>();
	private List<BodyAssignment> waitingAssignments = new Stack<BodyAssignment>();
	private Multiset<Rule> negDepCounter = HashMultiset.create();
	private Set<Dob> pendingTruths = Sets.newHashSet();
	private Set<Dob> exhaustedTruths = Sets.newHashSet();

	/**
	 * When this counter gets to 0, a pending truth becomes exhausted.
	 */
	private Multiset<Dob> dobAssignmentCounter = HashMultiset.create();
	
	private BodyAssignment next;
	
	/**
	 * This dob is used as a trigger for fully grounded rules
	 * that are entirely negative.
	 */
	private Dob vacuous = new Dob("[VAC_TRUE]");
	
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
		
		this.fortre = new Fortre(rta.allVars, rta.allDobs);
		
		this.unisuccess = HashMultimap.create();
		this.unispaces = Maps.newHashMap();
		for (Dob body : this.rta.allDobs) { 
			this.unispaces.put(body, new DobSpace(body)); 
		}

		// TODO: This is kind of gross. Make sure it is reasonable before fully committing.
		this.unispaces.put(this.fortre.root, new DobSpace(this.fortre.root));
		
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
		this.exhaustedTruths.clear();
		this.pendingTruths.clear();

		this.pendingAssignments.clear();
		this.waitingAssignments.clear();
		
		this.negDepCounter.clear();
		this.dobAssignmentCounter.clear();
		
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
		
		Multimap<Rule, BodyAssignment> generated = this.generateAssignments(dob);
		
		for (Rule rule : generated.keySet()) {
			int increase = generated.get(rule).size();
			Collection<Rule> negDescs = this.ruleNegDesc.get(rule);
			Colut.shiftAll(negDepCounter, negDescs, increase);
		}

		// Split the rules into pendingAssignments vs waitingAssignments.
		// An assignment is pendingAssignments if it is ready to be expanded.
		// An assignment is waitingAssignments if it's rule is being blocked by a non-zero 
		// number of pendingAssignments/waitingAssignments ancestors.
		for (Rule rule : generated.keySet()) {
			Collection<BodyAssignment> assignments = generated.get(rule);
			if (this.negDepCounter.count(rule) > 0) {
				this.waitingAssignments.addAll(assignments);
			} else {
				this.pendingAssignments.addAll(assignments);
			}
		}
		
		for (BodyAssignment assignment : generated.values()) {
			this.dobAssignmentCounter.add(assignment.ground);
		}
		
		if (generated.size() > 0) this.pendingTruths.add(dob);
		else exhaustGround(dob);
		
		return dob;
	}

	/**
	 * This method makes sure that the given dob is indexable from the 
	 * last node in the trunk of the fortre.
	 * @param dob
	 * @return
	 */
	private List<Dob> storeGround(Dob dob) {
		List<Dob> trunk = this.trunks.get(dob);
		Dob end = Colut.end(trunk);
		if (!unisuccess.containsEntry(end, dob)) {
			unisuccess.put(end, dob);
		}
		storeVariableReplacements(dob, end);
		return trunk;
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
	
		BodyAssignment assignment = this.next;
		this.next = null;
		
		Set<Dob> generated = expand(assignment);
		
		// Exhaust the dob for this assignment if appropriate
		Dob ground = assignment.ground;
		this.dobAssignmentCounter.remove(ground);
		if (this.dobAssignmentCounter.count(ground) == 0) exhaustGround(ground);

		// Inform rules with negative terms that they can proceed
		Collection<Rule> negDescs = this.ruleNegDesc.get(assignment.rule);
		Colut.shiftAll(negDepCounter, negDescs, -1);
		
		// Submerge all of the newly generated dobs
		List<Dob> result = Lists.newArrayListWithCapacity(generated.size());
		for (Dob dob : generated) {
			Dob submerged =queueTruth(dob);
			if (submerged != vacuous) result.add(submerged);
		}
		
		return result;
	}

	private void exhaustGround(Dob ground) {
		exhaustedTruths.add(ground);
		storeGround(ground);
	}

	/**
	 * Find a pendingAssignments assignment on which we can actually operate.
	 * We can only operate on a rule R that doesn't have any ancestors
	 * that still might generate grounds that potentially
	 * unify with a negative body term in R.
	 * @return
	 */
	private BodyAssignment nextPending() {
		BodyAssignment assignment = null;
		while (assignment == null && !this.pendingAssignments.empty()) {
			assignment = this.pendingAssignments.pop();

			if (!assignmentReady(assignment)) {
				this.waitingAssignments.add(assignment);
				assignment = null;
			}
		}
		return assignment;
	}

	private boolean assignmentReady(BodyAssignment assignment) {
		return this.negDepCounter.count(assignment.rule) == 0;
	}
	
	private void reconsiderWaiting() {
		List<BodyAssignment> stillWaiting = Lists.newArrayList();
		
		for (BodyAssignment assignment : this.waitingAssignments) {
			if (assignmentReady(assignment)) pendingAssignments.add(assignment);
			else stillWaiting.add(assignment);
		}
		
		this.waitingAssignments = stillWaiting;
	}

	public Set<Dob> expand(BodyAssignment grounding) {
		Dob dob = grounding.ground;
		Rule rule = grounding.rule;
		int position = grounding.position;
		
		Set<Dob> result = Sets.newHashSet();
		Unifier unifier = getUnifier();

		// Prepare the domains of each positive body in the rule
		List<Iterable<Dob>> assignments = getBodySpace(rule, position, dob);
		int bodySpaceSize = Cartesian.size(assignments);
		
		// Decide whether to expand by terms or by variables based on the relative
		// sizes of the replacements. This test is only triggered for a sufficiently 
		// large body size because it costs more time to generate the variable space.
		boolean useVariables = false;
		if (bodySpaceSize > VARIABLE_SPACE_MIN) {
			List<Iterable<Dob>> variables = getVariableSpace(rule, position, dob);
			useVariables = bodySpaceSize > Cartesian.size(variables);
			if (useVariables) assignments = variables;
		}
		
		// Initialize the unify with the unification we are applying 
		// at the given position.
		Map<Dob, Dob> reference = unifier.unify(rule.body.get(position).dob, dob);
		Map<Dob, Dob> unify = Maps.newHashMap(reference);
		List<Dob> vars = rule.vars;
		
		// Iterate through the Cartesian product of possibilities
		for (List<Dob> assignment : Cartesian.asIterable(assignments)) {
			Map<Dob, Dob> success = null;
			if (!useVariables) success = expandAsBodies(grounding, unify, assignment);
			else success = expandAsVariables(grounding, unify, assignment);
			
			// If we manage to unify against all bodies, apply the substitution
			// to the head and render it. If the generated head still has variables
			// in it, then do not add it to the result.
			if (success != null) {
				Dob generated = this.pool.submerge(unifier.replace(rule.head.dob, success));
				if (Colut.containsNone(generated.fullIterable(), vars)) {
					result.add(generated);
				}
			}
			
			unify.clear();
			unify.putAll(reference);
		}
		
		return result;
	}
	
	protected Map<Dob, Dob> expandAsVariables(BodyAssignment grounding, 
			Map<Dob, Dob> unify, List<Dob> candidates) {
		Rule rule = grounding.rule;
		List<Dob> vars = rule.vars; 
		List<Atom> body = rule.body;
		Unifier unifier = getUnifier();
		
		// Construct replacement
		for (int i = 0; i < vars.size(); i++) {
			unify.put(vars.get(i), candidates.get(i));
		}
		
		boolean success = true;
		for (int i = 0; i < body.size() && success; i++) {
			if (i == grounding.position) continue;
			Atom atom = body.get(i);
		
			// Generate the ground body
			Dob ground = this.pool.submerge(unifier.replace(atom.dob, unify));
			boolean truth = this.isTrue(ground);
			if (truth != atom.truth) success = false;
		}
		
		if (!success) return null;
		return unify;
	}

	private Map<Dob, Dob> expandAsBodies(BodyAssignment grounding, 
			Map<Dob, Dob> unify, List<Dob> candidates) {
		Rule rule = grounding.rule;
		List<Dob> vars = rule.vars;
		List<Atom> body = rule.body;
		Unifier unifier = getUnifier();
		
		boolean success = true;
		for (int i = 0; i < body.size() && success; i++) {
			if (i == grounding.position) continue;
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

	public boolean isTrue(Dob dob) {
		return this.exhaustedTruths.contains(dob) || this.pendingTruths.contains(dob);
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
	private List<Iterable<Dob>> getBodySpace(Rule rule, int position, Dob dob) {
		List<Iterable<Dob>> candidates = Lists.newArrayList(); 
		List<Dob> targetTrunk = this.trunks.get(dob);
		List<Dob> targetList = Lists.newArrayList(dob);
		
		for (int i = 0; i < rule.body.size(); i++) {
			Atom atom = rule.body.get(i);
			
			Iterable<Dob> next;
			if (!atom.truth) next = Lists.newArrayList((Dob)null);
			else if (i == position) next = Lists.newArrayList(dob);
			else {
				List<Dob> currentTrunk = this.trunks.get(atom.dob);
				next = getGroundCandidates(currentTrunk);
				if (targetTrunk.contains(Colut.end(currentTrunk))) {
					next = Iterables.concat(next, targetList);
				}
			}
			
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
	protected List<Iterable<Dob>> getVariableSpace(Rule rule, int position, Dob ground) {
		// Add a single null for rules with no variables
		List<Iterable<Dob>> candidates = Lists.newArrayList();
		if (rule.vars.size() == 0) {
			candidates.add(Lists.newArrayList((Dob)null));
			return candidates;
		}
		
		Unifier unifier = getUnifier();

		Multimap<Dob, Dob> variables = HashMultimap.create();
		Map<Dob, Dob> forced = unifier.unify(rule.body.get(position).dob, ground);
		OTMUtil.putAll(variables, forced);
		
		for (int i = 0; i < rule.body.size(); i++) {
			if (i == position) continue;

			Atom atom = rule.body.get(i);
			List<Dob> trunk = this.trunks.get(atom.dob);
			
			// For each node in the subtree, find the set of replacements
			// in terms of the root of the subtree. Then join right
			// to rephrase in terms of variables in the rule.
			Iterable<Dob> subtree = this.fortre.getCognateSubtree(trunk);
			for (Dob node : subtree) {
				Map<Dob, Collection<Dob>> raw = this.unispaces.get(node).replacements.asMap();
				Map<Dob, Dob> left = unifier.unify(atom.dob, node);
				
				Map<Dob, Collection<Dob>> replacements = raw;
				if (Colut.nonEmpty(left.keySet())) {
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
	protected Iterable<Dob> getGroundCandidates(List<Dob> trunk) {
		Iterable<Dob> subtree = this.fortre.getCognateSubtree(trunk);
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
	private Multimap<Rule, BodyAssignment> generateAssignments(Dob dob) {
		Multimap<Rule, BodyAssignment> result = ArrayListMultimap.create();

		// Iterate over all of the bodies we are potentially affecting.
		// This set must be a subset of the rules whose bodies are touched by the 
		// subtree of the fortre rooted at the end of the trunk.
		Set<Dob> subtree = Sets.newHashSet();
		List<Dob> trunk = this.trunks.get(dob);
		Iterables.addAll(subtree, fortre.getCognateSplay(trunk));
		Iterable<Rule> rules = rta.ruleIterableFromBodyDobs(subtree);
		for (Rule rule : rules) {
			List<BodyAssignment> assignments = generateAssignments(rule, subtree, dob);
			result.putAll(rule, assignments);
		}
		
		return result;
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
