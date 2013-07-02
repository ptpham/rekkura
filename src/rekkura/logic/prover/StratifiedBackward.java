package rekkura.logic.prover;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.state.algorithm.BackwardTraversal;
import rekkura.util.OtmUtil;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * This prover tries to do the minimum amount of work to 
 * prove everything that could possibly be proven from a 
 * given {@link Rule}.
 * @author ptpham
 *
 */
public class StratifiedBackward extends StratifiedProver {
	
	private final BackwardTraversal<Rule, Dob> traversal;
	private final BackwardTraversal.Visitor<Rule, Dob> visitor;
	
	public StratifiedBackward(Collection<Rule> rules) {
		super(rules);
		this.visitor = createVisitor();
		this.traversal = new BackwardTraversal<Rule,Dob>(visitor, this.rta.ruleToGenRule);
		clear();
	}
	
	private BackwardTraversal.Visitor<Rule, Dob> createVisitor() {
		return new BackwardTraversal.Visitor<Rule, Dob>() {
			@Override
			public Set<Dob> expandNode(Rule rule) {
				Set<Dob> generated = expandRule(rule);
				for (Dob dob : generated) preserveTruth(dob);
				return generated;
			}
		};
	}

	public void clear() {
		this.truths.clear();
		this.cachet.formToGrounds.clear();
		this.traversal.clear();
	}
	
	/**
	 * By establishing the "known" dobs generated from a given rule,
	 * the rule will never be expanded and it will be assumed that
	 * the union of the dob sets provided since the last {@code clear} consist
	 * of the entirety of the dobs that could have been generated from
	 * the given rule.
	 * @param addition
	 */
	public void putKnown(Multimap<Rule, Dob> addition) {
		preserveTruths(addition.values());
		traversal.known.putAll(addition);
	}
	
	public Set<Dob> getKnown(Rule rule) { return this.traversal.known.get(rule); }

	/**
	 * Returns the set of 
	 * @param dob
	 * @return
	 */
	public Set<Dob> ask(Dob dob) {
		List<Dob> spine = this.cachet.canonicalSpines.get(dob);
		Iterable<Rule> rules = OtmUtil.valueIterable(this.rta.headToRule, spine);

		Set<Dob> result = Sets.newHashSet();
		for (Rule rule : rules) this.traversal.ask(rule, result);
		return result;
	}
	
	@Override
	public Set<Dob> proveAll(Iterable<Dob> truths) {
		this.clear();
		this.preserveTruths(truths);
		
		Set<Dob> result = Sets.newHashSet();
		for (Rule rule : this.rta.allRules) this.traversal.ask(rule, result);
		
		return result;
	}
}
