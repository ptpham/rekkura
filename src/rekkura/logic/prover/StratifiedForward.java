package rekkura.logic.prover;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import rekkura.logic.Fortre;
import rekkura.logic.Pool;
import rekkura.logic.Ruletta;
import rekkura.logic.Topper;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.model.Rule.Assignment;
import rekkura.util.Colut;
import rekkura.util.OTMUtil;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;

/**
 * The set of rules provided to this prover must satisfy the following:
 * - No negative heads
 * - Stratified negation: From a rule R and its descendants, it must 
 * not be possible to generate a grounded dob that unifies with a 
 * negative term in the body of R.
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
	 * This is the set of ground terms that have been exhausted.
	 * A dob is exhausted if it has been formally used as the pivot
	 * to prove dobs in this class. (i.e. used in proveNext())
	 */
	public Set<Dob> truths;
	
	/**
	 * These hold the mappings from a body term B in a rule to grounds 
	 * that are known to successfully unify with B.
	 */
	protected Multimap<Dob, Dob> unisuccess;
	
	private Multimap<Rule, Assignment> pending;
	private Multimap<Rule, Assignment> waiting;
	private Multiset<Rule> negDepCounter;
	
	public StratifiedForward(Collection<Rule> rules, Collection<Dob> truths) {
		Set<Rule> submerged = Sets.newHashSet();
		this.pool = new Pool();
		this.rta = new Ruletta();
		this.topper = new Topper();
		
		for (Rule rule : rules) { submerged.add(pool.submerge(rule)); }
		
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
		this.truths = Sets.newHashSet();
		this.pending = HashMultimap.create();
		this.waiting = HashMultimap.create();
		this.negDepCounter = HashMultiset.create();
		
		for (Dob truth : truths) queueTruth(truth);
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
		List<Dob> trunk = storeGround(dob);
		Multimap<Rule, Assignment> generated = this.generateAssignments(dob, trunk);
		
		for (Rule rule : generated.keySet()) {
			int increase = generated.get(rule).size();
			Collection<Rule> negDescs = this.ruleNegDesc.get(rule);
			Colut.shiftAll(negDepCounter, negDescs, increase);
		}
		
		// Split the rules into pending vs waiting.
		// An assignment is pending if it is ready to be expanded.
		// An assignment is waiting if it's rule is being blocked by a non-zero 
		// number of pending/waiting ancestors.
		for (Rule rule : generated.keySet()) {
			Collection<Assignment> assignments = generated.get(rule);
			if (this.negDepCounter.count(rule) > 0) {
				this.waiting.putAll(rule, assignments);
			} else {
				this.pending.putAll(rule, assignments);
			}
		}
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
	
	public boolean hasMore() { return this.pending.size() > 0; }
	
	public Set<Dob> proveNext() {
		if (!hasMore()) throw new NoSuchElementException();
		
		// Find a pending assignment on which we can actually operate.
		// We can only operate on a rule R that doesn't have any ancestors
		// that still might generate grounds that potentially
		// unify with a negative body term in R.
		
		Dob dob = null;
		
		// TODO: Generate new dobs
		Set<Dob> raw = Sets.newHashSet();
		
		truths.add(dob);
		
		// Submerge all of the newly generated dobs
		Set<Dob> result = Sets.newHashSetWithExpectedSize(raw.size());
		result.addAll(this.pool.submergeDobs(raw));
		
		result.removeAll(truths);
		
		return result;
	}
	
	public Set<Dob> expand(Rule rule, int position, Dob dob) {
		
		
		return null;
	}
	
	/**
	 * This method takes a dob and returns the pivots
	 * where it can be applied.
	 * @param dob
	 * @return
	 */
	public Multimap<Rule, Assignment> generateAssignments(Dob dob) {
		List<Dob> trunk = fortre.getUnifyTrunk(dob);
		return generateAssignments(dob, trunk);
	}

	private Multimap<Rule, Assignment> generateAssignments(Dob dob,
			List<Dob> trunk) {
		Multimap<Rule, Assignment> result = HashMultimap.create();
		Dob end = Colut.end(trunk);
		
		// Iterate over all of the bodies we are potentially affecting.
		// This set must be a subset of the rules whose bodies are touched by the 
		// subtree of the fortre rooted at the end of the trunk.
		Set<Dob> subtree = Sets.newHashSet();
		Iterables.addAll(subtree, fortre.getSubtreeIterable(end));
		Iterable<Rule> rules = rta.ruleIterableFromBodyDobs(subtree);
		for (Rule rule : rules) {
			Set<Assignment> pivots = generateAssignments(rule, subtree, dob);
			result.putAll(rule, pivots);
		}
		
		return result;
	}
	
	public static Set<Assignment> generateAssignments(Rule rule, Set<Dob> forces, Dob ground) {
		Set<Assignment> result = Sets.newHashSet();
		for (int i = 0; i < rule.body.size(); i++) {
			Dob body = rule.body.get(i).dob;
			if (forces.contains(body)) { result.add(new Assignment(i, ground)); }
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
