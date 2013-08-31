package rekkura.ggp.machina;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.algorithm.Optimizer;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Pool;
import rekkura.state.model.StateMachine;

import com.google.common.collect.Sets;

public interface GgpStateMachine extends StateMachine<Set<Dob>, Dob> {

	public static abstract class Factory<M extends GgpStateMachine> {
		public abstract M create(Collection<Rule> rules);
		
		public static Set<Rule> optimizeStandard(Iterable<Rule> rules) {
			GameLogicContext glc = new GameLogicContext();
			List<Rule> submerged = glc.pool.rules.submerge(rules);
			Set<Rule> prohibited = Pool.rulesWithHeadContainingAny(Sets.newHashSet(glc.KEYWORDS), submerged);
			return Optimizer.standard(submerged, prohibited, glc.pool);
		}
	}
	
	public static final Factory<ProverStateMachine> GENERIC_FORWARD_PROVER = 
	new Factory<ProverStateMachine>() {
		@Override public ProverStateMachine create(Collection<Rule> rules)  {
			return ProverStateMachine.createWithStratifiedForward(optimizeStandard(rules));
		}
	};
	
	public static final Factory<ProverStateMachine> GENERIC_BACKWARD_PROVER = 
	new Factory<ProverStateMachine>() { 
		@Override public ProverStateMachine create(Collection<Rule> rules) {
			return ProverStateMachine.createWithStratifiedBackward(optimizeStandard(rules));
		}
	};
	
	public static final Factory<BackwardStateMachine> BACKWARD_PROVER = 
	new Factory<BackwardStateMachine>() {
		@Override public BackwardStateMachine create(Collection<Rule> rules) {
			return BackwardStateMachine.createForRules(optimizeStandard(rules));
		}
	};
}
