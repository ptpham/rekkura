package rekkura.logic.prover;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.Fortre;
import rekkura.logic.Pool;
import rekkura.logic.Ruletta;
import rekkura.logic.Topper;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Sets;

public class StratifiedForward {
	
	public Ruletta rta;
	
	public Topper toper;
	public Map<Dob, List<Dob>> deps;

	public Pool pool;
	
	public Fortre fortre;
	
	public StratifiedForward(Collection<Rule> rules) {
		Set<Rule> submerged = Sets.newHashSet();
		for (Rule rule : rules) { submerged.add(pool.submerge(rule)); }
		
		rta.construct(submerged);

		this.deps = toper.dependencies(rta.headToRule.keySet(), 
				rta.bodyToRule.keySet(), rta.allVars);
		
		this.fortre = new Fortre(rta.allVars);
		
		for (Dob dob : rta.bodyToRule.keySet()) { this.fortre.addDob(dob); }
	}
	
}
