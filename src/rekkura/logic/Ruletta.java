package rekkura.logic;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
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
	
	public Set<Rule> allRules;
	public Set<Dob> allVars, posDobs, negDobs;
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
	
	private Ruletta() {
		allRules = Sets.newHashSet();
		
		allVars = Sets.newHashSet();
		posDobs = Sets.newHashSet();
		negDobs = Sets.newHashSet();

		bodyToRule = HashMultimap.create();
		headToRule = HashMultimap.create();
		ruleToGenRule = HashMultimap.create();
		
		ruleRoots = Sets.newHashSet();
		
		fortre = new Fortre(allVars, Lists.<Dob>newArrayList(), new Pool());
	}
	
	public static Ruletta create(Collection<Rule> rules, Pool pool) {
		Ruletta result = new Ruletta();
		result.allRules.addAll(rules);
		
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
			result.headToRule.put(result.fortre.getTrunkEnd(rule.head.dob), rule);
			for (Dob body : Atom.dobIterableFromAtoms(rule.body)) {
				result.bodyToRule.put(result.fortre.getTrunkEnd(body), rule);	
			}
		}
		
		result.bodyToGenHead = dependencies(result.bodyToRule.keySet(), 
				result.headToRule.keySet(), result.allVars);
		
		result.bodyToGenRule = OtmUtil.joinRight(result.bodyToGenHead, result.headToRule);
		result.ruleToGenRule = OtmUtil.joinLeft(result.bodyToGenRule, result.bodyToRule);
		
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
	
	public List<Dob> getVariables(int num) {
		List<Dob> result = Lists.newArrayList();
		
		while (this.allVars.size() < num) {
			Dob generated = new Dob("[RTA" + this.allVars.size() + "]");
			this.allVars.add(generated);
		}
		
		Iterables.addAll(result, Colut.firstK(this.allVars, num)); 
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
			this.fortre.getAllCognates(), this.allVars);
		return OtmUtil.valueIterable(map, forms);
	}
	
	public List<List<Rule>> getPathsBetween(Dob src, Dob dst) {
		Set<Rule> targets = Sets.newHashSet(OtmUtil.valueIterable(this.headToRule, 
				this.fortre.getCognateSpine(dst)));
		Set<Rule> sources = Sets.newHashSet(OtmUtil.valueIterable(this.bodyToRule, 
				this.fortre.getCognateSpine(src)));

		List<List<Rule>> result = Lists.newArrayList();
		for (Rule target : targets) {
			Multimap<Rule, Rule> edges = Topper.dijkstra(target, this.ruleToGenRule);
			Set<Rule> reachable = OtmUtil.flood(edges, sources);
			OtmUtil.retainAll(reachable, edges);
			
			Set<Rule> roots = Topper.findRoots(edges);
			roots.retainAll(sources);
			for (Rule source : roots) {
				List<List<Rule>> paths = Topper.getPaths(source, target, edges);
				result.addAll(paths);
			}
		}
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
				if (Unifier.unifyVars(target, source, vars) == null 
					&& Unifier.unifyVars(source, target, vars) == null) continue;
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
		Iterables.addAll(subtree, fortre.getSpine(dob));
		return Sets.newHashSet(ruleIterableFromBodyDobs(subtree));
	}

	public Iterable<Dob> getAllTerms() {
		return Iterables.concat(this.headToRule.keySet(), this.bodyToRule.keySet()); 
	}
	
	public Iterator<Rule> ruleIteratorFromBodyDobs(Iterator<Dob> dobs) {
		return OtmUtil.valueIterator(bodyToRule, dobs);
	}
	
	public Iterable<Rule> ruleIterableFromBodyDobs(final Iterable<Dob> dobs) {
		return OtmUtil.valueIterable(bodyToRule, dobs);
	}

	public static Ruletta createEmpty() { return new Ruletta(); }
}
