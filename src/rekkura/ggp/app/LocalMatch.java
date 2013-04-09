package rekkura.ggp.app;

import java.util.List;

import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.MatchRunnable;
import rekkura.ggp.players.ConsolePlayer;
import rekkura.ggp.players.MonteCarloPlayer;
import rekkura.model.Rule;
import rekkura.test.ggp.SimpleGames;

public class LocalMatch {

	public static void main(String args[]) {
		int startclock = 30000, playclock = 30000;
		List<Rule> rules = SimpleGames.getConnectFour();
		Game.Config config = new Game.Config(startclock, playclock, rules);
		MatchRunnable runner = new MatchRunnable(config, new ConsolePlayer(), new MonteCarloPlayer());
		runner.run();
	}
	
}
