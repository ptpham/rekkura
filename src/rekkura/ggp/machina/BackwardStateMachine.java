package rekkura.ggp.machina;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.prover.StratifiedBackward;
import rekkura.logic.structures.Pool;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

/**
 * This state machine leverages the proving style of the backward prover
 * to greater effect than the "generic" prover state machine.
 * @author ptpham
 *
 */
public class BackwardStateMachine extends GameLogicContext implements GgpStateMachine {

	public final StratifiedBackward prover;
	
	private final Pool pool;
	
	/**
	 * This is for the purpose of coalescing inferences
	 * on the same turn.
	 */
	private Set<Dob> last;
	
	public final Multimap<Rule, Dob> knownStatic = HashMultimap.create();
	
	private BackwardStateMachine(StratifiedBackward prover) {
		super(prover.pool, prover.rta);
		this.prover = prover;
		this.pool = prover.pool;
		
		// Store the subset of known that will never change
		prover.proveAll(Lists.<Dob>newArrayList());
		
		List<Rule> queryRules = constructQueryRules();
		this.staticRules.addAll(pool.rules.submerge(queryRules));
		
		for (Rule rule : this.staticRules) {
			this.knownStatic.putAll(rule, this.prover.getKnown(rule));
		}
	}

	@Override
	public Set<Dob> getInitial() {
		return extractTrues(proverPass(EMPTY_STATE, INIT_QUERY, INIT_UNIFY));
	}

	@Override
	public ListMultimap<Dob, Dob> getActions(Set<Dob> state) {
		return extractActions(proverPass(state, LEGAL_QUERY, LEGAL_UNIFY));
	}

	@Override
	public Set<Dob> nextState(Set<Dob> state, Map<Dob, Dob> actions) {
		applyState(state);
		prover.preserveTruths(actions.values());
		return extractTrues(proverPass(state, NEXT_QUERY, NEXT_UNIFY));
	}

	@Override
	public boolean isTerminal(Set<Dob> state) {
		return proverPass(state, TERMINAL, EMTPY_UNIFY).contains(TERMINAL);
	}

	@Override
	public Multiset<Dob> getGoals(Set<Dob> state) {
		return extractGoals(proverPass(state, GOAL_QUERY, EMTPY_UNIFY));
	}
	
	public static StratifiedBackward createProverForRules(Collection<Rule> rules) {
		List<Rule> augmented = augmentWithQueryRules(rules);
		return new StratifiedBackward(augmented);
	}

	public static BackwardStateMachine createForRules(Collection<Rule> rules) {
		return new BackwardStateMachine(createProverForRules(rules));
	}

	public static List<Rule> augmentWithQueryRules(Collection<Rule> rules) {
		List<Rule> augmented = Lists.newArrayList(rules);
		augmented.addAll(GameLogicContext.getVacuousQueryRules());
		return augmented;
	}
	
	private Set<Dob> proverPass(Set<Dob> state, Dob query, Map<Dob, Dob> unify) {
		applyState(state);
		Set<Dob> proven = prover.ask(query);
		Set<Dob> submerged = ProverStateMachine.submersiveReplace(proven, unify, pool);
		return submerged;
	}

	private void applyState(Set<Dob> state) {
		if (state != last) {
			prover.clear();
			prover.preserveTruths(state);
			prover.putKnown(knownStatic);
			last = state;
		}
	}	
}
