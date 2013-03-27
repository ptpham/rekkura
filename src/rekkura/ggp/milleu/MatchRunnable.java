package rekkura.ggp.milleu;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import rekkura.ggp.machina.ProverStateMachine;
import rekkura.model.Dob;
import rekkura.util.Colut;
import rekkura.util.Synchron;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class MatchRunnable implements Runnable {
	public final Vector<Game.Record> history = Colut.newVector();
	public final Vector<Player> players = Colut.newVector();
	
	public final Multimap<Integer, Player> timeouts 
		= Multimaps.synchronizedSetMultimap(HashMultimap.<Integer,Player>create());
	public final Game.Config config;
	
	public MatchRunnable(List<Player> players, Game.Config config) {
		if (players == null) players = Lists.newArrayList();
		this.players.addAll(players);
		this.config = config;
	}
	
	public MatchRunnable(Game.Config config) {
		this(null, config);
	}
	
	@Override
	public void run() {
		ProverStateMachine machine = new ProverStateMachine(config.rules);
		List<Dob> roles = Game.getRoles(config.rules);
		Map<Player, Dob> playerRoles = Maps.newHashMap();
		List<Thread> threads = Lists.newArrayList();
		
		// Map players to roles
		while (players.size() < roles.size()) players.add(new Player.Legal());
		for (int i = 0; i < roles.size(); i++) {
			playerRoles.put(players.get(i), roles.get(i));
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
		Synchron.lightSleep(config.startclock);
		
		// Run game
		Map<Dob, Dob> actions = Maps.newHashMap();
		Set<Dob> state = machine.getInitial();
		int turn = 0;
		while (!machine.isTerminal(state)) {
			// Extract decided moves from players
			Multimap<Dob, Dob> legal = machine.getActions(state);
			Map<Dob, Dob> next = Maps.newHashMap();
			for (Player player : players) {
				Dob role = playerRoles.get(player);
				Dob move = player.getMove(turn);
				if (move == null) {
					move = Colut.any(legal.get(role));
					this.timeouts.put(turn, player);
				}
				next.put(role, move);
			}
			actions = next;
			
			// Tell players to start thinking about the next move
			for (Player player : players) player.advance(turn, actions);
			this.history.add(new Game.Record(state, actions));
			
			// Let players think
			Synchron.lightSleep(config.playclock);
			
			// Generate the next state
			state = machine.nextState(state, actions);
			turn++;
		}
	}
}
