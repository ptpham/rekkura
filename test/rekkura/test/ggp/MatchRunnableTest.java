package rekkura.test.ggp;

import org.junit.Assert;
import org.junit.Test;

import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Match;
import rekkura.ggp.milleu.MatchRunnable;
import rekkura.ggp.milleu.Player;

public class MatchRunnableTest {
	
	@Test
	public void basic() {
		Game.Config config = GgpTestUtil.createBlitzConfig(SimpleGames.getTrivial());
		MatchRunnable match = Match.newBuilder(config).build().newRunnable();
		match.run();

		Assert.assertEquals(0, match.timeouts.size());
		Assert.assertEquals(1, match.history.size());
	}
	
	@Test
	public void timeoutRecord() {
		Game.Config config = new Game.Config(0, 0, SimpleGames.getTrivial());
		MatchRunnable match = Match.newBuilder(config).build().newRunnable(new Player.Unresponsive());
		match.run();
		
		Assert.assertEquals(1, match.timeouts.size());
	}
	
	@Test
	public void goalsRegistered() {
		Game.Config config = GgpTestUtil.createBlitzConfig(SimpleGames.getTrivial());
		MatchRunnable match = Match.newBuilder(config).build().newRunnable();
		match.run();

		Assert.assertTrue(match.goals.size() > 0);
	}
}
