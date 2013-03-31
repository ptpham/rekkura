package rekkura.model;

import java.util.Map;


import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public interface StateMachine<S, A> {
	public S getInitial();
	public Multimap<Dob, A> getActions(S state);
	public S nextState(S state, Map<Dob, A> actions);
	public boolean isTerminal(S state);
	public Multiset<Dob> getGoals(S state);
}
