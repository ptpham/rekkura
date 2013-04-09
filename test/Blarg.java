import org.junit.Assert;
import org.junit.Test;

import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.MatchRunnable;
import rekkura.ggp.players.MonteCarloPlayer;
import rekkura.test.ggp.GgpTestUtil;
import rekkura.test.ggp.SimpleGames;


public class Blarg {


	@Test
	public void ticTacToe() {
		Game.Config config = GgpTestUtil.createMediumConfig(SimpleGames.getConnectFour());
		
		MonteCarloPlayer player = new MonteCarloPlayer();
		MatchRunnable match = new MatchRunnable(config, player);
		match.run();
		
		System.out.println(player.getWavesComputed());
	}
}
