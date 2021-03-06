package rekkura.test.ggp;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Match;
import rekkura.ggp.milleu.MatchRunnable;
import rekkura.ggp.milleu.Player;

public class MatchRunnableTest {
	
	@Test
	public void basic() {
		Game.Config config = new Game.Config(0, 0, SimpleGames.getTrivial());
		MatchRunnable match = Match.newBuilder(config).build().newRunnable(new Player.Unresponsive());
		match.run();
		
		assertEquals(1, match.timeouts.size());
		assertEquals(1, match.history.size());
		Assert.assertTrue(match.goals.size() > 0);
	}
}
