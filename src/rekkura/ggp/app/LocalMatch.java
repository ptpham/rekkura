package rekkura.ggp.app;

import java.util.Collections;
import java.util.List;

import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Match;
import rekkura.ggp.milleu.MatchRunnable;
import rekkura.ggp.milleu.Player;
import rekkura.ggp.player.ConsolePlayer;
import rekkura.ggp.player.MonteCarloPlayer;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;
import rekkura.util.Event;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class LocalMatch {

	public static void main(String args[]) {
		int startclock = 1000000, playclock = 1000000;
		List<Rule> rules = SimpleGames.getConnectFour();
		Game.Config config = new Game.Config(startclock, playclock, rules);
		
		EventBus bus = new EventBus();
		Event.register(bus, new Listener(Thread.currentThread()));
		
		ConsolePlayer human = new ConsolePlayer();
		human.bus = bus;
		
		List<Player> players = Lists.newArrayList();
		Collections.addAll(players, human, new MonteCarloPlayer());
		MatchRunnable runner = Match.newBuilder(config).build().newRunnable(players);
		runner.run();
	}
	
	private static class Listener {
		Thread matchThread;
		public Listener(Thread matchThread) { this.matchThread = matchThread; }
		
		@Subscribe
		public void handleHumanMove(ConsolePlayer.MoveEvent ev) {
			this.matchThread.interrupt();
		}
	}
	
}
