package rekkura.logic.merge;

import java.util.List;
import java.util.Set;

import rekkura.logic.merge.Merge.Result;
import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
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
	
	/**
	 * The truth merge will only expand merges such that the destination
	 * position is not a negation.
	 * @author ptpham
	 *
	 */
	public static class PositiveSubstitution extends DefaultMerge {
		private PositiveSubstitution() { }

		@Override
		public List<Rule> generate(Result merge, Rule srcFixed, Rule dstFixed) {
			List<Rule> result = Lists.newArrayList();
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
	public static class NegationSplit extends DefaultMerge {
		private NegationSplit() { }

		@Override
		public List<Rule> generate(Result merge, Rule srcFixed, Rule dstFixed) {
			List<Rule> result = Lists.newArrayList();
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
