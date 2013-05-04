package rekkura.logic.merge;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.Pool;
import rekkura.logic.Unifier;
import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.model.Vars;

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
		public final Set<Dob> vars = Sets.newHashSet();
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
		
		renderVars(result, pool);
		return result;
	}
	
	/**
	 * Construct the variables for the new rule. This set should contain
	 * all variables in the fixed source, all variables that were left
	 * untouched in the destination, and new variables for the incoming
	 * source variables in case there is a conflict.
	 * @param merge
	 * @param srcFixed
	 * @param dstFixed
	 * @return
	 */
	private static void renderVars(Merge.Result merge, Pool pool) {
		if (merge == null) return;
		Merge.Request req = merge.request;
		Set<Dob> vars = merge.vars;
		vars.addAll(req.dst.vars);
		
		// The destination unification is guaranteed to map only to 
		// non-variables by construction.
		vars.removeAll(merge.dstUnify.keySet());
		Set<Dob> srcHeadFlattened = Sets.newHashSet(req.src.head.dob.fullIterable());
		
		// If a variable in the source has not been accounted for in the
		// source unification, it needs to be disambiguated if the destination
		// has a copy of that variable.
		Set<Dob> srcTouched = merge.srcUnify.keySet();
		for (Dob var : req.src.vars) {
			if (!srcTouched.contains(var)) {
				if (vars.contains(var) && !srcHeadFlattened.contains(var)) {
					Dob safe = Vars.request(vars, pool.context);
					merge.srcUnify.put(var, safe);
					vars.add(safe);
				} else vars.add(var);
			}
		}
	}

}
