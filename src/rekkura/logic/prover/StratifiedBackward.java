package rekkura.logic.prover;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.algorithm.Renderer;
import rekkura.logic.algorithm.Terra;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.state.algorithm.BackwardTraversal;
import rekkura.util.OtmUtil;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
	public final BackwardTraversal<Rule, Dob> traversal;
	private final BackwardTraversal.Visitor<Rule, Dob> visitor;
	
	/**
	 * This multimap stores the previous supports of recursive rules.
	 */
	private final Map<Rule,Multimap<Atom,Dob>> previous = Maps.newHashMap();
	
	public StratifiedBackward(Collection<Rule> rules) {
		super(rules);
		this.visitor = createVisitor();
		this.traversal = new BackwardTraversal<Rule,Dob>(visitor, this.rta.ruleToGenRule);
		clear();
	}
	
	protected BackwardTraversal.Visitor<Rule, Dob> createVisitor() {
		return new BackwardTraversal.Visitor<Rule, Dob>() {
			@Override public Set<Dob> expandNode(Rule rule) 
			{ return standardRuleExpansion(rule); }
		};
	}
	
	public void clear() {
		this.truths.clear();
		this.previous.clear();
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
	public void preserveAndPutKnown(Multimap<Rule, Dob> addition) {
		preserveTruths(addition.values());
		traversal.known.putAll(addition);
	}
	
	/**
	 * Ask all rules with a head that potentially generates the given
	 * (grounded or ungrounded) dob, and return the union of the results.
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
		return askAll();
	}

	public Set<Dob> askAll() {
		Set<Dob> result = Sets.newHashSet();
		for (Rule rule : this.rta.allRules) this.traversal.ask(rule, result);
		return result;
	}
	
	protected Set<Dob> standardRuleExpansion(Rule rule) {
		ListMultimap<Atom, Dob> raw = cachet.getSupport(rule);
		List<Multimap<Atom,Dob>> supports = Lists.newArrayList();
		
		// Replace support with a diffed version if this rule has
		// been expanded before.
		if (this.previous.containsKey(rule)) {
			Multimap<Atom,Dob> old = this.previous.get(rule);
			Multimap<Atom,Dob> current = raw;
			for (Atom atom : current.keySet()) {
				List<Atom> selected = Lists.newArrayList(atom);
				Multimap<Atom,Dob> diff = OtmUtil.diffSelective(selected, current, old);
				supports.add(diff);
			}
		} else supports.add(raw);
		
		// Generate using the requested supports
		Set<Dob> generated = Sets.newHashSet();
		Renderer renderer = this.renderers.get(rule);
		for (Multimap<Atom,Dob> support : supports) {
			List<Map<Dob,Dob>> unifies = renderer.apply(rule, truths, support, pool);
			generated.addAll(Terra.renderHeads(unifies, rule, pool));
		}
		
		// Store the current support in previous so that we can do
		// a selective diff next time we see this rule.
		this.previous.put(rule, raw);
		for (Dob dob : generated) preserveTruth(dob);
		return generated;
	}
}
