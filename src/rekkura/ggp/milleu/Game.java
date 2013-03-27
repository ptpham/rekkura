package rekkura.ggp.milleu;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Lists;

public class Game {

	public static class Config {
		public int startclock, playclock;
		public List<Rule> rules;
		public Config(int startclock, int playclock, List<Rule> rules) {
			this.startclock = startclock;
			this.playclock = playclock;
			this.rules = rules;
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
	
	public static class Move {
		public int turn;
		public Dob dob;
		public Move(int turn, Dob dob) { 
			this.turn = turn; 
			this.dob = dob; 
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
			if (!first.name.equals("role")) continue;
			
			roles.add(head.dob.at(1));
		}
		
		return roles;
	}
	
}
