package rekkura.logic.algorithms;

import java.util.List;
import java.util.Set;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.util.Colut;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * This class holds static instances of various merge operations.
 * @author ptpham
 *
 */
public class Merges {
	public static final PositiveSubstitution posSub = new PositiveSubstitution();
	public static final NegationSplit negSplit = new NegationSplit();
	
	public static final Merge.Operation defaultMerge = Merge.combine(posSub, negSplit);
	
	/**
	 * The truth merge will only expand merges such that the destination
	 * position is positive.
	 * @author ptpham
	 *
	 */
	public static class PositiveSubstitution implements Merge.Operation {
		private PositiveSubstitution() { }

		public List<Rule> mergeRules(Merge.Application app) {
			List<Rule> result = Lists.newArrayList();
			Merge.Result merge = app.merge;
			Rule srcFixed = app.srcFixed;
			Rule dstFixed = app.dstFixed;
			if (!merge.getPivot().truth) return result;
			
			List<Atom> body = Colut.filterAt(dstFixed.body, merge.request.dstPosition);
			body.addAll(srcFixed.body);
			
			result.add(new Rule(dstFixed.head, body, merge.vars,
				Iterables.concat(srcFixed.distinct, dstFixed.distinct)));
			
			return result;
		}
	}
	
	/**
	 * This merge generates "first order approximation" rules in which
	 * negations split source bodies into negations of their constituent 
	 * terms. This merge will only generate rules using terms in the
	 * source rule if all variables in the term are contained in the
	 * source head.
	 * @author ptpham
	 *
	 */
	public static class NegationSplit implements Merge.Operation {
		private NegationSplit() { }

		public List<Rule> mergeRules(Merge.Application app) {
			List<Rule> result = Lists.newArrayList();
			Merge.Result merge = app.merge;
			Rule srcFixed = app.srcFixed;
			Rule dstFixed = app.dstFixed;
			if (merge.getPivot().truth) return result;

			Set<Dob> srcHeadVars = srcFixed.getVariablesOf(srcFixed.head.dob);
			for (Atom term : srcFixed.body) {
				Set<Dob> termVars = srcFixed.getVariablesOf(term.dob);
				if (!srcHeadVars.containsAll(termVars)) continue;
				
				List<Atom> body = Colut.filterAt(dstFixed.body, merge.request.dstPosition);
				body.add(new Atom(term.dob, !term.truth));
				result.add(new Rule(dstFixed.head, body, merge.vars,
						Iterables.concat(srcFixed.distinct, dstFixed.distinct)));
			}
			
			return result;
		}
	}

}
