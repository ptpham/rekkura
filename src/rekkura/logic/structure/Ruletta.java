package rekkura.logic.structure;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import rekkura.logic.algorithm.Unifier;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.state.algorithm.Topper;
import rekkura.util.OtmUtil;

import com.google.common.collect.*;

/**
 * This class maintains mappings and sets that are important for 
 * working with a set of rules. It also provides a handful of
 * utility operations like getting the set of rules that are 
 * affected by a given {@link Dob}.
 * @author ptpham
 *
 */
public class Ruletta {
	
	public Set<Rule> allRules;
	public Set<Dob> posDobs, negDobs;
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
	public Multimap<Rule, Rule> ruleToDescRule;

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
	
	private Ruletta() {
		allRules = Sets.newHashSet();
		
		posDobs = Sets.newHashSet();
		negDobs = Sets.newHashSet();

		bodyToRule = HashMultimap.create();
		headToRule = HashMultimap.create();
		ruleToGenRule = HashMultimap.create();
		
		ruleRoots = Sets.newHashSet();
		
		fortre = new Fortre(Lists.<Dob>newArrayList(), new Pool());
	}
	
	public static Ruletta create(Collection<Rule> rules, Pool pool) {
		Ruletta result = new Ruletta();
		result.allRules.addAll(pool.rules.submerge(rules));
		
		for (Atom atom : Rule.asAtomIterator(result.allRules)) {
			if (atom.truth) result.posDobs.add(atom.dob);
			else result.negDobs.add(atom.dob);
		}
		
		// Extract all variables
		for (Rule rule : result.allRules) {  pool.allVars.addAll(rule.vars); }
		
		// Extract all terms
		Set<Dob> allTerms = Sets.newHashSet();
		for (Atom atom : Rule.asAtomIterator(result.allRules)) { allTerms.add(atom.dob); }
		result.fortre = new Fortre(allTerms, pool);

		// Prepare data structures to compute dependencies
		for (Rule rule : result.allRules) { 
			result.headToRule.put(result.fortre.getTrunkEnd(rule.head.dob), rule);
			for (Dob body : Atom.asDobIterable(rule.body)) {
				result.bodyToRule.put(result.fortre.getTrunkEnd(body), rule);	
			}
		}
		
		result.bodyToGenHead = Unifier.nonConflicting(result.bodyToRule.keySet(), 
				result.headToRule.keySet(), pool.allVars);
		
		result.bodyToGenRule = OtmUtil.joinRight(result.bodyToGenHead, result.headToRule);
		result.ruleToGenRule = OtmUtil.joinLeft(result.bodyToGenRule, result.bodyToRule);
		
		for (Rule rule : result.allRules) {
			if (result.ruleToGenRule.get(rule).size() == 0) {
				result.ruleRoots.add(rule);
			}
		}
		
		result.ruleToDescRule = HashMultimap.create();
		Multimaps.invertFrom(result.ruleToGenRule, result.ruleToDescRule);
		
		result.ruleOrder = Topper.generalTopSort(result.ruleToDescRule, result.ruleRoots);
		return result;
	}
	
	/**
	 * This returns the list of rules mapped to by forms that unify
	 * against the given query.
	 * @param query a dob with variables that you want to unify against
	 * forms in the form tree.
	 * @param map the place where you want to look up rules after you 
	 * have filtered the forms you are interested in.
	 * @return
	 */
	public Iterable<Rule> getRulesWith(Dob query, Multimap<Dob, Rule> map) {
		List<Dob> forms = Unifier.retainSuccesses(query, 
			this.fortre.getCognateSpine(query), this.fortre.pool.allVars);
		return OtmUtil.valueIterable(map, forms);
	}
	
	/**
	 * Returns the set of rules that are mapped to by the forms in the
	 * spine of the given dob.
	 * @param query
	 * @param map
	 * @return
	 */
	public Iterable<Rule> getSpineRules(Dob query, Multimap<Dob, Rule> map) {
		return OtmUtil.valueIterable(map, this.fortre.getCognateSpine(query));
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
		Iterables.addAll(subtree, fortre.getSpine(dob));
		return Sets.newHashSet(OtmUtil.valueIterable(bodyToRule, subtree));
	}

	public Iterable<Dob> getAllTerms() {
		return Iterables.concat(this.headToRule.keySet(), this.bodyToRule.keySet()); 
	}

	public static Ruletta createEmpty() { return new Ruletta(); }
}
