package rekkura.ggp.milleu;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.ggp.machina.ProverStateMachine;
import rekkura.model.Dob;
import rekkura.util.Synchron;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class MatchRunnable implements Runnable {
	List<Player> players;
	Game.Config config;
	
	public MatchRunnable(List<Player> players, Game.Config config) {
		this.players = players;
		this.config = config;
	}
	
	@Override
	public void run() {
		ProverStateMachine machine = new ProverStateMachine(config.rules);
		List<Dob> roles = Game.getRoles(config.rules);
		Map<Player, Dob> playerRoles = Maps.newHashMap();
		List<Thread> threads = Lists.newArrayList();
		
		// Map players to roles
		for (int i = 0; i < roles.size(); i++) {
			Player player = i < players.size() ? players.get(i) : new Player.Legal();
			playerRoles.put(player, roles.get(i));
		}
		
		// Construct harnesses for players
		for (Player player : players) threads.add(new Thread(player));
		
		// Configure players
		for (Player player : players) {
			Dob role = playerRoles.get(player);
			player.setMatch(role, config);
		}
		
		// Start players
		for (Thread thread : threads) thread.start();
		if (!Synchron.lightSleep(config.startclock)) return;
		
		// Run game
		Map<Dob, Dob> actions = Maps.newHashMap();
		Set<Dob> state = machine.getInitial();
		int turn = 0;
		while (!machine.isTerminal(state)) {
			// Extract decided moves from players
			Map<Dob, Dob> next = Maps.newHashMap();
			for (Player player : players) {
				Dob role = playerRoles.get(player);
				next.put(role, player.getMove(turn));
			}
			actions = next;
			
			// Tell players to start thinking about the next move
			for (Player player : players) player.advance(turn, actions);
			
			// Let players think
			if (!Synchron.lightSleep(config.playclock)) return;
			
			state = machine.nextState(state, actions);
			turn++;
		}
	}
}
