package rekkura.logic.prover;

import java.util.*;

import rekkura.logic.Fortre;
import rekkura.logic.Pool;
import rekkura.logic.Ruletta;
import rekkura.logic.Topper;
import rekkura.model.Dob;
import rekkura.model.Rule;
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
	
	public Topper toper;
	
	/**
	 * This holds the set of head dobs that may generate a body dob.
	 */
	public Multimap<Dob, Dob> headDeps;
	
	/**
	 * This holds the set of rules that may generate a body dob.
	 */
	public Multimap<Dob, Rule> dobRuleDeps;

	/**
	 * This holds the set of rules that may generate dobs for the body
	 * of the given rule.
	 */
	public Multimap<Rule, Rule> ruleRuleDeps;

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
	
	/**
	 * This is the set of ground terms that have not been used to generate
	 * new ground terms.
	 */
	private Set<Dob> unexpanded;
	
	private static class Pivot {
		public final Dob ground;
		public final int position;
		
		public Pivot(int position, Dob ground) {
			this.position = position;
			this.ground = ground;
		}
	}
	
	private Map<Rule, Set<Pivot>> pendingPivots;
	
	public StratifiedForward(Collection<Rule> rules) {
		Set<Rule> submerged = Sets.newHashSet();
		for (Rule rule : rules) { submerged.add(pool.submerge(rule)); }
		
		rta.construct(submerged);

		for (Rule rule : rta.allRules) {
			Preconditions.checkArgument(rule.head.truth, "Rules must have positive heads!");
		}
		
		this.headDeps = toper.dependencies(rta.bodyToRule.keySet(), 
				rta.headToRule.keySet(), rta.allVars);
		
		this.dobRuleDeps = OTMUtil.joinRight(this.headDeps, this.rta.headToRule);
		this.ruleRuleDeps = OTMUtil.joinLeft(this.dobRuleDeps, this.rta.bodyToRule);
		
		this.fortre = new Fortre(rta.allVars);
		
		for (Dob dob : rta.bodyToRule.keySet()) { this.fortre.addDob(dob); }
		
		this.unisuccess = HashMultimap.create();
		this.unexpanded = Sets.newHashSet();
		this.truths = Sets.newHashSet();
		this.pendingPivots = Maps.newHashMap();
	}

	/**
	 * Add a dob that is true. The dob will be attached to the last
	 * node on its unify trunk of the fortre. 
	 * @param dob
	 */
	public void queueTruth(Dob dob) {
		List<Dob> trunk = this.fortre.getUnifyTrunk(dob);
		Dob end = Colut.end(trunk);
		if (!unisuccess.containsEntry(end, dob)) {
			unisuccess.put(end, dob);
		}
		
		this.unexpanded.add(dob);
	}
	
	public boolean hasMore() { return Colut.nonEmpty(unexpanded); }
	
	public Set<Dob> proveNext() {
		if (!hasMore()) throw new NoSuchElementException();
		Dob dob = Colut.popAny(unexpanded);
		
		// TODO: Generate new dobs
		Set<Dob> raw = Sets.newHashSet();
		
		truths.add(dob);
		
		// Submerge all of the newly generated dobs
		Set<Dob> result = Sets.newHashSetWithExpectedSize(raw.size());
		result.addAll(this.pool.submergeDobs(raw));
		
		result.removeAll(truths);
		result.removeAll(unexpanded);
		unexpanded.addAll(result);
		
		return result;
	}
	
	/**
	 * This method takes a dob and returns the pivots
	 * where it can be applied.
	 * @param dob
	 * @return
	 */
	public Multimap<Rule, Pivot> generatePivots(Dob dob) {
		Multimap<Rule, Pivot> result = HashMultimap.create();
		
		List<Dob> trunk = fortre.getUnifyTrunk(dob);
		Dob end = Colut.end(trunk);
		
		// Iterate over all of the bodies we are potentially affecting.
		// This set must be a subset of the rules whose bodies are touched by the 
		// subtree of the fortre rooted at the end of the trunk.
		Set<Dob> subtree = Sets.newHashSet();
		Iterables.addAll(subtree, fortre.getSubtreeIterable(end));
		Iterable<Rule> rules = rta.ruleIterableFromBodyDobs(subtree);
		for (Rule rule : rules) {
			Set<Pivot> pivots = pivotize(rule, subtree, dob);
			result.putAll(rule, pivots);
		}
		
		return result;
	}
	
	public static Set<Pivot> pivotize(Rule rule, Set<Dob> forces, Dob ground) {
		Set<Pivot> result = Sets.newHashSet();
		for (int i = 0; i < rule.body.size(); i++) {
			Dob body = rule.body.get(i).dob;
			if (forces.contains(body)) { result.add(new Pivot(i, ground)); }
		}
		return result;
	}
	
	public Set<Dob> expand(Rule rule, int position, Dob dob) {
		
		
		return null;
	}
	
	public static Iterator<Set<Dob>> asIterator(final StratifiedForward prover) {
		return new Iterator<Set<Dob>>() {
			@Override public boolean hasNext() { return prover.hasMore(); }
			@Override public Set<Dob> next() { return prover.proveNext(); }
			@Override public void remove() { throw new IllegalAccessError("Remove not allowed!"); }
		};
	}
}
