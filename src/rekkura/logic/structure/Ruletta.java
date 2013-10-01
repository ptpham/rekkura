package rekkura.logic.structure;

import java.util.List;
import java.util.Set;

import rekkura.logic.algorithm.Unifier;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.state.algorithm.Topper;
import rekkura.util.Colut;
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
	
	public Dob homvar;
	public Set<Rule> allRules;
	public Set<Dob> posDobs, negDobs;
	public Multimap<Dob, Rule> bodyToRule, headToRule;

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
	}
	
	public static Ruletta create(Iterable<Rule> rules, Pool pool) {
		Ruletta result = new Ruletta();
		result.allRules.addAll(pool.rules.submerge(rules));
		
		for (Atom atom : Rule.asAtomIterator(result.allRules)) {
			if (atom.truth) result.posDobs.add(atom.dob);
			else result.negDobs.add(atom.dob);
		}
		
		// Extract all variables
		for (Rule rule : result.allRules) {  pool.allVars.addAll(rule.vars); }

		// Prepare data structures to compute dependencies
		result.homvar = Colut.any(pool.allVars);
		for (Rule rule : result.allRules) { 
			Dob headForm = Unifier.homogenize(rule.head.dob, result.homvar, pool);
			result.headToRule.put(headForm, rule);
			for (Dob body : Atom.asDobIterable(rule.body)) {
				Dob bodyForm = Unifier.homogenize(body, result.homvar, pool);
				result.bodyToRule.put(bodyForm, rule);	
			}
		}
		
		result.bodyToGenHead = Unifier.nonConflicting(result.bodyToRule.keySet(),
			result.headToRule.keySet(), pool);
		
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

	public static Ruletta createEmpty() { return new Ruletta(); }

	public static List<Rule> filterNonConflictingBodies(Dob query, Iterable<Rule> targets, Pool pool) {
		List<Rule> result = Lists.newArrayList();
		for (Rule rule : targets) {
			for (Atom term : rule.body) {
				if (Unifier.nonConflicting(term.dob, query, pool)) result.add(rule);
			}
		}
		return result;
	}
}
