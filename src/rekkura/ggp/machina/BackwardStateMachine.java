package rekkura.ggp.machina;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.prover.StratifiedBackward;
import rekkura.logic.structure.Pool;

import com.google.common.collect.*;

/**
 * This state machine leverages the proving style of the backward prover
 * to greater effect than the "generic" prover state machine.
 * @author ptpham
 *
 */
public class BackwardStateMachine extends GameLogicContext implements GgpStateMachine {

	public final StratifiedBackward prover;
	private final Pool pool;
		
	public final Multimap<Rule, Dob> knownStatic = HashMultimap.create();
	public final Set<Rule> queryRules = Sets.newHashSet();
	
	private BackwardStateMachine(StratifiedBackward prover) {
		super(prover.pool, prover.rta);
		this.prover = prover;
		this.pool = prover.pool;
		
		// Store the subset of known that will never change
		prover.proveAll(Lists.<Dob>newArrayList());

		// Construct and add query rules
		queryRules.addAll(pool.rules.submerge(constructQueryRules()));
		this.staticRules.addAll(queryRules);
		
		for (Rule rule : this.staticRules) {
			this.knownStatic.putAll(rule, this.prover.getKnown(rule));
		}
	}

	@Override
	public Set<Dob> getInitial() {
		return extract(TRUE_QUERY, proverPass(EMPTY_STATE, INIT_QUERY, INIT_UNIFY));
	}

	@Override
	public ListMultimap<Dob, Dob> getActions(Set<Dob> state) {
		return extractActions(proverPass(state, LEGAL_QUERY, LEGAL_UNIFY));
	}

	@Override
	public Set<Dob> nextState(Set<Dob> state, Map<Dob, Dob> actions) {
		Iterable<Dob> complete = Iterables.concat(state, actions.values());
		return extract(TRUE_QUERY, proverPass(complete, NEXT_QUERY, NEXT_UNIFY));
	}

	@Override
	public boolean isTerminal(Set<Dob> state) {
		return proverPass(state, TERMINAL, EMTPY_UNIFY).contains(TERMINAL);
	}

	@Override
	public Map<Dob, Integer> getGoals(Set<Dob> state) {
		return extractGoals(proverPass(state, GOAL_QUERY, EMTPY_UNIFY));
	}
	
	public static StratifiedBackward.Standard createProverForRules(Collection<Rule> rules) {
		List<Rule> augmented = augmentWithQueryRules(rules);
		return new StratifiedBackward.Standard(augmented);
	}

	public static BackwardStateMachine createForRules(Collection<Rule> rules) {
		return new BackwardStateMachine(createProverForRules(rules));
	}

	public static List<Rule> augmentWithQueryRules(Collection<Rule> rules) {
		List<Rule> augmented = Lists.newArrayList(rules);
		augmented.addAll(GameLogicContext.getVacuousQueryRules());
		return augmented;
	}
	
	private Set<Dob> proverPass(Iterable<Dob> state, Dob query, Map<Dob, Dob> unify) {
		prover.clear();
		prover.preserveTruths(state);
		prover.putKnown(knownStatic);
		Set<Dob> proven = prover.ask(query);
		Set<Dob> submerged = ProverStateMachine.submersiveReplace(proven, unify, pool);
		return submerged;
	}
}
