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
		MatchRunnable match = Match.newBuilder(config).buildRunnable();
		match.run();

		Assert.assertEquals(0, match.timeouts.size());
		Assert.assertEquals(1, match.history.size());
	}
	
	@Test
	public void timeoutRecord() {
		Game.Config config = new Game.Config(0, 0, SimpleGames.getTrivial());
		Match.Builder builder = Match.newBuilder(config, Match.wrap(new Player.Unresponsive()));
		MatchRunnable match = builder.buildRunnable();
		match.run();
		
		Assert.assertEquals(1, match.timeouts.size());
	}
	
	@Test
	public void goalsRegistered() {
		Game.Config config = GgpTestUtil.createBlitzConfig(SimpleGames.getTrivial());
		MatchRunnable match = Match.newBuilder(config).buildRunnable();
		match.run();

		Assert.assertTrue(match.goals.size() > 0);
	}
}
