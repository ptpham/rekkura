package rekkura.ggp.milleu;

import java.util.Collection;
import java.util.List;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Lists;

public class GameManip {

	public static List<Dob> getRoles(Collection<Rule> rules) {
		List<Dob> roles = Lists.newArrayList();
		
		for (Rule rule : rules) {
			if (rule.body.size() != 0 || rule.vars.size() != 0) continue;
			
			Atom head = rule.head;
			if (!head.truth || head.dob.size() != 2) continue;
			
			Dob first = head.dob.at(0);
			if (!first.isTerminal()) continue;
			if (!first.name.equals("role")) continue;
			
			roles.add(head.dob.at(1));
		}
		
		return roles;
	}
	
}
