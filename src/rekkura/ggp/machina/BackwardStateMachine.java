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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * This state machine leverages the proving style of the backward prover
 * to greater effect than the "generic" prover state machine.
 * @author ptpham
 *
 */
public class BackwardStateMachine implements GgpStateMachine {

	public final StratifiedBackward prover;
	public final GameLogicContext glc;
	private final Pool pool;
	
		
	public final Multimap<Rule, Dob> knownStatic = HashMultimap.create();
	public final Set<Rule> queryRules = Sets.newHashSet();

	public BackwardStateMachine(StratifiedBackward prover) {
		this.glc = new GameLogicContext(prover.pool, prover.rta);
		this.prover = prover;
		this.pool = prover.pool;

		// Construct and add query rules
		queryRules.addAll(pool.rules.submerge(glc.constructQueryRules()));
		glc.staticRules.addAll(queryRules);

		// Store the subset of known that will never change
		prover.proveAll(Lists.<Dob>newArrayList());
		for (Rule rule : this.glc.staticRules) {
			this.knownStatic.putAll(rule, this.prover.traversal.known.get(rule));
		}
	}

	@Override
	public Set<Dob> getInitial() {
		return glc.extract(glc.TRUE_QUERY, proverPass(glc.EMPTY_STATE, glc.INIT_QUERY, glc.INIT_UNIFY));
	}

	@Override
	public ListMultimap<Dob, Dob> getActions(Set<Dob> state) {
		return glc.extractActions(proverPass(state, glc.LEGAL_QUERY, glc.LEGAL_UNIFY));
	}

	@Override
	public Set<Dob> nextState(Set<Dob> state, Map<Dob, Dob> actions) {
		Iterable<Dob> complete = Iterables.concat(state, actions.values());
		return glc.extract(glc.TRUE_QUERY, proverPass(complete, glc.NEXT_QUERY, glc.NEXT_UNIFY));
	}

	@Override
	public boolean isTerminal(Set<Dob> state) {
		return proverPass(state, glc.TERMINAL, glc.EMTPY_UNIFY).contains(glc.TERMINAL);
	}

	@Override
	public Map<Dob, Integer> getGoals(Set<Dob> state) {
		return glc.extractGoals(proverPass(state, glc.GOAL_QUERY, glc.EMTPY_UNIFY));
	}
	
	public static StratifiedBackward createProverForRules(Collection<Rule> rules) {
		List<Rule> augmented = GameLogicContext.augmentWithQueryRules(rules);
		return new StratifiedBackward(augmented);
	}

	public static BackwardStateMachine createForRules(Collection<Rule> rules) {
		return new BackwardStateMachine(createProverForRules(rules));
	}
	
	private Set<Dob> proverPass(Iterable<Dob> state, Dob query, Map<Dob, Dob> unify) {
		prover.clear();
		prover.preserveTruths(state);
		prover.preserveAndPutKnown(knownStatic);
		prover.traversal.visited.addAll(knownStatic.keySet());
		Set<Dob> proven = prover.ask(query);
		Set<Dob> submerged = ProverStateMachine.submersiveReplace(proven, unify, pool);
		return submerged;
	}
}
