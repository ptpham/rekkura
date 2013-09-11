package rekkura.logic.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Pool;

import com.google.common.collect.Lists;
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
	
	/**
	 * A Merge.Result represents the raw data structures obtained
	 * from a Merge.Request.
	 * @author ptpham
	 *
	 */
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
	
	/**
	 * A Merge.Application represents the transformation of
	 * the source and the destination according to the given
	 * Merge.Result.
	 * @author ptpham
	 *
	 */
	public static class Application {
		public final Rule srcFixed, dstFixed;
		public final Merge.Result merge;
		public Application(Rule srcFixed, Rule dstFixed, Merge.Result merge) {
			this.srcFixed = srcFixed;
			this.dstFixed = dstFixed;
			this.merge = merge;
		}
	}
	
	/**
	 * A merge operation uses an application to generate new rules.
	 * @author ptpham
	 *
	 */
	public static interface Operation {
		public List<Rule> mergeRules(Merge.Application app);
	}
	
	public static Merge.Result compute(Rule src, Rule dst, int dstPosition, Pool pool) {
		return compute(new Merge.Request(src, dst, dstPosition), pool);
	}

	public static Merge.Result compute(Request req, Pool pool) {
		Atom term = req.dst.body.get(req.dstPosition);
		Map<Dob, Dob> unify = Unifier.unify(req.src.head.dob, term.dob);
		if (unify == null) return null;
		
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
	
	public static Merge.Application apply(Merge.Result merge, Pool pool) {
		if (merge == null) return null;
		Rule src = merge.request.src;
		Rule dst = merge.request.dst;
		
		// Apply the unifications to their respective rules
		Rule srcFixed = pool.rules.submerge(Unifier.replace(src, merge.srcUnify, merge.vars));
		Rule dstRaw = pool.rules.submerge(Unifier.replace(dst, merge.dstUnify, merge.vars));
		
		// Construct the body separately because the unification replace may have changed
		// the canonical ordering of the terms in the destination body. This is not ideal.
		List<Atom> fixedBody = Unifier.replace(dst, merge.dstUnify, merge.vars).body;
		Rule dstFixed = new Rule(dstRaw.head, fixedBody, dstRaw.vars, dstRaw.distinct);

		return new Merge.Application(srcFixed, dstFixed, merge);
	}
	
	public static List<Application> applyOverBody(Rule src, Rule dst, Pool pool) {
		List<Application> result = Lists.newArrayList();
		
		for (int i = 0; i < dst.body.size(); i++) {
			Merge.Request req = new Merge.Request(src, dst, i);
			Merge.Application app = Merge.apply(Merge.compute(req, pool), pool);
			if (app != null) result.add(app);
		}
		
		return result;
	}
	
	public static List<Rule> applyOperation(Rule src, Rule dst, Merge.Operation op, Pool pool) {
		List<Rule> result = Lists.newArrayList();
		List<Application> apps = applyOverBody(src, dst, pool);
		
		for (Application app : apps) {
			List<Rule> generated = op.mergeRules(app);
			for (Rule rule : generated) {
				Rule submerged = pool.rules.submerge(rule);
				result.add(submerged);
			}
		}

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
					Dob safe = pool.vargen.request(vars);
					merge.srcUnify.put(var, safe);
					vars.add(safe);
				} else vars.add(var);
			}
		}
	}

	public static Merge.Operation combine(final Merge.Operation... operations) {
		return new Merge.Operation() {
			@Override public List<Rule> mergeRules(Merge.Application app) {
				List<Rule> result = Lists.newArrayList();
				for (Merge.Operation op : operations) {
					List<Rule> generated = op.mergeRules(app);
					result.addAll(generated);
				}
				return result;
			}
		};
	}
}
