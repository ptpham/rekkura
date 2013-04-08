package rekkura.model;

import java.util.Map;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;

public interface StateMachine<S, A> {
	public S getInitial();
	public ListMultimap<Dob, A> getActions(S state);
	public S nextState(S state, Map<Dob, A> actions);
	public boolean isTerminal(S state);
	public Multiset<Dob> getGoals(S state);
}
