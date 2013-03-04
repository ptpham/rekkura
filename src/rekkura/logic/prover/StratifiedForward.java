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
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
	 * This holds the set of dobs that may generate 
	 */
	public Map<Dob, Set<Dob>> deps;

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
	protected Map<Dob, Set<Dob>> unisuccess;
	
	/**
	 * This is the set of ground terms that have not been used to generate
	 * new ground terms.
	 */
	private Set<Dob> unexpanded;
	
	/**
	 * This map has a size which is the number of negative bodies
	 * times the number of bodies.
	 */
	private Map<Dob, Set<Dob>> negDepMap;

	public StratifiedForward(Collection<Rule> rules) {
		Set<Rule> submerged = Sets.newHashSet();
		for (Rule rule : rules) { submerged.add(pool.submerge(rule)); }
		
		rta.construct(submerged);

		for (Rule rule : rta.allRules) {
			Preconditions.checkArgument(rule.head.truth, "Rules must have positive heads!");
		}
		
		this.deps = toper.dependencies(rta.headToRule.keySet(), 
				rta.bodyToRule.keySet(), rta.allVars);
		
		this.negDepMap = Maps.newHashMap();
		for (Dob dob : rta.negDobs) {
			Set<Dob> ancestors = OTMUtil.flood(deps, dob);
			ancestors.remove(dob);
			this.negDepMap.put(dob, ancestors);
		}
		
		this.fortre = new Fortre(rta.allVars);
		
		for (Dob dob : rta.bodyToRule.keySet()) { this.fortre.addDob(dob); }
		
		this.unisuccess = Maps.newHashMap();
		this.unexpanded = Sets.newHashSet();
		this.truths = Sets.newHashSet();
	}

	/**
	 * Add a dob that is true. The dob will be attached to the last
	 * node on its unify trunk of the fortre. 
	 * @param dob
	 */
	public void queueTruth(Dob dob) {
		List<Dob> trunk = this.fortre.getUnifyTrunk(dob);
		Dob end = Colut.end(trunk);
		if (!OTMUtil.contains(unisuccess, end, dob)) {
			OTMUtil.put(unisuccess, end, dob);
		}
		
		this.unexpanded.add(dob);
	}
	
	public boolean hasMore() { return Colut.nonEmpty(unexpanded); }
	
	public Set<Dob> proveNext() {
		if (!hasMore()) throw new NoSuchElementException();
		Dob dob = Colut.popAny(unexpanded);
		Set<Dob> raw = expand(dob);
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
	 * This method holds the meat of this class. This method takes a dob
	 * and returns the set of dobs that are entailed by the given dob and 
	 * the current set of exhausted truths.
	 * @param dob
	 * @return
	 */
	public Set<Dob> expand(Dob dob) {
		Set<Dob> result = Sets.newHashSet();
		
		List<Dob> trunk = fortre.getUnifyTrunk(dob);
		Dob end = Colut.end(trunk);
		
		// Iterate over all of the bodies we are potentially affecting.
		// This set must be a subset of the rules whose bodies are touched by the 
		// subtree of the fortre rooted at the end of the trunk.
		Set<Dob> subtree = Sets.newHashSet();
		Iterables.addAll(subtree, fortre.getSubtreeIterable(end));
		Iterable<Rule> rules = rta.ruleIterableFromBodyDobs(subtree);
		for (Rule rule : rules) {
			Colut.addAll(result, expand(rule, subtree, dob));
		}
		
		return result;
	}
	
	public Set<Dob> expand(Rule rule, Set<Dob> forces, Dob dob) {
		// Verify that this is in fact a rule that we can pivot on
		// by finding one of the forces in the body of the rule.
		for (Dob pivot : Rule.dobIterableFromRule(rule)) {
			if (!Colut.contains(forces, pivot)) continue;
			
			
		}
		
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
