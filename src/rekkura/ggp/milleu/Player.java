package rekkura.ggp.milleu;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import rekkura.ggp.machina.ProverStateMachine;
import rekkura.ggp.machina.StateMachine;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Colut;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public interface Player {
	public abstract void start(Dob role, Collection<Rule> rules, MatchConfig config);
	public abstract Dob play(Map<Dob, Dob> actions);
	public abstract void stop(Map<Dob, Dob> actions);
	
	public abstract class Partial implements Player {

		protected MatchConfig config;
		protected Dob role;
		
		@Override
		public final void start(Dob role, Collection<Rule> rules, MatchConfig config) {
			this.config = config;
			this.role = role;
			
			this.metagame(rules);
		}
		
		protected abstract void metagame(Collection<Rule> rules);
	}
	
	public abstract class StateBased<M extends StateMachine<Set<Dob>, Dob>> extends Partial {
		private Set<Dob> current;
		protected M machine;
		
		protected abstract M constructMachine(Collection<Rule> rules);
		protected abstract void plan(Set<Dob> initial);
		protected abstract Dob move(Set<Dob> state, Multimap<Dob, Dob> actions);
		protected abstract void reflect(Set<Dob> state, Multiset<Dob> goals);
		
		@Override
		public Dob play(Map<Dob, Dob> actions) {
			current = this.machine.nextState(current, actions);
			return move(current, this.machine.getActions(current));
		}

		@Override
		public void stop(Map<Dob, Dob> actions) {
			current = this.machine.nextState(current, actions);
			reflect(current, this.machine.getGoals(current));
		}

		@Override
		protected final void metagame(Collection<Rule> rules) {
			this.machine = constructMachine(rules);
			current = this.machine.getInitial();
			plan(current);
		}
	}
	
	public abstract class ProverBased extends StateBased<ProverStateMachine> {
		@Override
		protected ProverStateMachine constructMachine(Collection<Rule> rules) {
			return new ProverStateMachine(rules);
		}
	}
	
	public class Random extends ProverBased {
		@Override
		protected Dob move(Set<Dob> state, Multimap<Dob, Dob> actions) {
			return Colut.any(actions.get(this.role));
		}
		
		@Override protected void plan(Set<Dob> initial) { }
		@Override protected void reflect(Set<Dob> state, Multiset<Dob> goals) { }
	}
}
