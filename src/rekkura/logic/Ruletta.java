package rekkura.logic;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.OTMUtil;

import com.google.common.collect.*;

/**
 * result class maintains mappings and sets that are important for 
 * working with a set of rules.
 * @author ptpham
 *
 */
public class Ruletta {
	
	public Set<Rule> allRules;
	public Set<Dob> allVars, allDobs, posDobs, negDobs;
	public Multimap<Dob, Rule> bodyToRule, headToRule;
	public Fortre fortre;

	/**
	 * result holds the set of head dobs that may generate a body dob.
	 * Memory is O(R^2).
	 */
	public Multimap<Dob, Dob> bodyToGenHead;
	
	/**
	 * result holds the set of rules that may generate a body dob.
	 * Memory is O(R^2).
	 */
	public Multimap<Dob, Rule> bodyToGenRule;

	/**
	 * result holds the set of rules that may generate dobs for the body
	 * of the given rule.
	 * Memory is O(R^2).
	 */
	public Multimap<Rule, Rule> ruleToGenRule;
	
	/**
	 * These are the rules that do not have any dependencies.
	 */
	public Set<Rule> ruleRoots;
	
	/**
	 * result holds the index of rules in topological order
	 */
	public Multiset<Rule> ruleOrder;
	
	public static Ruletta create(Collection<Rule> rules, Pool pool) {
		Ruletta result = new Ruletta();
		result.allRules = Sets.newHashSet(rules);
		
		result.allVars = Sets.newHashSet();
		result.allDobs = Sets.newHashSet();
		result.posDobs = Sets.newHashSet();
		result.negDobs = Sets.newHashSet();

		result.bodyToRule = HashMultimap.create();
		result.headToRule = HashMultimap.create();
		
		for (Dob dob : Rule.dobIterableFromRules(result.allRules)) { result.allDobs.add(dob); }
		for (Atom atom : Rule.atomIterableFromRules(result.allRules)) {
			if (atom.truth) result.posDobs.add(atom.dob);
			else result.negDobs.add(atom.dob);
		}
		
		// Extract all variables
		for (Rule rule : result.allRules) {  result.allVars.addAll(rule.vars); }
		
		// Extract all terms
		Set<Dob> allTerms = Sets.newHashSet();
		for (Atom atom : Rule.atomIterableFromRules(result.allRules)) { allTerms.add(atom.dob); }
		result.fortre = new Fortre(result.allVars, allTerms, pool);

		// Prepare data structures to compute dependencies
		for (Rule rule : result.allRules) { 
			result.headToRule.put(rule.head.dob, rule);
			for (Dob body : Atom.dobIterableFromAtoms(rule.body)) {
				result.bodyToRule.put(body, rule);	
			}
		}
		
		result.bodyToGenHead = dependencies(result.bodyToRule.keySet(), 
				result.headToRule.keySet(), result.allVars);
		
		result.bodyToGenRule = OTMUtil.joinRight(result.bodyToGenHead, result.headToRule);
		result.ruleToGenRule = OTMUtil.joinLeft(result.bodyToGenRule, result.bodyToRule);
		
		result.ruleRoots = Sets.newHashSet();
		for (Rule rule : result.allRules) {
			if (result.ruleToGenRule.get(rule).size() == 0) {
				result.ruleRoots.add(rule);
			}
		}
		
		Multimap<Rule, Rule> ruleToDescRule = HashMultimap.create();
		Multimaps.invertFrom(result.ruleToGenRule, ruleToDescRule);
		
		result.ruleOrder = Topper.generalTopSort(ruleToDescRule, result.ruleRoots);

		return result;
	}
	

	/**
	 * Computes for each target dob the set of source dobs that unify with it.
	 * @param dobs
	 * @param vars
	 * @param fortre 
	 * @return
	 */
	public static Multimap<Dob, Dob> dependencies(Collection<Dob> targetDobs, 
			Collection<Dob> sourceDobs, Set<Dob> vars) {
		Multimap<Dob, Dob> result = HashMultimap.create(targetDobs.size(), sourceDobs.size());
		
		for (Dob target : targetDobs) {
			for (Dob source : sourceDobs) {
				Map<Dob, Dob> unify = Unifier.unifyVars(target, source, vars);
				if (unify == null) continue;
				result.put(target, source);
			}
		}
		
		return result;
	}
	

	/**
	 * result method takes a dob and returns the rules where
	 * it can be applied. result set must be a subset of the rules whose bodies are 
	 * touched by the subtree of the fortre rooted at the end of the trunk.
	 * @param dob
	 * @return
	 */
	public Set<Rule> getAffectedRules(Dob dob) {
		Set<Dob> subtree = Sets.newHashSet();
		Iterables.addAll(subtree, fortre.getCognateSplay(dob));
		return Sets.newHashSet(ruleIterableFromBodyDobs(subtree));
	}

	public Iterable<Dob> getAllTerms() {
		return Iterables.concat(this.headToRule.keySet(), this.bodyToRule.keySet()); 
	}
	
	public Iterator<Rule> ruleIteratorFromBodyDobs(Iterator<Dob> dobs) {
		return OTMUtil.valueIterator(bodyToRule, dobs);
	}
	
	public Iterable<Rule> ruleIterableFromBodyDobs(final Iterable<Dob> dobs) {
		return OTMUtil.valueIterable(bodyToRule, dobs);
	}
}
