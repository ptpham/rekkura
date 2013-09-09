package rekkura.state.model;

import java.util.Map;

import rekkura.logic.model.Dob;

import com.google.common.collect.ListMultimap;

public interface StateMachine<S, A> {
	interface GetInitial<S> { S getInitial(); }
	interface GetActions<S, A> { ListMultimap<Dob, A> getActions(S state); }
	interface NextState<S, A> { S nextState(S state, Map<Dob, A> actions); }
	interface IsTerminal<S> { boolean isTerminal(S state); }
	interface GetGoals<S> {  Map<Dob, Integer> getGoals(S state); }
	
	interface Standard<S, A> extends GetInitial<S>, 
		GetActions<S, A>, NextState<S, A>, IsTerminal<S>, GetGoals<S> { }
}
