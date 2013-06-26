package rekkura.test.ggp.machina;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.ggp.milleu.Game;
import rekkura.ggp.player.MonteCarloPlayer;
import rekkura.logic.format.StandardFormat;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.GgpTestUtil;
import rekkura.test.ggp.SimpleGames;
import rekkura.util.Synchron;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
		
		Game.Config config = GgpTestUtil.createBlitzConfig(game);
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
		
		// Yeah this is a total hack.
		Map<Dob, Dob> actionMap = Maps.newHashMap();
		for (Dob action : actions) { actionMap.put(action, action); }
		
		// Advance the player to a state where there
		// should be a clear best move.
		player.setMatch(role, config);
		player.advance(actionMap);
		
		Thread thread = new Thread(player);
		thread.start();
		
		while (player.getWavesComputed() < 1) { Synchron.lightSleep(50); }
		
		String taken = StandardFormat.inst.toString(player.getDecision(1));
		Assert.assertEquals("((does)(o)((mark)(2)(2)))", taken);
	}
}
