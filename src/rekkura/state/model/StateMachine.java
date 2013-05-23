package rekkura.state.model;

import java.util.Map;

import rekkura.logic.model.Dob;

import com.google.common.collect.ListMultimap;

public interface StateMachine<S, A> {
	public S getInitial();
	public ListMultimap<Dob, A> getActions(S state);
	public S nextState(S state, Map<Dob, A> actions);
	public boolean isTerminal(S state);
	public Map<Dob, Integer> getGoals(S state);
}
