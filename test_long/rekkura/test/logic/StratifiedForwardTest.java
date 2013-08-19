package rekkura.test.logic;

import rekkura.logic.prover.StratifiedProver;
import rekkura.logic.prover.StratifiedProver.Factory;
import rekkura.test.logic.prover.StratifiedProverTest;

public class StratifiedForwardTest extends StratifiedProverTest {
	@Override protected Factory getFactory() { return StratifiedProver.FORWARD_FACTORY; }
}
