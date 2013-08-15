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

import com.google.common.collect.Multimap;

public class Expansion {
	public static Set<Dob> standard(Rule rule,
		Set<Dob> truths, Multimap<Atom,Dob> support, Pool pool) {
		List<Atom> expanders = Terra.getGreedyVarCoverExpanders(rule, support);
		List<Atom> check = Colut.deselect(rule.body, expanders);
		Cartesian.AdvancingIterator<Unification> iterator =
			Terra.getBodyUnifications(rule, expanders, support, truths);
	
		List<Map<Dob,Dob>> unifies = Terra.applyUnifications(rule, iterator, check, pool, truths);
		return Terra.renderHeads(unifies, rule, pool);
	}
}
