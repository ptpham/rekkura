package rekkura.test.ggp.machina;

import rekkura.ggp.machina.GgpStateMachine;
import rekkura.ggp.machina.GgpStateMachine.Factory;

public class BackwardStateMachineTest extends StateMachineTest {
	@Override protected Factory<?> getFactory() { return GgpStateMachine.BACKWARD_PROVER; }
}
