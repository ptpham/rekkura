package rekkura.logic.merge;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.Pool;
import rekkura.logic.Unifier;
import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class is responsible for computing the data structures
 * relevant to the expansion of a term in a rule (the destination)
 * given another rule (the source).
 * @author ptpham
 *
 */
public class Merge {
	public static class Request {
		public final Rule src, dst;
		public final int dstPosition;
		public Request(Rule src, Rule dst, int dstPosition) {
			this.dstPosition = dstPosition;
			this.src = src;
			this.dst = dst;
		}
		
		public Atom getPivot() { return dst.body.get(dstPosition); }
	}
	
	public static class Result {
		public final Map<Dob, Dob> srcUnify = Maps.newHashMap();
		public final Map<Dob, Dob> dstUnify = Maps.newHashMap();
		public final Request request;
		
		public Result(Request request) {
			this.request = request;
		}
		
		public Atom getPivot() { return request.getPivot(); }
	}
	
	public static interface Operation {
		public List<Rule> mergeRules(Rule src, Rule dst, Pool pool);
	}
	
	public static Merge.Result compute(Rule src, Rule dst, int dstPosition, Pool pool) {
		return compute(new Merge.Request(src, dst, dstPosition), pool);
	}

	public static Merge.Result compute(Request req, Pool pool) {
		Atom term = req.dst.body.get(req.dstPosition);
		Map<Dob, Dob> unify = Unifier.unify(req.src.head.dob, term.dob);

		Result result = new Result(req);
		for (Map.Entry<Dob, Dob> entry : unify.entrySet()) {
			Dob key = entry.getKey();
			Dob val = entry.getValue();
			
			boolean keyIsVar = req.src.vars.contains(key);
			boolean valIsVar = req.dst.vars.contains(val);
			
			if (keyIsVar) {
				result.srcUnify.put(key, val);
			} else if (valIsVar) {
				result.dstUnify.put(val, key);
			} else {
				result = null;
				break;
			}
		}
		return result;
	}
	
	/**
	 * Construct the variables for the new rule. This set
	 * should contain all of the vars in the fixed source, 
	 * and all of the vars that were left untouched in the destination.
	 * @param merge
	 * @param srcFixed
	 * @param dstFixed
	 * @return
	 */
	public static Set<Dob> renderVars(Merge.Result merge,
			Rule srcFixed, Rule dstFixed) {
		Set<Dob> vars = Sets.newHashSet(dstFixed.vars);
		vars.removeAll(merge.dstUnify.keySet());
		vars.addAll(srcFixed.vars);
		return vars;
	}

}
