package rekkura.ggp.milleu;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import rekkura.ggp.machina.ProverStateMachine;
import rekkura.logic.model.Dob;
import rekkura.util.Colut;
import rekkura.util.Event;
import rekkura.util.Synchron;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;

/**
 * A {@link MatchRunnable} provides the logic for pitting two players
 * against each other. It will take care of spawning a new thread for
 * each player and record what happens during the match with synchronized
 * data structures.
 * @author ptpham
 *
 */
public class MatchRunnable implements Runnable {
	public final Vector<Game.Record> history = Synchron.newVector();
	public final Vector<Player> players = Synchron.newVector();
	
	public final Multimap<Integer, Player> timeouts = Synchron.newHashMultimap();
	public final Map<Dob, Integer> goals = Synchron.newHashmap();
	public final Match match;

	private final EventBus bus;
	private class WithMatchRunnable { public final MatchRunnable match = MatchRunnable.this; }

	public class TimeoutEvent extends WithMatchRunnable { }
	public class StartEvent extends WithMatchRunnable { }
	public class PlayEvent extends WithMatchRunnable { }
	public class StopEvent extends WithMatchRunnable { }
	public class GoalEvent extends WithMatchRunnable { }

	public MatchRunnable(Match match) { this(match, null); }
	public MatchRunnable(Match match, EventBus bus) {
		this.match = match;
		this.bus = bus;
	}
	
	@Override
	public void run() {
		Game.Config config = match.config;
		ProverStateMachine machine = ProverStateMachine.createWithStratifiedBackward(config.rules);
		List<Dob> roles = Game.getRoles(config.rules);
		Map<Player, Dob> playerRoles = Maps.newHashMap();
		List<Thread> threads = preparePlayers(roles, playerRoles);
		
		// Start players
		for (Thread thread : threads) thread.start();
		Synchron.lightSleep(config.startclock);
		Event.post(bus, new StartEvent());
		
		// Run game
		int turn = 0;
		Map<Dob, Dob> actions = Maps.newHashMap();
		Set<Dob> state = machine.getInitial();
		boolean terminal = machine.isTerminal(state);
		while (!terminal) {
			// Extract decided moves from players
			Multimap<Dob, Dob> legal = machine.getActions(state);
			Map<Dob, Dob> next = Maps.newHashMap();
			for (Player player : players) {
				Dob role = playerRoles.get(player);
				Dob move = player.getDecision(turn);
				if (move == null) {
					move = Colut.any(legal.get(role));
					this.timeouts.put(turn, player);
					Event.post(bus, new TimeoutEvent());
				}
				next.put(role, move);
			}
			actions = next;
			
			// Generate the next state
			state = machine.nextState(state, actions);
			terminal = machine.isTerminal(state);
			turn++;

			// Update records and inform players
			this.history.add(new Game.Record(state, actions));
			if (!terminal) handlePlay(actions);
			else handleStop(actions);
		}
		
		// Compute the goals for the players
		this.goals.putAll(machine.getGoals(state));
		Event.post(bus, new GoalEvent());
	}

	private List<Thread> preparePlayers(List<Dob> roles, Map<Player, Dob> playerRoles) {
		List<Thread> threads = Lists.newArrayList();
		while (players.size() < roles.size()) players.add(new Player.Legal());
		
		// Map players to roles
		for (int i = 0; i < roles.size(); i++) playerRoles.put(players.get(i), roles.get(i));
		
		// Construct harnesses for players
		for (Player player : players) threads.add(new Thread(player));
		
		// Configure players
		for (Player player : players) {
			Dob role = playerRoles.get(player);
			player.setMatch(role, match.config);
		}
		return threads;
	}
	
	/**
	 * Provides moves to players in such a way that signals the
	 * end of the game.
	 * @param actions
	 */
	private void handleStop(Map<Dob, Dob> actions) {
		Event.post(bus, new StopEvent());
		for (Player player : players) player.complete(actions);
	}

	/**
	 * Provides moves to players and waits while they think
	 * @param actions
	 */
	private void handlePlay(Map<Dob, Dob> actions) {
		Event.post(bus, new PlayEvent());
		for (Player player : players) player.advance(actions);
		Synchron.lightSleep(match.config.playclock);
	}

}
