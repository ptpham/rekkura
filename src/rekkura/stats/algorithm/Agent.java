package rekkura.stats.algorithm;

import java.util.List;
import java.util.Map;

import rekkura.logic.model.Dob;

import com.google.common.collect.Maps;

public class Agent {
	public interface Player<U> { public U play(); }
	public interface Explorer<U> { public U explore(); }
	public interface Informable<U> { public void inform(U action, double value); }
	
	public interface Standard<U> extends Player<U>, Explorer<U>, Informable<U> { }

	public static <A> Map<Dob, A> jointSuggest(Map<Dob, ? extends Agent.Explorer<A>> agents,
		List<Dob> roles) {
		Map<Dob, A> joint = Maps.newHashMap();
		for (Dob role : roles) {
			A move = agents.get(role).explore();
			if (move == null) return null;
			joint.put(role, move);
		}
		return joint;
	}

	public static <A> void jointInform(Map<Dob, ? extends Agent.Informable<A>> agents,
		Map<Dob, A> actions, Map<Dob, Integer> goals) {
		if (actions == null) return;
		for (Map.Entry<Dob, ? extends Agent.Informable<A>> entry : agents.entrySet()) { 
			Dob role = entry.getKey();
			Integer goal = goals.get(role);
			entry.getValue().inform(actions.get(role), goal);
		}
	}
}
