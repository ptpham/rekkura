package rekkura.ggp.machina;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.Pool;
import rekkura.logic.Ruletta;
import rekkura.logic.Unifier;
import rekkura.logic.prover.StratifiedBackward;
import rekkura.logic.prover.StratifiedForward;
import rekkura.logic.prover.StratifiedProver;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
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
		return extractTrues(proverPass(Lists.<Dob>newArrayList(), INIT_UNIFY));
	}

	@Override public boolean isTerminal(Set<Dob> dobs) {
		return this.proverPass(dobs, EMTPY_UNIFY).contains(TERMINAL);
	}
	
	@Override
	public Multimap<Dob, Dob> getActions(Set<Dob> state) {
		return extractActions(proverPass(state, LEGAL_UNIFY));
	}

	@Override
	public Set<Dob> nextState(Set<Dob> state, Map<Dob, Dob> actions) {
		Iterable<Dob> truths = Iterables.concat(state, actions.values());
		return extractTrues(proverPass(truths, NEXT_UNIFY));
	}

	@Override
	public Multiset<Dob> getGoals(Set<Dob> truths) {
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
			if (replaced != dob) replaced = pool.submerge(replaced);
			advanced.add(replaced);
		}
		return advanced;
	}

	public static ProverStateMachine createWithStratifiedForward(Collection<Rule> rules) {
		return new ProverStateMachine(new StratifiedForward(rules));
	}
	
	public static ProverStateMachine createWithStratifiedBackward(Collection<Rule> rules) {
		return new ProverStateMachine(new StratifiedBackward(rules));
	}
	
	
}
