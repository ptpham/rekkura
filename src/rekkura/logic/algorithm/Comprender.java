package rekkura.logic.algorithm;

import java.util.List;

import rekkura.logic.model.Rule;
import rekkura.logic.structure.Pool;
import rekkura.util.Colut;

import com.google.common.collect.Lists;

/**
 * This class is responsible for working with combinations of rules. <br>
 * @author ptpham
 *
 */
public class Comprender {

	/**
	 * Generates all rules that can be constructed by merging
	 * the rules in the path in order using the given operation
	 * @param path
	 * @param pool
	 * @param op
	 * @return
	 */
	public static List<Rule> mergeAll(List<Rule> path, Pool pool, Merge.Operation op) {
		List<Rule> result = Lists.newArrayList();
		if (path.size() < 2) {
			result.addAll(path);
			return result;
		}
		
		Rule src = Colut.first(path);
		List<Rule> compressed = mergeAll(Colut.slice(path, 1, path.size()), pool, op);
		for (Rule dst : compressed) result.addAll(Merge.applyOperation(src, dst, op, pool));
				
		return result;
	}
	
	public static Rule lift(Rule src, Rule dst, int dstPosition, Pool pool) {
		Merge.Result merge = Merge.compute(src, dst, dstPosition, pool);
		if (merge == null) return null;
		
		return Unifier.replace(src, merge.srcUnify, merge.vars);
	}
	
	public static List<Rule> lift(Rule src, Rule dst, Pool pool) {
		List<Rule> result = Lists.newArrayList();
		for (int i = 0; i < dst.body.size(); i++) {
			Rule lifted = lift(src, dst, i, pool);
			if (lifted != null) result.add(lifted);
		}
		return result;
	}
}
