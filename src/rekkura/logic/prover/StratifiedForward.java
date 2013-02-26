package rekkura.logic.prover;

import java.util.Set;

import rekkura.logic.Ruletta;
import rekkura.model.Rule;

public class StratifiedForward {
	
	private Ruletta ruletta;
	
	public StratifiedForward(Set<Rule> rules) {
		ruletta.construct(rules);
	}
	
}
