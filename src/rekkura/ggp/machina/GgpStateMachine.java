package rekkura.ggp.machina;

import java.util.Collection;
import java.util.Set;

import rekkura.logic.algorithm.Optimizer;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.state.model.StateMachine;

public interface GgpStateMachine extends StateMachine<Set<Dob>, Dob> {

	public static interface Factory<M extends GgpStateMachine> { M create(Collection<Rule> rules); }
	
	public static final Factory<ProverStateMachine> GENERIC_FORWARD_PROVER = 
	new Factory<ProverStateMachine>() {
		@Override public ProverStateMachine create(Collection<Rule> rules)  {
			return ProverStateMachine.createWithStratifiedForward(Optimizer.standard(rules));
		}
	};
	
	public static final Factory<ProverStateMachine> GENERIC_BACKWARD_PROVER = 
	new Factory<ProverStateMachine>() { 
		@Override public ProverStateMachine create(Collection<Rule> rules) {
			return ProverStateMachine.createWithStratifiedBackward(Optimizer.standard(rules));
		}
	};
	
	public static final Factory<BackwardStateMachine> BACKWARD_PROVER = 
	new Factory<BackwardStateMachine>() {
		@Override public BackwardStateMachine create(Collection<Rule> rules) {
			return BackwardStateMachine.createForRules(Optimizer.standard(rules));
		}
	};
}
