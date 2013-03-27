package rekkura.ggp.machina;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.fmt.StandardFormat;
import rekkura.logic.Pool;
import rekkura.logic.Ruletta;
import rekkura.logic.Unifier;
import rekkura.logic.prover.StratifiedForward;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.*;

public class ProverStateMachine implements 
	StateMachine<Set<Dob>, Dob> {

	// GDL reserve words
	public final Dob TERMINAL, INIT, LEGAL, BASE;
	public final Dob ROLE, DOES, NEXT, TRUE, GOAL;
	
	// Data structures for doing GGP manipulations
	public final Dob GOAL_QUERY, GOAL_QUERY_ROLE_VAR;
	public final Map<Dob, Dob> NEXT_TURN_UNIFY = Maps.newHashMap();
	public final Map<Dob, Dob> INITIALIZE_UNIFY = Maps.newHashMap();
	public final Map<Dob, Dob> ACTION_UNIFY = Maps.newHashMap();
	public final Map<Dob, Dob> EMTPY_UNIFY = Maps.newHashMap();
	
	public final StratifiedForward prover;
	public final Ruletta rta;
	
	public ProverStateMachine(Collection<Rule> rules) {
		this.prover = new StratifiedForward(rules);
		this.rta = prover.rta;
		
		this.TERMINAL = getDob("(terminal)");
		this.BASE = getDob("(base)");
		this.TRUE = getDob("(true)");
		this.DOES = getDob("(does)");
		this.INIT = getDob("(init)");
		this.LEGAL = getDob("(legal)");
		this.NEXT = getDob("(next)");
		this.GOAL = getDob("(goal)");
		this.ROLE = getDob("(role)");

		Pool pool = this.prover.pool;
		List<Dob> var = this.prover.rta.getVariables(2);
		this.GOAL_QUERY_ROLE_VAR = var.get(0);
		Dob rawGoalQuery = new Dob(GOAL, GOAL_QUERY_ROLE_VAR, var.get(1));
		this.GOAL_QUERY = pool.submerge(rawGoalQuery);
		
		this.NEXT_TURN_UNIFY.put(this.NEXT, this.TRUE);
		this.INITIALIZE_UNIFY.put(this.INIT, this.TRUE);
		this.ACTION_UNIFY.put(this.LEGAL, this.DOES);
	}
	
	private Dob getDob(String dob) {
		Dob raw = StandardFormat.inst.dobFromString(dob);
		return this.prover.pool.submerge(raw);
	}
	
	@Override
	public Set<Dob> getInitial() {
		return proverPass(Lists.<Dob>newArrayList(), INITIALIZE_UNIFY);
	}

	@Override public boolean isTerminal(Set<Dob> dobs) {
		return this.proverPass(dobs, EMTPY_UNIFY).contains(TERMINAL);
	}
	

	@Override
	public Multimap<Dob, Dob> getActions(Set<Dob> state) {
		Set<Dob> actions = proverPass(state, ACTION_UNIFY);
		Multimap<Dob, Dob> result = HashMultimap.create();
		for (Dob dob : actions) {
			if (dob.size() < 3) continue;
			if (dob.at(0) != this.DOES) continue;
			result.put(dob.at(1), dob);
		}
		return result;
	}

	@Override
	public Set<Dob> nextState(Set<Dob> state, Map<Dob, Dob> actions) {
		Iterable<Dob> truths = Iterables.concat(state, actions.values());
		return proverPass(truths, NEXT_TURN_UNIFY);
	}

	@Override
	public Multiset<Dob> getGoals(Set<Dob> dobs) {
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

	private Set<Dob> proverPass(Iterable<Dob> truths, Map<Dob, Dob> unify) {
		Set<Dob> proven = this.prover.proveAll(truths);
		Iterables.addAll(proven, truths);
		
		Set<Dob> advanced = Sets.newHashSetWithExpectedSize(proven.size()); 
		for (Dob dob : proven) {
			Dob replaced = Unifier.replace(dob, unify);
			if (replaced != dob) replaced = this.prover.pool.submerge(replaced);
			advanced.add(replaced);
		}
		return advanced;
	}

}
