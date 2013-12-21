package rekkura.ggp.milleu;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.algorithm.Unifier;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Pool;
import rekkura.logic.structure.Ruletta;
import rekkura.util.OtmUtil;
import rekkura.util.Typoz;

import com.google.common.collect.*;

/**
 * A {@link GameLogicContext} constructs references to all of the 
 * reserve keywords in GDL, and partitions rules by whether
 * they are "GDL static" or "GDL mutable". A rule is "GDL static"
 * if it can never be influenced by the state or player actions.
 * A rule is GDL mutable if it is not GDL static. <br>
 * <br>
 * It also contains {@link Dob}s that represent unification templates 
 * for important structures in GDL and methods to query for important
 * GDL structures in a given state such as the goals, actions, and 
 * trues.<br>
 * <br>
 * For utilities at a matchmaking/parsing level, take a look at
 * {@code Game}.
 * @author ptpham
 *
 */
public class GameLogicContext {

	// GDL reserve words
	public final Dob TERMINAL, INIT, LEGAL, BASE, INPUT;
	public final Dob ROLE, DOES, NEXT, TRUE, GOAL;
	public final ImmutableList<Dob> KEYWORDS;

	// Data structures for doing GGP manipulations
	public final Dob ROLE_VAR, GENERIC_VAR;
	public final Dob BASE_QUERY, INPUT_QUERY;
	public final Dob GOAL_QUERY, INIT_QUERY;
	public final Dob NEXT_QUERY, LEGAL_QUERY;
	public final Dob DOES_QUERY, TRUE_QUERY;
	public final Map<Dob, Dob> NEXT_UNIFY = Maps.newHashMap();
	public final Map<Dob, Dob> INIT_UNIFY = Maps.newHashMap();
	public final Map<Dob, Dob> LEGAL_UNIFY = Maps.newHashMap();
	public final Map<Dob, Dob> EMTPY_UNIFY = Maps.newHashMap();
	public final Map<Dob, Dob> BASE_UNIFY = Maps.newHashMap();
	public final Map<Dob, Dob> INPUT_UNIFY = Maps.newHashMap();
	public final ImmutableList<Dob> GENERIC_VAR_LIST;

	public final String ROLE_VAR_NAME = "[GLC_ROLE]";
	public final String GENERIC_VAR_NAME = "[GLC_GEN]";

	public final ImmutableSet<Dob> EMPTY_STATE = ImmutableSet.of();

	public final Pool pool;
	public final Ruletta rta;

	/**
	 * This holds the set of rules whose expansion never 
	 * relies on the current state or what the players do.
	 */
	public final Set<Rule> staticRules = Sets.newHashSet();

	public final Set<Rule> mutableRules = Sets.newHashSet();

	public GameLogicContext() {
		this(new Pool(), Ruletta.createEmpty());
	}

	public GameLogicContext(Pool pool, Ruletta rta) {
		this.pool = pool;
		this.rta = rta;

		this.TERMINAL = getTerminalDob(Game.TERMINAL_NAME);
		this.BASE = getTerminalDob(Game.BASE_NAME);
		this.INPUT = getTerminalDob(Game.INPUT_NAME);
		this.TRUE = getTerminalDob(Game.TRUE_NAME);
		this.DOES = getTerminalDob(Game.DOES_NAME);
		this.INIT = getTerminalDob(Game.INIT_NAME);
		this.LEGAL = getTerminalDob(Game.LEGAL_NAME);
		this.NEXT = getTerminalDob(Game.NEXT_NAME);
		this.GOAL = getTerminalDob(Game.GOAL_NAME);
		this.ROLE = getTerminalDob(Game.ROLE_NAME);
		this.KEYWORDS = ImmutableList.of(this.TERMINAL, this.BASE, this.INIT,
			this.INPUT, this.TRUE, this.DOES, this.LEGAL, this.NEXT, this.GOAL, this.ROLE);

		this.ROLE_VAR = getTerminalDob(ROLE_VAR_NAME);
		this.GENERIC_VAR = getTerminalDob(GENERIC_VAR_NAME);
		this.GENERIC_VAR_LIST = ImmutableList.of(ROLE_VAR, GENERIC_VAR);

		pool.allVars.add(ROLE_VAR);
		pool.allVars.add(GENERIC_VAR);

		this.GOAL_QUERY = pool.dobs.submerge(new Dob(GOAL, ROLE_VAR, GENERIC_VAR));
		this.LEGAL_QUERY = pool.dobs.submerge(new Dob(LEGAL, ROLE_VAR, GENERIC_VAR));
		this.INPUT_QUERY = pool.dobs.submerge(new Dob(INPUT, ROLE_VAR, GENERIC_VAR));
		this.DOES_QUERY = pool.dobs.submerge(new Dob(DOES, ROLE_VAR, GENERIC_VAR));
		this.INIT_QUERY = pool.dobs.submerge(new Dob(INIT, GENERIC_VAR));
		this.NEXT_QUERY = pool.dobs.submerge(new Dob(NEXT, GENERIC_VAR));
		this.BASE_QUERY = pool.dobs.submerge(new Dob(BASE, GENERIC_VAR));
		this.TRUE_QUERY = pool.dobs.submerge(new Dob(TRUE, GENERIC_VAR));

		this.NEXT_UNIFY.put(this.NEXT, this.TRUE);
		this.INIT_UNIFY.put(this.INIT, this.TRUE);
		this.LEGAL_UNIFY.put(this.LEGAL, this.DOES);
		this.INPUT_UNIFY.put(this.INPUT, this.DOES);
		this.BASE_UNIFY.put(this.BASE, this.TRUE);

		Multimap<Rule, Rule> ruleToDepRule = HashMultimap.create();
		Multimaps.invertFrom(this.rta.ruleToGenRule, ruleToDepRule);

		Set<Rule> roots = Sets.newHashSet();
		roots.addAll(Ruletta.filterNonConflictingBodies(DOES_QUERY, rta.allRules, pool));
		roots.addAll(Ruletta.filterNonConflictingBodies(TRUE_QUERY, rta.allRules, pool));

		mutableRules.addAll(OtmUtil.flood(ruleToDepRule, roots));
		this.staticRules.addAll(this.rta.allRules);
		this.staticRules.removeAll(mutableRules);
	}

	private Dob getTerminalDob(String name) {
		return this.pool.dobs.submerge(new Dob(name));
	}

	/**
	 * Parses any goals in the given dobs into a map. If there are multiple
	 * goal dobs with the same roles, the entry in the map will be undefined.
	 * @param dobs
	 * @return
	 */
	public Map<Dob, Integer> extractGoals(Iterable<Dob> dobs) {
		Map<Dob, Integer> result = Maps.newHashMap();
		for (Dob dob : dobs) {
			Map<Dob, Dob> unify = Unifier.unifyVars(GOAL_QUERY, dob, pool.allVars);
			if (unify == null) continue;

			Dob role = unify.get(ROLE_VAR);
			Dob value = unify.get(GENERIC_VAR);
			if (value == null || role == null) continue;			

			Integer goal = Typoz.lightParseInt(value.name);
			if (goal == null) continue;
			result.put(role, goal);
		}

		return result;
	}

	public Set<Dob> extract(Dob query, Collection<Dob> dobs) {
		Set<Dob> result = Sets.newHashSetWithExpectedSize(dobs.size());
		for (Dob dob : dobs) {
			if (Unifier.unifyVars(query, dob, GENERIC_VAR_LIST) != null) {
				result.add(dob);
			}
		}
		return result;
	}

	public ListMultimap<Dob, Dob> extractActions(Collection<Dob> truths) {
		ListMultimap<Dob, Dob> result = ArrayListMultimap.create();
		for (Dob dob : truths) {
			if (Unifier.unifyVars(DOES_QUERY, dob, GENERIC_VAR_LIST) == null) continue;
			result.put(dob.at(1), dob);
		}
		return result;
	}
	
	/**
	 * Use this method to generate a set of rules that represent forms 
	 * with which you may want to make queries.
	 * @return
	 */
	public static List<Rule> getVacuousQueryRules() {
		return new GameLogicContext().constructQueryRules();
	}

	public List<Rule> constructQueryRules() {
		List<Dob> queries = Lists.newArrayList(INIT_QUERY,
				NEXT_QUERY, GOAL_QUERY, LEGAL_QUERY, TRUE_QUERY, 
				DOES_QUERY, BASE_QUERY, INPUT_QUERY);
		List<Rule> result = Lists.newArrayList();
		for (Dob dob : queries) result.add(Rule.asVacuous(dob, GENERIC_VAR_LIST));
		return result;
	}

	public static List<Rule> augmentWithQueryRules(Collection<Rule> rules) {
		List<Rule> augmented = Lists.newArrayList(rules);
		augmented.addAll(GameLogicContext.getVacuousQueryRules());
		return augmented;
	}
}
