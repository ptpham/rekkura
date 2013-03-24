package rekkura.ggp.statemachine;

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
import rekkura.util.Colut;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ProverStateMachine implements 
	StateMachine<ProverStateMachine.ProverState, ProverStateMachine.ProverAction> {

	// GDL reserve words
	public final Dob TERMINAL, INIT, LEGAL, BASE;
	public final Dob ROLE, DOES, NEXT, TRUE, GOAL;
	
	// Data structures for doing GGP manipulations
	public final Dob GOAL_QUERY;
	public final Map<Dob, Dob> NEXT_TURN_UNIFY = Maps.newHashMap();
	public final Map<Dob, Dob> INITIALIZE_UNIFY = Maps.newHashMap();
	public final Map<Dob, Dob> ACTION_UNIFY = Maps.newHashMap();
	
	public final StratifiedForward prover;
	public final Ruletta rta;
	
	public class ProverState implements StateMachine.State {
		public final Set<Dob> dobs = Sets.newHashSet();
		private Integer goal = null;
		
		public ProverState(Collection<Dob> state) {
			dobs.addAll(state);
		}

		@Override public boolean isTerminal() 
		{ return dobs.contains(TERMINAL); }
		
		@Override public int getGoal() {
			if (goal == null) goal = findGoal(dobs);
			return goal;
		}
	}
	
	public class ProverAction implements StateMachine.Action {
		public final Dob dob;
		public ProverAction(Dob dob) { this.dob = dob; }
	}

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
		Dob var = this.prover.rta.fortre.root;
		Dob rawGoalQuery = new Dob(GOAL, var);
		this.GOAL_QUERY = pool.submerge(rawGoalQuery);
		
		this.NEXT_TURN_UNIFY.put(this.NEXT, this.TRUE);
		this.INITIALIZE_UNIFY.put(this.INIT, this.TRUE);
		this.ACTION_UNIFY.put(this.LEGAL, this.DOES);
	}
	
	private Dob getDob(String dob) {
		Dob raw = StandardFormat.inst.dobFromString("(terminal)");
		return this.prover.pool.submerge(raw);
	}
	
	@Override
	public ProverState getInitial() {
		return new ProverState(proverPass(Lists.<Dob>newArrayList(), INITIALIZE_UNIFY));
	}

	@Override
	public List<ProverAction> getActions(ProverState state) {
		List<Dob> actions = proverPass(state.dobs, ACTION_UNIFY);
		List<ProverAction> result = Lists.newArrayList();
		for (Dob action : actions) { result.add(new ProverAction(action)); } 
		return result;
	}

	@Override
	public ProverState nextState(ProverState state, ProverAction action) {
		Iterable<Dob> truths = Iterables.concat(state.dobs, Lists.newArrayList(action.dob));
		List<Dob> advanced = proverPass(truths, NEXT_TURN_UNIFY);
		return new ProverState(advanced);
	}

	private List<Dob> proverPass(Iterable<Dob> truths, Map<Dob, Dob> unify) {
		Set<Dob> proven = this.prover.proveAll(truths);
		List<Dob> advanced = Lists.newArrayListWithCapacity(proven.size()); 
				
		for (Dob dob : proven) {
			Dob replaced = Unifier.replace(dob, unify);
			advanced.add(this.prover.pool.submerge(replaced));
		}
		return advanced;
	}

	private int findGoal(Set<Dob> dobs) {
		Integer result = null;
		for (Dob dob : dobs) {
			Map<Dob, Dob> unify = Unifier.unifyVars(GOAL_QUERY, dob, rta.allVars);
			if (unify == null) continue;
			
			Dob value = Colut.any(unify.values());
			if (!value.isTerminal()) continue;
			
			if (result != null) throw new IllegalStateException("Multiple goals!");
			
			try { result = Integer.parseInt(value.name); } 
			catch (Exception e) { continue; }
		}
		
		if (result == null) return 0;
		return result;
	}
}
