package rekkura.test.ggp;

import org.junit.Assert;
import org.junit.Test;

import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.MatchRunnable;

public class MatchRunnableTest {
	
	private static final int SMALL_STARTCLOCK = 20;
	private static final int SMALL_PLAYCLOCK = 10;
	
	@Test
	public void basic() {
		Game.Config config = new Game.Config(SMALL_STARTCLOCK, SMALL_PLAYCLOCK, SimpleGames.getTrivial());
		MatchRunnable match = new MatchRunnable(config);
		match.run();

		Assert.assertEquals(0, match.timeouts.size());
		Assert.assertEquals(1, match.history.size());
	}
	
	@Test
	public void timeoutRecord() {
		Game.Config config = new Game.Config(0, 0, SimpleGames.getTrivial());
		MatchRunnable match = new MatchRunnable(config);
		match.run();
		
		Assert.assertEquals(1, match.timeouts.size());
	}
	
	@Test
	public void ticTacToe() {
		Game.Config config = new Game.Config(1000, 1000, SimpleGames.getTicTacToe());
		MatchRunnable match = new MatchRunnable(config);
		match.run();

	}
	
}
