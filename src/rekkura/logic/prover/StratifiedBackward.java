package rekkura.logic.prover;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.algorithm.Terra;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.state.algorithm.BackwardTraversal;
import rekkura.state.algorithm.Topper;
import rekkura.util.Colut;
import rekkura.util.OtmUtil;

import com.google.common.collect.*;

/**
 * This prover tries to do the minimum amount of work to 
 * prove everything that could possibly be proven from a 
 * given {@link Rule}.
 * @author ptpham
 *
 */
public abstract class StratifiedBackward extends StratifiedProver {
	
	private final BackwardTraversal<Rule, Dob> traversal;
	private final BackwardTraversal.Visitor<Rule, Dob> visitor;
	private final Multimap<Rule,Dob> processed = HashMultimap.create();
	
	/**
	 * This multimap stores the previous supports of recursive rules.
	 */
	private final Map<Rule,Multimap<Atom,Dob>> previous = Maps.newHashMap();
	private final Set<Rule> recursives;
	
	public StratifiedBackward(Collection<Rule> rules) {
		super(rules);
		this.visitor = createVisitor();
		this.traversal = new BackwardTraversal<Rule,Dob>(visitor, this.rta.ruleToGenRule);
		this.recursives = Sets.newHashSet(Colut.flatten(Topper.stronglyConnected(rta.ruleToGenRule)));
		clear();
	}
	
	protected abstract BackwardTraversal.Visitor<Rule, Dob> createVisitor();
	
	public void clear() {
		this.truths.clear();
		this.previous.clear();
		this.processed.clear();
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
		List<Dob> spine = this.cachet.spines.get(dob);
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
	
	protected Set<Dob> standardRuleExpansion(Rule rule) {
		ListMultimap<Atom, Dob> support = Terra.getBodySpace(rule, cachet);
		Set<Dob> generated = expandRecursiveRule(rule, support);
		if (generated == null) generated = expandRule(rule, truths, support, pool);
		for (Dob dob : generated) preserveTruth(dob);
		return generated;
	}
	
	/**
	 * This method performs a more efficient recursive expansion by diffing the 
	 * previous support with the current one in each term.
	 * @param rule
	 * @param current
	 * @return
	 */
	protected Set<Dob> expandRecursiveRule(Rule rule, ListMultimap<Atom,Dob> current) {
		if (!this.recursives.contains(rule)) return null;
		Multimap<Atom,Dob> old = this.previous.get(rule);
		
		Set<Dob> generated = null;
		if (old != null) {
			generated = Sets.newHashSet();
			for (Atom atom : current.keySet()) {
				List<Atom> selected = Lists.newArrayList(atom);
				Multimap<Atom,Dob> diff = OtmUtil.diffSelective(selected, current, old);
				generated.addAll(expandRule(rule, truths, diff, pool));
			}
		}
		this.previous.put(rule, HashMultimap.create(current));
		return generated;
	}

	public static class Standard extends StratifiedBackward {
		public Standard(Collection<Rule> rules) { super(rules); }

		@Override
		protected BackwardTraversal.Visitor<Rule, Dob> createVisitor() {
			return new BackwardTraversal.Visitor<Rule, Dob>() {
				@Override public Set<Dob> expandNode(Rule rule) 
				{ return standardRuleExpansion(rule); }
			};
		}
	}
}
