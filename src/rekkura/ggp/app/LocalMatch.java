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

import com.google.common.collect.Lists;

public class LocalMatch {

	public static void main(String args[]) {
		int startclock = 10000, playclock = 10000;
		List<Rule> rules = SimpleGames.getConnectFour();
		Game.Config config = new Game.Config(startclock, playclock, rules);
		
		List<Player> players = Lists.newArrayList();
		Collections.addAll(players, new ConsolePlayer(), new MonteCarloPlayer());
		Match.Builder builder = Match.newBuilder(config, Match.wrap(players));
		MatchRunnable runner = builder.buildRunnable();
		runner.run();
	}
	
}
