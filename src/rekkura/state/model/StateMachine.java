package rekkura.state.model;

import java.util.Map;

import rekkura.logic.model.Dob;

import com.google.common.collect.ListMultimap;

public class StateMachine<S, A> {
	public interface GetInitial<S> { S getInitial(); }
	public interface GetActions<S, A> { ListMultimap<Dob, A> getActions(S state); }
	public interface NextState<S, A> { S nextState(S state, Map<Dob, A> actions); }
	public interface IsTerminal<S> { boolean isTerminal(S state); }
	public interface GetGoals<S> {  Map<Dob, Integer> getGoals(S state); }
	
	public interface Evaluator<S> extends IsTerminal<S>, GetGoals<S> { }
	public interface Advancer<S, A> extends GetActions<S, A>, NextState<S, A> { }
	public interface Standard<S, A> extends GetInitial<S>, Advancer<S, A>, Evaluator<S> { }
}
