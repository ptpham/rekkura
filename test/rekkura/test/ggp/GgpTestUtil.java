package rekkura.test.ggp;

import java.util.List;

import rekkura.ggp.milleu.Game;
import rekkura.model.Rule;

public class GgpTestUtil {

	public static final int BLITZ_STARTCLOCK = 20;
	public static final int BLITZ_PLAYCLOCK = 10;
	
	public static final int FAST_STARTCLOCK = 400;
	public static final int FAST_PLAYCLOCK = 200;
	
	public static Game.Config createBlitzConfig(List<Rule> rules) {
		return new Game.Config(BLITZ_STARTCLOCK, BLITZ_PLAYCLOCK, rules);
	}
	
	public static Game.Config createFastConfig(List<Rule> rules) {
		return new Game.Config(FAST_STARTCLOCK, FAST_PLAYCLOCK, rules);
	}

	
}
