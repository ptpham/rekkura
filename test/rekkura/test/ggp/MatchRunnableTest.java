package rekkura.test.ggp;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.MatchRunnable;
import rekkura.ggp.milleu.Player;

import com.google.common.collect.Lists;

public class MatchRunnableTest {
	
	@Test
	public void basic() {
		Game.Config config = GgpTestUtil.createBlitzConfig(SimpleGames.getTrivial());
		MatchRunnable match = new MatchRunnable(config);
		match.run();

		Assert.assertEquals(0, match.timeouts.size());
		Assert.assertEquals(1, match.history.size());
	}
	
	@Test
	public void timeoutRecord() {
		Game.Config config = new Game.Config(0, 0, SimpleGames.getTrivial());
		List<Player> players = Lists.<Player>newArrayList(new Player.Unresponsive());
		MatchRunnable match = new MatchRunnable(config, players);
		match.run();
		
		Assert.assertEquals(1, match.timeouts.size());
	}
	
	@Test
	public void ticTacToe() {
		Game.Config config = GgpTestUtil.createBlitzConfig(SimpleGames.getTicTacToe());
		MatchRunnable match = new MatchRunnable(config);
		match.run();

		Assert.assertTrue(match.goals.size() > 0);
	}
	
}
