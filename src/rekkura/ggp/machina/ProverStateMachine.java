package rekkura.ggp.machina;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.algorithm.Unifier;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.prover.StratifiedBackward;
import rekkura.logic.prover.StratifiedForward;
import rekkura.logic.prover.StratifiedProver;
import rekkura.logic.structure.Pool;
import rekkura.logic.structure.Ruletta;

import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ProverStateMachine implements GgpStateMachine {
	
	public final StratifiedProver prover;
	public final GameLogicContext glc;
	public final Ruletta rta;
	
	public ProverStateMachine(StratifiedProver prover) {
		this.glc = new GameLogicContext(prover.pool, prover.rta);
		this.rta = prover.rta;
		this.prover = prover;
	}
	
	@Override
	public Set<Dob> getInitial() {
		return glc.extract(glc.TRUE_QUERY, proverPass(Lists.<Dob>newArrayList(), glc.INIT_UNIFY));
	}

	@Override public boolean isTerminal(Set<Dob> dobs) {
		return this.proverPass(dobs, glc.EMTPY_UNIFY).contains(glc.TERMINAL);
	}
	
	@Override
	public ListMultimap<Dob, Dob> getActions(Set<Dob> state) {
		return glc.extractActions(proverPass(state, glc.LEGAL_UNIFY));
	}

	@Override
	public Set<Dob> nextState(Set<Dob> state, Map<Dob, Dob> actions) {
		Iterable<Dob> truths = Iterables.concat(state, actions.values());
		return glc.extract(glc.TRUE_QUERY, proverPass(truths, glc.NEXT_UNIFY));
	}

	@Override
	public Map<Dob, Integer> getGoals(Set<Dob> truths) {
		return glc.extractGoals(proverPass(truths, glc.EMTPY_UNIFY));
	}
	
	private Set<Dob> proverPass(Iterable<Dob> truths, Map<Dob, Dob> unify) {
		Set<Dob> proven = this.prover.proveAll(truths);
		return submersiveReplace(proven, unify, this.prover.pool);
	}

	public static Set<Dob> submersiveReplace(Set<Dob> proven, Map<Dob, Dob> unify, Pool pool) {
		Set<Dob> advanced = Sets.newHashSetWithExpectedSize(proven.size()); 
		for (Dob dob : proven) {
			Dob replaced = Unifier.replace(dob, unify);
			if (replaced != dob) replaced = pool.dobs.submerge(replaced);
			advanced.add(replaced);
		}
		return advanced;
	}
	
	public static ProverStateMachine createWithStratifiedForward(Collection<Rule> rules) {
		List<Rule> augmented = GameLogicContext.augmentWithQueryRules(rules);
		return new ProverStateMachine(new StratifiedForward(augmented));
	}
	
	public static ProverStateMachine createWithStratifiedBackward(Collection<Rule> rules) {
		List<Rule> augmented = GameLogicContext.augmentWithQueryRules(rules);
		return new ProverStateMachine(new StratifiedBackward(augmented));
	}
}
