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

public class ProverStateMachine extends GameLogicContext implements GgpStateMachine {
	
	public final StratifiedProver prover;
	public final Ruletta rta;
	
	public ProverStateMachine(StratifiedProver prover) {
		super(prover.pool, prover.rta);
		this.rta = prover.rta;
		this.prover = prover;
	}
	
	@Override
	public Set<Dob> getInitial() {
		return extract(TRUE_QUERY, proverPass(Lists.<Dob>newArrayList(), INIT_UNIFY));
	}

	@Override public boolean isTerminal(Set<Dob> dobs) {
		return this.proverPass(dobs, EMTPY_UNIFY).contains(TERMINAL);
	}
	
	@Override
	public ListMultimap<Dob, Dob> getActions(Set<Dob> state) {
		return extractActions(proverPass(state, LEGAL_UNIFY));
	}

	@Override
	public Set<Dob> nextState(Set<Dob> state, Map<Dob, Dob> actions) {
		Iterable<Dob> truths = Iterables.concat(state, actions.values());
		return extract(TRUE_QUERY, proverPass(truths, NEXT_UNIFY));
	}

	@Override
	public Map<Dob, Integer> getGoals(Set<Dob> truths) {
		return extractGoals(proverPass(truths, EMTPY_UNIFY));
	}
	
	private Set<Dob> proverPass(Iterable<Dob> truths, Map<Dob, Dob> unify) {
		Set<Dob> proven = this.prover.proveAll(truths);
		return submersiveReplace(proven, unify, this.pool);
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
		List<Rule> augmented = augmentWithQueryRules(rules);
		return new ProverStateMachine(new StratifiedForward(augmented));
	}
	
	public static ProverStateMachine createWithStratifiedBackward(Collection<Rule> rules) {
		List<Rule> augmented = augmentWithQueryRules(rules);
		return new ProverStateMachine(new StratifiedBackward.Standard(augmented));
	}
}
