package rekkura.logic.prover;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.OTMUtil;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class StratifiedBackward extends StratifiedProver {

	/**
	 * The prover will not expand any rule that has non-zero entries here.
	 */
	public final Multimap<Rule, Dob> known = HashMultimap.create();
	
	public StratifiedBackward(Collection<Rule> rules) {
		super(rules);
		clear();
	}
	
	public void clear() {
		this.truths.clear();
		this.cachet.unisuccess.clear();
		this.known.clear();
		this.storeTruth(vacuous);
	}

	/**
	 * Returns the set of 
	 * @param dob
	 * @return
	 */
	public Set<Dob> ask(Dob dob) {
		List<Dob> spine = this.cachet.canonicalSpines.get(dob);
		Iterable<Rule> rules = OTMUtil.valueIterable(this.rta.headToRule, spine);
		
		Set<Dob> result = Sets.newHashSet();
		for (Rule rule : rules) ask(rule, result);
		return result;
	}
	
	@Override
	public Set<Dob> proveAll(Iterable<Dob> truths) {
		this.clear();
		this.storeTruths(truths);
		
		Set<Dob> result = Sets.newHashSet();
		for (Rule rule : this.rta.allRules) ask(rule, result);
		
		return result;
	}

	private void ask(Rule rule, Set<Dob> result) {
		if (known.containsKey(rule)) {
			result.addAll(known.get(rule));
			return;
		}
		
		for (Rule parent : this.rta.ruleToGenRule.get(rule)) {
			ask(parent, result);
		}
		
		Set<Dob> generated = expandRule(rule);
		this.known.putAll(rule, generated);
		for (Dob dob : generated) this.storeTruth(dob);
		result.addAll(generated);
	}
}
