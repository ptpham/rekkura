package rekkura.ggp.machina;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.Pool;
import rekkura.logic.prover.StratifiedBackward;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public class BackwardStateMachine extends GameLogicContext implements 
	StateMachine<Set<Dob>, Dob> {

	public final StratifiedBackward prover;
	
	private final Pool pool;
	
	/**
	 * This is for the purpose of coalescing inferences
	 * on the same turn.
	 */
	private Set<Dob> last;
	
	public BackwardStateMachine(StratifiedBackward prover) {
		super(prover.pool, prover.rta);
		this.prover = prover;
		this.pool = prover.pool;
	}

	@Override
	public Set<Dob> getInitial() {
		return extractTrues(proverPass(EMPTY_STATE, INIT_QUERY, INIT_UNIFY));
	}

	@Override
	public Multimap<Dob, Dob> getActions(Set<Dob> state) {
		return extractActions(proverPass(state, LEGAL_QUERY, LEGAL_UNIFY));
	}

	@Override
	public Set<Dob> nextState(Set<Dob> state, Map<Dob, Dob> actions) {
		return extractTrues(proverPass(state, NEXT_QUERY, NEXT_UNIFY));

	}

	@Override
	public boolean isTerminal(Set<Dob> state) {
		return proverPass(state, TERMINAL, EMTPY_UNIFY).contains(TERMINAL);
	}

	@Override
	public Multiset<Dob> getGoals(Set<Dob> state) {
		return extractGoals(proverPass(state, GOAL_QUERY, EMTPY_UNIFY));
	}

	public static BackwardStateMachine createForRules(Collection<Rule> rules) {
		return new BackwardStateMachine(new StratifiedBackward(rules));
	}
	
	private Set<Dob> proverPass(Set<Dob> state, Dob query, Map<Dob, Dob> unify) {
		if (state != last) {
			prover.clear();
			prover.storeTruths(state);
			last = state;
		}
		
		Set<Dob> proven = prover.ask(query);
		Set<Dob> submerged = ProverStateMachine.submersiveReplace(proven, unify, pool);
		return submerged;
	}	
	
}
