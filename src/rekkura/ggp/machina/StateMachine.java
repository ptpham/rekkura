package rekkura.ggp.machina;

import java.util.Collection;

import rekkura.model.Dob;

import com.google.common.collect.Multiset;

public interface StateMachine<S, A> {
	public S getInitial();
	public Collection<A> getActions(S state);
	public S nextState(S state, Iterable<A> actions);
	public boolean isTerminal(S state);
	public Multiset<Dob> getGoals(S state);
}
