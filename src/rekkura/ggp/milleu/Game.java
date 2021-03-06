package rekkura.ggp.milleu;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.algorithm.Unifier;
import rekkura.logic.format.StandardFormat;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.prover.StratifiedBackward;
import rekkura.util.Limiter;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * This class represents a collection of utilities and 
 * constants for working with GDL games. It is intendend
 * to be useful at a match/parsing layer and not at the logic
 * layer. For the latter, take a look at {@link GameLogicContext}.
 * @author ptpham
 *
 */
public class Game {

	public static final String ROLE_NAME = "role";
	public static final String TERMINAL_NAME = "terminal";
	public static final String BASE_NAME = "base";
	public static final String INPUT_NAME = "input";
	public static final String TRUE_NAME = "true";
	public static final String DOES_NAME = "does";
	public static final String INIT_NAME = "init";
	public static final String LEGAL_NAME = "legal";
	public static final String NEXT_NAME = "next";
	public static final String GOAL_NAME = "goal";
		
	public static class Config {
		public long startclock, playclock;
		public ImmutableList<Rule> rules;
		public Config(long startclock, long playclock, List<Rule> rules) {
			this.startclock = startclock;
			this.playclock = playclock;
			this.rules = ImmutableList.copyOf(rules);
		}
	}
	
	public static class Turn {
		public int turn;
		public Set<Dob> state;
		public Turn(int turn, Set<Dob> state) { 
			this.turn = turn; 
			this.state = state; 
		}
	}
	
	public static class Decision {
		public int turn;
		public Dob action;
		public Decision(int turn, Dob action) { 
			this.turn = turn; 
			this.action = action; 
		}
	}
	
	public static class Record {
		public Set<Dob> state;
		public Map<Dob, Dob> moves;
		public Record(Set<Dob> state, Map<Dob, Dob> moves) {
			this.state = state;
			this.moves = moves;
		}
		
		@Override
		public String toString() {
			return "(State:" + state + ", Moves:" + moves + ")";
		}
	}
	
	public static List<Dob> getRoles(Collection<Rule> rules) {
		List<Dob> roles = Lists.newArrayList();
		
		for (Rule rule : rules) {
			if (rule.body.size() != 0 || rule.vars.size() != 0) continue;
			
			Atom head = rule.head;
			if (!head.truth || head.dob.size() != 2) continue;
			
			Dob first = head.dob.at(0);
			if (!first.isTerminal()) continue;
			if (!first.name.equals(ROLE_NAME)) continue;
			
			roles.add(head.dob.at(1));
		}
		
		return roles;
	}
	
	/**
	 * A "move" is the part of an "action" that conveys what the
	 * player is doing. I.e. an "action" is the combination of a
	 * role and move.
	 * @param role
	 * @param move
	 * @return
	 */
	public static Dob convertMoveToAction(Dob role, Dob move) {
		return new Dob(new Dob(DOES_NAME), role, move);
	}
	
	public static Map<Dob, Dob> convertMovesToActionMap(List<Dob> roles, List<Dob> moves) {
		Preconditions.checkArgument(roles.size() == moves.size());
		
		Map<Dob, Dob> result = Maps.newHashMap();
		for (int i = 0; i < roles.size(); i++) {
			Dob role = roles.get(i), move = moves.get(i);
			result.put(role, convertMoveToAction(role, move));
		}
		
		return result;
	}
	
	public static List<Dob> convertActionMapToMoves(List<Dob> roles, Map<Dob, Dob> actions) {
		List<Dob> result = Lists.newArrayList();
		for (Dob role : roles) result.add(actions.get(role));
		return result;
	}
	
	public static Dob convertActionToMove(Dob action) {
		if (action == null || action.size() < 3) return null;
		return action.at(2);
	}

	public static Dob getRoleForAction(Dob action) {
		if (action == null || action.size() < 3) return null;
		return action.at(1);
	}
	
	public static Decision asDecisionsFromStandardFormat(int turn, String stdDob) {
		return new Decision(turn, StandardFormat.inst.dobFromString(stdDob));
	}
	
	public static List<Decision> asDecisionsFromStandardFormat(Iterable<String> stdMoves) {
		int i = 0;
		List<Decision> result = Lists.newArrayList();
		for (String raw : stdMoves) {
			result.add(asDecisionsFromStandardFormat(i, raw));
			i++;
		}
		return result;
	}

	/**
	 * Returns for each rule in the game the set of dobs that
	 * will potentially be generated from it.
	 * @param raw
	 * @param limiter
	 * @return
	 */
	public static Multimap<Rule,Dob> generateAll(Iterable<Rule> raw, Limiter limiter) {
		// Release the negation constraints on rules
		List<Rule> rules = Lists.newArrayList();
		for (Rule rule : raw) {
			List<Atom> positives = Atom.filterPositives(rule.body);
			rules.add(new Rule(rule.head, positives, rule.vars, rule.distinct));
		}
		
		// Add the rule to handle moving inits to trues
		Rule helper = StandardFormat.inst.ruleFromString("{(?x)|<((true)(?x)),true>:-<((init)(?x)),true>}");
		rules.add(helper);
		
		// Set up required logic structures
		StratifiedBackward prover = new StratifiedBackward(rules);
		GameLogicContext context = new GameLogicContext(prover.pool, prover.rta);
		helper = prover.pool.rules.submerge(helper);

		// Spin until we reach the limit or until we don't generate any new dobs
		limiter.begin();
		Multimap<Rule,Dob> generated = HashMultimap.create();
		while (!limiter.exceeded()) {
			prover.traversal.visited.clear();
			List<Dob> current = Lists.newArrayList(prover.askAll());
			current = Unifier.replaceDobs(current, context.LEGAL_UNIFY);
			current = Unifier.replaceDobs(current, context.NEXT_UNIFY);
			prover.storeTruths(current);
			if (!generated.putAll(prover.traversal.known)) break;
		}
		
		return generated;
	}
}
