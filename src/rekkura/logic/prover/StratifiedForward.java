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
import rekkura.util.Colut;
import rekkura.util.OTMUtil;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class StratifiedForward {
	
	public Ruletta rta;
	
	public Topper toper;
	public Map<Dob, List<Dob>> deps;

	public Pool pool;
	
	public Fortre fortre;
	
	protected Map<Dob, Set<Dob>> grounds;
	
	public StratifiedForward(Collection<Rule> rules) {
		Set<Rule> submerged = Sets.newHashSet();
		for (Rule rule : rules) { submerged.add(pool.submerge(rule)); }
		
		rta.construct(submerged);

		this.deps = toper.dependencies(rta.headToRule.keySet(), 
				rta.bodyToRule.keySet(), rta.allVars);
		
		this.fortre = new Fortre(rta.allVars);
		
		for (Dob dob : rta.bodyToRule.keySet()) { this.fortre.addDob(dob); }
		
		this.grounds = Maps.newHashMap();
	}

	public void addTruth(Dob dob) {
		
		List<Dob> trunk = this.fortre.getUnifyTrunk(dob);
		Dob end = Colut.end(trunk);
		if (!OTMUtil.contains(grounds, end, dob)) {
			OTMUtil.put(grounds, end, dob);
		}
		
		for (Dob node : trunk) {
			
		}
		
	}
	
	
}
