package rekkura.logic.prover;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import rekkura.model.Rule;
import rekkura.model.Rule.Assignment;
import rekkura.util.Cartesian;
import rekkura.util.Colut;
import rekkura.util.NestedIterable;
import rekkura.util.OTMUtil;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

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

	public Pool pool;
	
	public Fortre fortre;
	
	/**
	 * These hold the mappings from a body term B in a rule to grounds 
	 * that are known to successfully unify with B.
	 */
	protected Multimap<Dob, Dob> unisuccess;
	
	private Stack<Assignment> pendingAssignments;
	private List<Assignment> waitingAssignments;
	private Multiset<Rule> negDepCounter;
	private Set<Dob> pendingTruths, exhaustedTruths;
	private Assignment next;
	
	/**
	 * This dob is used as a trigger for fully grounded rules
	 * that are entirely negative.
	 */
	private Dob vacuous = new Dob("[VAC_TRUE]");
	
	/**
	 * When this counter gets to 0, a pending truth becomes exhausted.
	 */
	private Multiset<Dob> dobAssignmentCounter;
	
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
		
		this.fortre = new Fortre(rta.allVars);
		
		for (Dob dob : rta.bodyToRule.keySet()) { this.fortre.addDob(dob); }
		
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

	public void reset(Collection<Dob> truths) {
		clear();
		this.queueTruth(vacuous);
		for (Dob truth : truths) queueTruth(truth);
	}
	
	/**
	 * Initialize private variables that track the state of the prover
	 */
	public void clear() {
		this.exhaustedTruths = Sets.newHashSet();
		this.pendingTruths = Sets.newHashSet();

		this.pendingAssignments = new Stack<Assignment>();
		this.waitingAssignments = new Stack<Assignment>();
		
		this.negDepCounter = HashMultiset.create();
		this.dobAssignmentCounter = HashMultiset.create();
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
	protected void queueTruth(Dob dob) {
		dob = this.pool.submerge(dob);
		if (pendingTruths.contains(dob) || exhaustedTruths.contains(dob)) return;
		
		Multimap<Rule, Assignment> generated = this.generateAssignments(dob);
		
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
			Collection<Assignment> assignments = generated.get(rule);
			if (this.negDepCounter.count(rule) > 0) {
				this.waitingAssignments.addAll(assignments);
			} else {
				this.pendingAssignments.addAll(assignments);
			}
		}
		
		for (Assignment assignment : generated.values()) {
			this.dobAssignmentCounter.add(assignment.ground);
		}
		
		this.pendingTruths.add(dob);
	}

	/**
	 * This method makes sure that the given dob is indexable from the 
	 * last node in the trunk of the fortre.
	 * @param dob
	 * @return
	 */
	private List<Dob> storeGround(Dob dob) {
		List<Dob> trunk = this.fortre.getUnifyTrunk(dob);
		Dob end = Colut.end(trunk);
		if (!unisuccess.containsEntry(end, dob)) {
			unisuccess.put(end, dob);
		}
		return trunk;
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
	
	public Set<Dob> proveNext() {
		if (!hasMore()) throw new NoSuchElementException();
	
		Assignment assignment = this.next;
		this.next = null;
		
		Set<Dob> generated = expand(assignment.rule, assignment.position, assignment.ground);
		
		// Exhaust the dob for this assignment if appropriate
		Dob ground = assignment.ground;
		this.dobAssignmentCounter.remove(ground);
		if (this.dobAssignmentCounter.count(ground) == 0) exhaustGround(ground);
		
		// Submerge all of the newly generated dobs
		Set<Dob> result = Sets.newHashSetWithExpectedSize(generated.size());
		result.addAll(this.pool.submergeDobs(generated));
		for (Dob dob : result) queueTruth(dob);
		result.remove(vacuous);
		
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
	private Assignment nextPending() {
		Assignment assignment = null;
		while (assignment == null && !this.pendingAssignments.empty()) {
			assignment = this.pendingAssignments.pop();

			if (assignmentReady(assignment)) {
				this.waitingAssignments.add(assignment);
				assignment = null;
			}
		}
		return assignment;
	}

	private boolean assignmentReady(Assignment assignment) {
		return this.negDepCounter.count(assignment.rule) > 0;
	}
	
	private void reconsiderWaiting() {
		List<Assignment> stillWaiting = Lists.newArrayList();
		
		for (Assignment assignment : this.waitingAssignments) {
			if (assignmentReady(assignment)) pendingAssignments.add(assignment);
			else stillWaiting.add(assignment);
		}
		
		this.waitingAssignments = stillWaiting;
	}

	public Set<Dob> expand(Rule rule, int position, Dob dob) {
		Set<Dob> result = Sets.newHashSet();
		Unifier unifier = this.topper.unifier;
		int bodySize = rule.body.size();
		
		// Prepare the domains of each positive body in the rule
		List<Iterable<Dob>> candidates = getAssignmentSpace(rule, position, dob);
		
		// Initialize the unify with the unification we are applying 
		// at the given position.
		Map<Dob, Dob> reference = unifier.unify(rule.body.get(position).dob, dob);
		Map<Dob, Dob> unify = Maps.newHashMap(reference);
		Set<Dob> vars = rule.vars;
		
		// Iterate through the Cartesian product of possibilities
		for (List<Dob> assignment : Cartesian.asIterable(candidates)) {
			boolean success = true;
			for (int i = 0; i < bodySize && success; i++) {
				if (i == position) continue;
				Atom atom = rule.body.get(i);
				
				// If the atom must be true, use the possibility provided
				// in the full assignment.
				Dob base = atom.dob;
				if (atom.truth) {
					Dob target = assignment.get(i);
					Map<Dob, Dob> unifyResult = unifier.unifyAssignment(base, target, unify);
					if (unifyResult == null || !vars.containsAll(unify.keySet())) success = false;
				// If the atom must be false, check that the state 
				// substitution applied to the dob does not yield 
				// something that is true.
				} else if (!vars.containsAll(unify.keySet())) { 
					success = false;
				} else { 
					Dob generated = this.pool.submerge(unifier.replace(base, unify));
					if (this.exhaustedTruths.contains(generated)) success = false;
				}
			}
			
			// If we manage to unify against all bodies, apply the substitution
			// to the head and render it. If the generated head still has variables
			// in it, then do not add it to the result.
			if (success) {
				Dob generated = this.pool.submerge(unifier.replace(rule.head.dob, unify));
				if (Colut.containsNone(generated.fullIterable(), vars)) {
					result.add(generated);
				}
			}
			
			unify.clear();
			unify.putAll(reference);
		}
		
		return result;
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
	private List<Iterable<Dob>> getAssignmentSpace(Rule rule, int position,
			Dob dob) {
		List<Iterable<Dob>> candidates = Lists.newArrayList(); 
		for (int i = 0; i < rule.body.size(); i++) {
			Atom atom = rule.body.get(i);
			
			Iterable<Dob> next;
			if (!atom.truth) next = Lists.newArrayList((Dob)null);
			else if (i == position) next = Lists.newArrayList(dob);
			else next = getGroundCandidates(atom.dob);
			
			candidates.add(next);
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
		Iterable<Dob> subtree = this.fortre.getUnifySubtree(dob);
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
	private Multimap<Rule, Assignment> generateAssignments(Dob dob) {
		Multimap<Rule, Assignment> result = HashMultimap.create();

		// Iterate over all of the bodies we are potentially affecting.
		// This set must be a subset of the rules whose bodies are touched by the 
		// subtree of the fortre rooted at the end of the trunk.
		Set<Dob> subtree = Sets.newHashSet();
		Iterables.addAll(subtree, fortre.getUnifySubtree(dob));
		Iterable<Rule> rules = rta.ruleIterableFromBodyDobs(subtree);
		for (Rule rule : rules) {
			Set<Assignment> assignments = generateAssignments(rule, subtree, dob);
			result.putAll(rule, assignments);
		}
		
		return result;
	}
	
	public static Set<Assignment> generateAssignments(Rule rule, Set<Dob> forces, Dob ground) {
		Set<Assignment> result = Sets.newHashSet();
		for (int i = 0; i < rule.body.size(); i++) {
			Atom atom = rule.body.get(i);
			if (!atom.truth) continue;
			
			Dob body = atom.dob;
			if (forces.contains(body)) { result.add(new Assignment(ground, i, rule)); }
		}
		return result;
	}
	
	public static Iterator<Set<Dob>> asIterator(final StratifiedForward prover) {
		return new Iterator<Set<Dob>>() {
			@Override public boolean hasNext() { return prover.hasMore(); }
			@Override public Set<Dob> next() { return prover.proveNext(); }
			@Override public void remove() { throw new IllegalAccessError("Remove not allowed!"); }
		};
	}
}
