package rekkura.logic;

import java.util.Map;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Maps;

/**
 * This class is responsible for computing the data structures
 * relevant to the expansion of a term in a rule (dst) given
 * another rule (src).
 * @author ptpham
 *
 */
public class Merger {
	public static class Request {
		final Rule src, dst;
		final int dstPosition;
		public Request(Rule src, Rule dst, int dstPosition) {
			this.dstPosition = dstPosition;
			this.src = src;
			this.dst = dst;
		}
	}
	
	public static class Result {
		public final Map<Dob, Dob> srcUnify = Maps.newHashMap();
		public final Map<Dob, Dob> dstUnify = Maps.newHashMap();
		public final Request request;
		
		public Result(Request request) {
			this.request = request;
		}
	}
	
	public static Merger.Result compute(Rule src, Rule dst, int dstPosition, Pool pool) {
		return compute(new Merger.Request(src, dst, dstPosition), pool);
	}

	public static Merger.Result compute(Request req, Pool pool) {
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
}
