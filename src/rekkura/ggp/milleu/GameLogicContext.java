package rekkura.ggp.milleu;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.fmt.StandardFormat;
import rekkura.logic.Pool;
import rekkura.logic.Ruletta;
import rekkura.logic.Unifier;
import rekkura.model.Dob;

import com.google.common.collect.*;

public class GameLogicContext {

	// GDL reserve words
	public final Dob TERMINAL, INIT, LEGAL, BASE;
	public final Dob ROLE, DOES, NEXT, TRUE, GOAL;
	
	// Data structures for doing GGP manipulations
	public final Dob GOAL_QUERY, GOAL_QUERY_ROLE_VAR;
	public final Map<Dob, Dob> NEXT_TURN_UNIFY = Maps.newHashMap();
	public final Map<Dob, Dob> INITIALIZE_UNIFY = Maps.newHashMap();
	public final Map<Dob, Dob> ACTION_UNIFY = Maps.newHashMap();
	public final Map<Dob, Dob> EMTPY_UNIFY = Maps.newHashMap();
	
	public final Pool pool;
	public final Ruletta rta;
	
	public GameLogicContext(Pool pool, Ruletta rta) {
		this.pool = pool;
		this.rta = rta;
		
		this.TERMINAL = getDob("(terminal)");
		this.BASE = getDob("(base)");
		this.TRUE = getDob("(true)");
		this.DOES = getDob("(does)");
		this.INIT = getDob("(init)");
		this.LEGAL = getDob("(legal)");
		this.NEXT = getDob("(next)");
		this.GOAL = getDob("(goal)");
		this.ROLE = getDob("(role)");

		List<Dob> var = rta.getVariables(2);
		this.GOAL_QUERY_ROLE_VAR = var.get(0);
		Dob rawGoalQuery = new Dob(GOAL, GOAL_QUERY_ROLE_VAR, var.get(1));
		this.GOAL_QUERY = pool.submerge(rawGoalQuery);
		
		this.NEXT_TURN_UNIFY.put(this.NEXT, this.TRUE);
		this.INITIALIZE_UNIFY.put(this.INIT, this.TRUE);
		this.ACTION_UNIFY.put(this.LEGAL, this.DOES);
	}

	private Dob getDob(String dob) {
		Dob raw = StandardFormat.inst.dobFromString(dob);
		return this.pool.submerge(raw);
	}
	

	public Multiset<Dob> extractGoals(Set<Dob> dobs) {
		Multiset<Dob> result = HashMultiset.create();
		for (Dob dob : dobs) {
			Map<Dob, Dob> unify = Unifier.unifyVars(GOAL_QUERY, dob, rta.allVars);
			if (unify == null) continue;
			
			Dob role = null;
			Dob value = null;
			if (unify.entrySet().size() != 2) continue;
			for (Map.Entry<Dob, Dob> entry : unify.entrySet()) {
				if (entry.getKey() == GOAL_QUERY_ROLE_VAR) role = entry.getValue();
				else if (value != null) value = entry.getValue();
			}
			
			if (!value.isTerminal()) continue;			
			
			int goal = 0;
			try { goal = Integer.parseInt(value.name); } 
			catch (Exception e) { continue; }
			result.add(role, goal);
		}
		
		return result;
	}
	
	public Set<Dob> extractTrues(Set<Dob> dobs) {
		Set<Dob> result = Sets.newHashSetWithExpectedSize(dobs.size());
		for (Dob dob : dobs) {
			boolean keep = !dob.isTerminal() 
				&& dob.size() > 0 && dob.at(0) == this.TRUE;
			if (keep) result.add(dob);
		}
		return result;
	}
	
	public Multimap<Dob, Dob> extractActions(Set<Dob> truths) {
		Multimap<Dob, Dob> result = HashMultimap.create();
		for (Dob dob : truths) {
			if (dob.size() < 3) continue;
			if (dob.at(0) != this.DOES) continue;
			result.put(dob.at(1), dob);
		}
		return result;
	}
}
