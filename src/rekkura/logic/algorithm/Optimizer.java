package rekkura.logic.algorithm;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import rekkura.logic.model.Rule;
import rekkura.logic.structure.Pool;
import rekkura.logic.structure.Ruletta;
import rekkura.util.Colut;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Optimizers modify sets of rules in order to improve performance.
 * E.g. of simulating the game.
 * @author ptpham
 *
 */
public abstract class Optimizer {

	/**
	 * Keys in the returned map will be removed from the ruleset and replaced
	 * with the values that they map to. Any null values in the resulting multimap
	 * will be disregarded.
	 * @param rta
	 * @param probhibited
	 * @param pool
	 * @return
	 */
	public abstract Multimap<Rule,Rule> apply(Ruletta rta, Set<Rule> probhibited, Pool pool);

	public static Set<Rule> standard(Iterable<Rule> rules, Set<Rule> prohibited, Pool pool) {
		return Optimizer.loop(rules, prohibited, pool, Optimizer.MERGE_POS_SUB_SINGLE, Optimizer.LIFT);
	}

	public static Set<Rule> loop(Iterable<Rule> orig, Set<Rule> prohibited, Pool pool, Optimizer... ops) {
		Set<Rule> result = Sets.newHashSet(orig);
		
		boolean changed = true;
		while (changed) {
			changed = false;
			for (Optimizer op : ops) {
				Ruletta rta = Ruletta.create(result, pool);
				Multimap<Rule,Rule> replace = op.apply(rta, prohibited, pool);
				replace.removeAll(prohibited);
				
				if (replace.size() > 0) {
					result.removeAll(replace.keySet());
					result.addAll(Colut.filterNulls(replace.values()));
					changed = true;
				}
			}
		}
		
		return result;
	}
	
	/**
	 * This class provides the boilerplate for an optimizer that replaces
	 * a single rule with one or more rules based on the rule's relationship
	 * with the children. The boilerplate involves code that avoids touching
	 * rules that have participated in an optimization during the current pass.
	 * @author ptpham
	 *
	 */
	private static abstract class LocalReplace extends Optimizer {
		@Override
		public Multimap<Rule, Rule> apply(Ruletta rta, Set<Rule> prohibited, Pool pool) {
			Multimap<Rule,Rule> result = HashMultimap.create();
			Set<Rule> touched = Sets.newHashSet();
			
			for (Rule rule : rta.ruleOrder) {
				Collection<Rule> children = rta.ruleToDescRule.get(rule);
				if (Colut.containsAny(children, touched)) continue;
				if (prohibited.contains(rule)) continue;
				if (touched.contains(rule)) continue;

				List<Rule> generated = apply(rule, children, pool);
				
				if (generated != null && children.size() > 0) {
					result.putAll(rule, generated);
					touched.addAll(children);
					touched.add(rule);
				}
			}
			
			return result;
		}

		protected abstract List<Rule> apply(Rule rule, Iterable<Rule> children, Pool pool);
	}

	
	/**
	 * In order for this merge optimization to apply to a rule, it must
	 * be possible to merge with all children of the rule in exactly one
	 * position. Furthermore, the result of merges must have fewer 
	 * variables than the number of variables in the source rule.
	 * @author ptpham
	 *
	 */
	public static Optimizer MERGE_POS_SUB_SINGLE = new LocalReplace() {
		@Override
		protected List<Rule> apply(Rule rule, Iterable<Rule> children, Pool pool) {
			List<Rule> generated = Lists.newArrayList();
			for (Rule child : children) {
				List<Rule> merged = Merge.applyOperation(rule, child, Merges.POSITIVE_SUBSTITUTION, pool);
				if (merged.size() != 1) merged.clear();
				
				Rule current = Colut.first(merged);
				if (current != null && current.vars.size() < rule.vars.size()) generated.add(current);
				else return null;
			} return generated;
		}
	};
	
	/**
	 * If all children of the rule can restrict the rule in some way, then
	 * we can remove the rule and add all of the restricted versions.
	 */
	public static Optimizer LIFT = new LocalReplace() {
		@Override
		protected List<Rule> apply(Rule rule, Iterable<Rule> children, Pool pool) {
			List<Rule> generated = Lists.newArrayList();
			for (Rule child : children) {
				List<Rule> lifted = Comprender.lift(rule, child, pool);
				if (lifted.isEmpty()) return null;
				generated.addAll(lifted);
			} return generated;
		}
	};

}
