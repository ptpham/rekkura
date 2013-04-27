package rekkura.logic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Colut;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class is responsible for working with combinations of rules. <br>
 * - mergeRules(src, dst): incorporate the terms in src into dst <br>
 * - pushVars(src, dst): generate the unification that assigns variables
 * in src to dobs in dst <br>
 * - pullRule(src, dst): generate a copy of src that is partially 
 * grounded by information in dst <br>
 * @author ptpham
 *
 */
public class Comprender {

	/**
	 * Generates all rules that can be constructed by merging
	 * the rules in the path in order.
	 * @param path
	 * @return
	 */
	public static List<Rule> mergeAll(List<Rule> path, Pool pool) {
		List<Rule> result = Lists.newArrayList();
		if (path.size() < 2) {
			result.addAll(path);
			return result;
		}
		
		Rule src = Colut.first(path);
		List<Rule> compressed = mergeAll(Colut.slice(path, 1, path.size()), pool);
		for (Rule dst : compressed) result.addAll(mergeRules(src, dst, pool));
				
		return result;
	}
	
	/**
	 * This method will return all possible ways that the head of 
	 * the source rule might fit into the body of the destination rule.
	 * @param src
	 * @param dst
	 * @return
	 */
	public static List<Rule> mergeRules(Rule src, Rule dst, Pool pool) {
		List<Rule> result = Lists.newArrayList();
		
		for (int i = 0; i < dst.body.size(); i++) {
			RulePairUnify pair = new RulePairUnify(src, dst, i);
			if (!compute(pair, pool)) continue;
			
			// Apply the unifications to their respective rules
			Rule srcFixed = pool.rules.submerge(Unifier.replace(src, pair.srcUnify, dst.vars));
			Rule dstFixed = pool.rules.submerge(Unifier.replace(dst, pair.dstUnify, src.vars));
			
			List<Atom> body = Lists.newArrayList();
			for (int j = 0; j < dstFixed.body.size(); j++) {
				if (j != pair.dstPosition) body.add(dstFixed.body.get(j));
			}
			body.addAll(srcFixed.body);
			
			Set<Dob> vars = Sets.newHashSet(dstFixed.vars);
			vars.removeAll(pair.dstUnify.keySet());
			vars.addAll(srcFixed.vars);
			
			Rule merged = new Rule(dstFixed.head, body, vars);
			result.add(merged);
		}
		
		return result;
	}
	
	private static class RulePairUnify {
		Map<Dob, Dob> srcUnify = Maps.newHashMap();
		Map<Dob, Dob> dstUnify = Maps.newHashMap();
		final Rule src, dst;
		final int dstPosition;
		
		public RulePairUnify(Rule src, Rule dst, int dstPosition) {
			this.dstPosition = dstPosition;
			this.src = src;
			this.dst = dst;
		}
	}
	
	private static boolean compute(RulePairUnify pair, Pool pool) {
		Atom term = pair.dst.body.get(pair.dstPosition);
		Map<Dob, Dob> unify = Unifier.unify(pair.src.head.dob, term.dob);

		boolean splitSuccess = true;
		for (Map.Entry<Dob, Dob> entry : unify.entrySet()) {
			Dob key = entry.getKey();
			Dob val = entry.getValue();
			
			boolean keyIsVar = pair.src.vars.contains(key);
			boolean valIsVar = pair.dst.vars.contains(val);
			
			if (keyIsVar) pair.srcUnify.put(key, val);
			else if (valIsVar) pair.dstUnify.put(val, key);
			else splitSuccess = false;
		}
		return splitSuccess;
	}
}
