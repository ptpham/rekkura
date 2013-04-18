package rekkura.ggp.milleu;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.Pool;
import rekkura.logic.Ruletta;
import rekkura.logic.Unifier;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.OtmUtil;

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
	
	public final Set<Dob> EMPTY_STATE = Sets.newHashSet();
	
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

		List<Dob> var = rta.getVariables(2);
		this.ROLE_VAR = var.get(0);
		this.GENERIC_VAR = var.get(1);
		this.GOAL_QUERY = pool.submerge(new Dob(GOAL, ROLE_VAR, GENERIC_VAR));
		this.LEGAL_QUERY = pool.submerge(new Dob(LEGAL, ROLE_VAR, GENERIC_VAR));
		this.INPUT_QUERY = pool.submerge(new Dob(INPUT, ROLE_VAR, GENERIC_VAR));
		this.DOES_QUERY = pool.submerge(new Dob(DOES, ROLE_VAR, GENERIC_VAR));
		this.INIT_QUERY = pool.submerge(new Dob(INIT, GENERIC_VAR));
		this.NEXT_QUERY = pool.submerge(new Dob(NEXT, GENERIC_VAR));
		this.BASE_QUERY = pool.submerge(new Dob(BASE, GENERIC_VAR));
		this.TRUE_QUERY = pool.submerge(new Dob(TRUE, GENERIC_VAR));

		this.NEXT_UNIFY.put(this.NEXT, this.TRUE);
		this.INIT_UNIFY.put(this.INIT, this.TRUE);
		this.LEGAL_UNIFY.put(this.LEGAL, this.DOES);
		
		Multimap<Rule, Rule> ruleToDepRule = HashMultimap.create();
		Multimaps.invertFrom(this.rta.ruleToGenRule, ruleToDepRule);
		
		Set<Rule> roots = Sets.newHashSet();
		roots.addAll(rta.getAffectedRules(DOES_QUERY));
		roots.addAll(rta.getAffectedRules(TRUE_QUERY));

		mutableRules.addAll(OtmUtil.flood(ruleToDepRule, roots));
		this.staticRules.addAll(this.rta.allRules);
		this.staticRules.removeAll(mutableRules);
	}
	
	private Dob getTerminalDob(String name) {
		return this.pool.submerge(new Dob(name));
	}
	
	public Multiset<Dob> extractGoals(Set<Dob> dobs) {
		Multiset<Dob> result = HashMultiset.create();
		for (Dob dob : dobs) {
			Map<Dob, Dob> unify = Unifier.unifyVars(GOAL_QUERY, dob, rta.allVars);
			if (unify == null) continue;
			
			Dob role = unify.get(ROLE_VAR);
			Dob value = unify.get(GENERIC_VAR);
			if (value == null || role == null) continue;			
			
			int goal = 0;
			try { goal = Integer.parseInt(value.name); } 
			catch (Exception e) { continue; }
			result.add(role, goal);
		}
		
		return result;
	}
	
	public Set<Dob> extractTrues(Set<Dob> dobs) {
		Set<Dob> result = Sets.newHashSetWithExpectedSize(dobs.size());
		for (Dob dob : dobs) {
			boolean keep = !dob.isTerminal() 
				&& dob.size() > 0 && dob.at(0) == this.TRUE;
			if (keep) result.add(dob);
		}
		return result;
	}
	
	public ListMultimap<Dob, Dob> extractActions(Collection<Dob> truths) {
		ListMultimap<Dob, Dob> result = ArrayListMultimap.create();
		for (Dob dob : truths) {
			if (dob.size() < 3) continue;
			if (dob.at(0) != this.DOES) continue;
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
		List<Rule> result = Lists.newArrayList();
		
		GameLogicContext context = new GameLogicContext();
		List<Dob> queries = Lists.newArrayList(context.INIT_QUERY,
			context.NEXT_QUERY, context.GOAL_QUERY, context.LEGAL_QUERY,
			context.TRUE_QUERY, context.DOES_QUERY);
		
		List<Dob> vars = Lists.newArrayList(context.ROLE_VAR, context.GENERIC_VAR);
		for (Dob dob : queries) result.add(Rule.asVacuousRule(dob, vars));
		return result;
	}
}
