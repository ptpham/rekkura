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

public abstract class Optimizer {

	public abstract Multimap<Rule,Rule> apply(Ruletta rta, Set<Rule> probhibited, Pool pool);

	public static Set<Rule> standard(Iterable<Rule> rules, Set<Rule> prohibited, Pool pool) {
		return Optimizer.loop(rules, prohibited, pool, Optimizer.MERGE_POS_SUB_SINGLE);
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
	 * In order for this merge optimization to apply to a rule, it must
	 * be possible to merge with all children of the rule in exactly one
	 * position.
	 * @author ptpham
	 *
	 */
	public static Optimizer MERGE_POS_SUB_SINGLE = new Optimizer() {
		@Override
		public Multimap<Rule, Rule> apply(Ruletta rta, Set<Rule> prohibited, Pool pool) {
			Multimap<Rule,Rule> result = HashMultimap.create();
			Set<Rule> touched = Sets.newHashSet();
			
			for (Rule rule : rta.ruleOrder) {
				boolean success = true;
				List<Rule> generated = Lists.newArrayList();
				Collection<Rule> children = rta.ruleToDescRule.get(rule);
				if (Colut.containsAny(children, touched)) continue;
				if (prohibited.contains(rule)) continue;
				if (touched.contains(rule)) continue;

				for (Rule child : children) {
					List<Rule> merged = Merge.applyOperation(rule, child, Merges.POSITIVE_SUBSTITUTION, pool);
					if (merged.size() != 1) merged.clear();
					
					Rule current = Colut.first(merged);
					if (current != null && current.vars.size() < rule.vars.size()) {
						generated.add(current);
					} else {
						success = false;
						break;
					}
				}
				
				if (success && children.size() > 0) {
					result.putAll(rule, generated);
					touched.addAll(children);
					touched.add(rule);
				}
			}
			
			return result;
		}
	};

}
