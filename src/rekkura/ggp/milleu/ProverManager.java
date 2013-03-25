package rekkura.ggp.milleu;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.ggp.machina.ProverStateMachine;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Maps;

public class ProverManager {
	
	public void run(List<Player> players, List<Rule> rules, MatchConfig config) {
		ProverStateMachine machine = new ProverStateMachine(rules);
		List<Dob> roles = GameManip.getRoles(rules);
		Map<Player, Dob> playerRoles = Maps.newHashMap();
		
		// Map players to roles
		for (int i = 0; i < roles.size(); i++) {
			Player player = i < players.size() ? players.get(i) : new Player.Random();
			playerRoles.put(player, roles.get(i));
		}
		
		// Send start messages to players
		for (Player player : players) {
			player.start(playerRoles.get(player), rules, config);
		}
		
		// Run game
		Map<Dob, Dob> actions = Maps.newHashMap();
		Set<Dob> state = machine.getInitial();
		while (true) {
			Map<Dob, Dob> next = Maps.newHashMap();
			for (Player player : players) {
				Dob move = player.play(actions);
				next.put(playerRoles.get(player), move);
			}
			actions = next;
			
			state = machine.nextState(state, actions);
			
			// Send stop messages to players
			if (machine.isTerminal(state)) {
				for (Player player : players) {
					player.stop(actions);
				}
				break;
			}
		}
	}
}
