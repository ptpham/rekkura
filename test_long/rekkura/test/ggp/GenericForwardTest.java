package rekkura.test.ggp;

import rekkura.ggp.machina.GgpStateMachine;
import rekkura.ggp.machina.GgpStateMachine.Factory;

public class GenericForwardTest extends StateMachineTest {
	@Override protected Factory<?> getFactory() { return GgpStateMachine.GENERIC_FORWARD_PROVER; }
}
