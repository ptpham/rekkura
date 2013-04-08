package rekkura.test.ggp;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.fmt.StandardFormat;
import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.MonteCarloPlayer;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Synchron;

import com.google.common.collect.Lists;

public class MonteCarloPlayerTest {

	/**
	 * This loosely tests performance in addition
	 * to correctness. If this fails sometimes, you may have 
	 * made something too slow.
	 */
	@Test
	public void basic() {
		MonteCarloPlayer player = new MonteCarloPlayer();
		List<Rule> game = SimpleGames.getTicTacToe();
		
		Game.Config config = GgpTestUtil.createFastConfig(game);
		Dob role = StandardFormat.inst.dobFromString("(o)");
		List<String> rawActions = Lists.newArrayList(
				"((does)(x)((mark)(3)(3)))", 
				"((does)(x)((mark)(1)(1)))", 
				"((does)(x)((mark)(1)(3)))", 
				"((does)(x)((mark)(3)(2)))", 
				"((does)(o)((mark)(1)(2)))",
				"((does)(o)((mark)(2)(1)))",
				"((does)(o)((mark)(2)(3)))");
		
		List<Dob> actions = StandardFormat.inst.dobsFromStrings(rawActions);
		
		// Advance the player to a state where there
		// should be a clear best move.
		player.setMatch(role, config);
		player.advance(0, actions);
		
		Thread thread = new Thread(player);
		thread.start();
		
		while (player.getWavesComputed() < 1) { Synchron.lightSleep(100); }
		
		String taken = StandardFormat.inst.toString(player.getAction(1));
		Assert.assertEquals("((does)(o)((mark)(2)(2)))", taken);
	}
}
