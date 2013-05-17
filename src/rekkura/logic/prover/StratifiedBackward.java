package rekkura.logic.prover;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.state.algorithms.Topper;
import rekkura.util.OtmUtil;

import com.google.common.collect.HashMultimap;
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

	/**
	 * The prover will not expand any rule that has non-zero entries here.
	 */
	private final HashMultimap<Rule, Dob> known = HashMultimap.create();
	
	private final HashMultimap<Rule, Dob> pending = HashMultimap.create();
	
	/**
	 * This multiset gives the index of the strongly connected component
	 * the rule belongs to.
	 */
	private final Map<Rule, Integer> indices = Maps.newHashMap();
	
	/**
	 * This maintains the strongly connected component sets. This
	 * set does not include rules that are not in a strongly connected
	 * component.
	 */
	private final List<Set<Rule>> components;
	
	/**
	 * This array is used to coordinate across a strongly connected
	 * component. The first node reached in the component becomes the 
	 * root of the component and continues to ask into the component
	 * as long as new dobs are being generated.
	 */
	private final boolean[] rooted;
	private final Set<Rule> asking = Sets.newHashSet();
	
	public StratifiedBackward(Collection<Rule> rules) {
		super(rules);
		
		components = Topper.stronglyConnected(this.rta.ruleToGenRule);
		for (int i = 0; i < components.size(); i++) {
			Set<Rule> cycle = components.get(i);
			for (Rule rule : cycle) indices.put(rule, i);
		}
		
		rooted = new boolean[components.size()];
		
		clear();
	}
	
	public void clear() {
		this.truths.clear();
		this.cachet.unisuccess.clear();
		this.pending.clear();
		this.asking.clear();
		this.known.clear();
		
		Arrays.fill(rooted, false);
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
		known.putAll(addition);
	}
	
	public Set<Dob> getKnown(Rule rule) { return this.known.get(rule); }

	/**
	 * Returns the set of 
	 * @param dob
	 * @return
	 */
	public Set<Dob> ask(Dob dob) {
		List<Dob> spine = this.cachet.canonicalSpines.get(dob);
		Iterable<Rule> rules = OtmUtil.valueIterable(this.rta.headToRule, spine);

		Set<Dob> result = Sets.newHashSet();
		for (Rule rule : rules) ask(rule, result);
		return result;
	}
	
	@Override
	public Set<Dob> proveAll(Iterable<Dob> truths) {
		this.clear();
		this.preserveTruths(truths);
		
		Set<Dob> result = Sets.newHashSet();
		for (Rule rule : this.rta.allRules) ask(rule, result);
		
		return result;
	}
	
	private boolean ask(Rule rule, Set<Dob> result) {
		if (known.containsKey(rule)) {
			result.addAll(known.get(rule));
			return false;
		}
		
		boolean inComponent = this.indices.containsKey(rule);
		if (inComponent) {
			int index = this.indices.get(rule);
			return expandComponentRule(rule, result, index);
		} else return expandRuleToMap(rule, result, this.known);
	}

	private boolean expandComponentRule(Rule rule, Set<Dob> result, int index) {
		boolean root = !this.rooted[index];
		if (!this.asking.add(rule)) return false;
		
		boolean expanded = false;
		if (root) expanded = expandRuleAsRoot(rule, result, index);
		else expanded = expandRuleToMap(rule, result, this.pending);
		
		this.asking.remove(rule);
		return expanded;
	}

	/**
	 * The root (the first node reached in this strongly connected
	 * component) acts as the base for a loop that continues
	 * as long as new dobs are being generated. Once everything 
	 * has been generated, the dobs in pending are move to known
	 * for the entire component.
	 * @param rule
	 * @param result
	 * @param index
	 * @return
	 */
	private boolean expandRuleAsRoot(Rule rule, Set<Dob> result, int index) {
		boolean expanded = false;

		this.rooted[index] = true;
		while (true) {
			boolean current = expandRuleToMap(rule, result, this.pending);
			expanded |= current;
			if (!current) break;
		}
		this.rooted[index] = false;
		
		for (Rule node : this.components.get(index)) {
			this.known.putAll(node, this.pending.get(node));
			this.pending.removeAll(node);
		}
		
		return expanded;
	}

	private boolean expandRuleToMap(Rule rule, Set<Dob> result, Multimap<Rule, Dob> map) {
		boolean expanded = false;
		for (Rule parent : this.rta.ruleToGenRule.get(rule)) {
			expanded |= ask(parent, result);
		}

		Set<Dob> generated = expandRule(rule);
		map.putAll(rule, generated);
		for (Dob dob : generated) this.preserveTruth(dob);
		expanded |= result.addAll(generated);
		
		return expanded;
	}
}
