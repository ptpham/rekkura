package rekkura.ggp.statemachine;

import java.util.List;

public interface StateMachine
	<S extends StateMachine.State, A extends StateMachine.Action> {
	
	public interface State { 
		boolean isTerminal();
		int getGoal();
	}
	
	public interface Action { }
	
	public S getInitial();
	public List<A> getActions(S state);
	public S nextState(S state, A action);
}
