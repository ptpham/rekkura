package rekkura.test.ggp.machina;

import rekkura.ggp.machina.GgpStateMachine;
import rekkura.ggp.machina.GgpStateMachine.Factory;

public class GenericBackwardTest extends StateMachineTest {
	@Override protected Factory<?> getFactory() { return GgpStateMachine.GENERIC_BACKWARD_PROVER; }
}
