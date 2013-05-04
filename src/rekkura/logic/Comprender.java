package rekkura.logic;

import java.util.List;

import rekkura.logic.merge.Merge;
import rekkura.model.Rule;
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
		for (Rule dst : compressed) result.addAll(op.mergeRules(src, dst, pool));
				
		return result;
	}
}
