package rekkura.logic.prover;

import java.util.Collection;
import java.util.Set;

import rekkura.model.Dob;
import rekkura.model.Rule;

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

	public Set<Dob> ask(Dob dob) {
		Dob form = this.cachet.canonicalForms.get(dob);
		Collection<Rule> rules = this.rta.headToRule.get(form);
		
		Set<Dob> result = Sets.newHashSet();
		for (Rule rule : rules) ask(rule, result);
		return result;
	}
	
	@Override
	public Set<Dob> proveAll(Iterable<Dob> truths) {
		this.clear();
		for (Dob truth : truths) storeTruth(truth);
		
		Set<Dob> result = Sets.newHashSet();
		for (Rule rule : this.rta.allRules) ask(rule, result);
		
		return result;
	}

	private void ask(Rule rule, Set<Dob> result) {
		if (known.containsKey(rule)) return;
		
		for (Rule parent : this.rta.ruleToGenRule.get(rule)) {
			ask(parent, result);
		}
		
		Set<Dob> generated = expandRule(rule);
		this.known.putAll(rule, generated);
		for (Dob dob : generated) this.storeTruth(dob);
		result.addAll(generated);
	}
}
