package rekkura.ggp.milleu;

import java.util.Collection;
import java.util.Map;

import rekkura.model.Dob;
import rekkura.model.Rule;

public interface Player {
	public abstract void start(int startclock, int playclock, Dob role, Collection<Rule> rules);	
	public abstract Dob play(Map<Dob, Dob> actions);
	public abstract void stop(Map<Dob, Dob> actions);
	
	public abstract class Partial implements Player {

		protected int startclock, playclock;
		protected Dob role;
		
		@Override
		public final void start(int startclock, int playclock, Dob role,
				Collection<Rule> rules) {
			this.startclock = startclock;
			this.playclock = playclock;
			this.role = role;
			
			this.metagame(rules);
		}
		
		protected abstract void metagame(Collection<Rule> rules);
	}
}
