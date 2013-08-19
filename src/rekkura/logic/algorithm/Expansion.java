
package rekkura.logic.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.model.Unification;
import rekkura.logic.structure.Pool;
import rekkura.util.Cartesian;
import rekkura.util.Colut;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public abstract class Expansion {
	
	public abstract Set<Dob> expand(Rule rule, Set<Dob> truths, Multimap<Atom,Dob> support, Pool pool);
	
	public static Standard getStandard() { return new Standard(); }
	
	public static class Standard extends Expansion {
		@Override public Set<Dob> expand(Rule rule,
			Set<Dob> truths, Multimap<Atom, Dob> support, Pool pool) {
			return standard(rule, truths, support, pool);
		}
	}
	
	public static Multimap<Atom,Dob> getTrivialSupport(Rule rule, Set<Dob> truths) {
		Multimap<Atom,Dob> result = HashMultimap.create();
		for (Atom atom : rule.body) {
			if (!atom.truth) continue;
			result.putAll(atom, truths);
		}
		return result;
	}
	
	public static Set<Dob> standard(Rule rule, Set<Dob> truths, Multimap<Atom,Dob> support, Pool pool) {
		List<Atom> expanders = Terra.getGreedyVarCoverExpanders(rule, support);
		List<Atom> check = Colut.deselect(rule.body, expanders);
		Cartesian.AdvancingIterator<Unification> iterator =
			Terra.getUnificationIterator(rule, expanders, support, truths);
		return applyAndRender(rule, iterator, check, pool, truths);
	}
	
	
	protected static Set<Dob> applyAndRender(Rule rule, Cartesian.AdvancingIterator<Unification> iterator,
		List<Atom> check, Pool pool, Set<Dob> truths) {
		List<Map<Dob,Dob>> unifies = Terra.applyUnifications(rule, iterator, check, pool, truths);
		return Terra.renderHeads(unifies, rule, pool);
	}
}
