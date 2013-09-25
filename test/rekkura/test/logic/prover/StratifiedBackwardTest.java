package rekkura.test.logic.prover;

import rekkura.logic.prover.StratifiedProver;
import rekkura.logic.prover.StratifiedProver.Factory;

public class StratifiedBackwardTest extends StratifiedProverTest {
	@Override protected Factory getFactory() { return StratifiedProver.BACKWARD_FACTORY; }
}
