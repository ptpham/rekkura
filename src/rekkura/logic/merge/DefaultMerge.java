package rekkura.logic.merge;

import java.util.List;

import rekkura.logic.Pool;
import rekkura.logic.Unifier;
import rekkura.logic.merge.Merge.Result;
import rekkura.model.Atom;
import rekkura.model.Rule;

import com.google.common.collect.Lists;

/**
 * A default merge will try to merge the head of the source with
 * each term in the destination. This class exposes a nicer context
 * in which one can do a single merge operation.
 * @author ptpham
 *
 */
public abstract class DefaultMerge implements Merge.Operation {

	/**
	 * In this context, a {@link Merge.Result} has already been
	 * computed on a source rule and a destination rule. The merge
	 * has been applied (fixed) to the source and the destination.
	 * A set of variables will be provided that represents the canonical
	 * variable merge (as computed in {@link Merge.renderVars}).
	 * @param merge
	 * @param srcFixed
	 * @param dstFixed
	 * @param vars
	 * @return
	 */
	public abstract List<Rule> generate(Merge.Result merge, Rule srcFixed, Rule dstFixed);
	
	@Override
	public List<Rule> mergeRules(Rule src, Rule dst, Pool pool) {
		List<Rule> result = Lists.newArrayList();
		
		for (int i = 0; i < dst.body.size(); i++) {
			Merge.Request req = new Merge.Request(src, dst, i);
			Merge.Result merge = Merge.compute(req, pool);
			if (merge == null) continue;
			
			// Apply the unifications to their respective rules
			Rule srcFixed = pool.rules.submerge(Unifier.replace(src, merge.srcUnify, merge.vars));
			Rule dstRaw = pool.rules.submerge(Unifier.replace(dst, merge.dstUnify, merge.vars));
			
			// Construct the body separately because the unification replace may have changed
			// the canonical ordering of the terms in the destination body. This is not ideal.
			List<Atom> fixedBody = Unifier.replace(dst, merge.dstUnify, merge.vars).body;
			Rule dstFixed = new Rule(dstRaw.head, fixedBody, dstRaw.vars, dstRaw.distinct);

			// Generate and submerge results
			List<Rule> generated = generate(merge, srcFixed, dstFixed);
			for (Rule rule : generated) {
				Rule submerged = pool.rules.submerge(rule);
				result.add(submerged);
			}
		}
		
		return result;
	}

	public static DefaultMerge combine(final DefaultMerge... operations) {
		return new DefaultMerge() {
			@Override public List<Rule> generate(Result merge, Rule srcFixed, Rule dstFixed) {
				
				List<Rule> result = Lists.newArrayList();
				for (DefaultMerge op : operations) {
					List<Rule> generated = op.generate(merge, srcFixed, dstFixed);
					result.addAll(generated);
				}
				return result;
			}
		};
	}
}
