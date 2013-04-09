package rekkura.ggp.net;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import rekkura.fmt.KifFormat;
import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Game.Config;
import rekkura.ggp.milleu.Player;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Colut;
import rekkura.util.Synchron;

import com.google.common.collect.Lists;

public class GgpProtocol {

	public static enum PlayerState { READY, BUSY, DONE }
	
	/**
	 * A PlayerHandler is responsible for handling the logical
	 * essence of the GGP protocol. See DefaultPlayerHandler for
	 * an example.
	 * @author ptpham
	 *
	 */
	public static interface PlayerHandler {
		PlayerState handleStart(String match, Dob role, Game.Config config);
		Dob handlePlay(String match, List<Dob> moves);
		PlayerState handleStop(String match, List<Dob> moves);
	}
	
	public static int getGgpStartClock(Game.Config config) {
		return config.startclock - config.playclock;
	}
	
	/**
	 * The DefaultPlayerHandler handles conversions between 
	 * actions (the internal representation) and moves (the GGP 
	 * protocol standard). It also manages time for the player.
	 * The assumption here is that the Player passed in is running
	 * on a different thread. Sad times will ensue otherwise.
	 * @author ptpham
	 *
	 */
	private static class DefaultPlayerHandler implements PlayerHandler {
		public final Player player;
		
		List<Dob> roles;
		private String current;
		private int turn, ggpPlayClock, ggpStartClock;
		
		private DefaultPlayerHandler(Player player) { this.player = player; }
		private static final int EPSILON = 300;
		
		@Override
		public PlayerState handleStart(String match, Dob role, Config config) {
			if (!validMatch(match)) return PlayerState.BUSY;
			
			turn = 0;
			current = match;
			player.setMatch(role, config);
			ggpPlayClock = config.playclock;
			ggpStartClock = getGgpStartClock(config);
			roles = Game.getRoles(config.rules);
			
			Synchron.lightSleep(ggpStartClock - EPSILON);
			return PlayerState.READY;
		}

		@Override
		public Dob handlePlay(String match, List<Dob> moves) {
			if (!validMatch(match)) return new Dob("");
			
			// This condition is necessary because the first
			// play move in the GGP protocol doesn't have any moves.
			// Thus, we don't want to advance the state.
			if (moves.size() == roles.size()) {
				Map<Dob, Dob> actions = Game.convertMovesToActionMap(roles, moves);
				player.advance(turn++, actions);
			}
			Synchron.lightSleep(ggpPlayClock - EPSILON);
			
			Dob action = player.getDecision(turn);
			return Game.convertActionToMove(action);
		}

		@Override
		public PlayerState handleStop(String match, List<Dob> moves) {
			if (!validMatch(match)) return PlayerState.BUSY;
			current = null;

			System.out.println(roles + " " + moves);
			Map<Dob, Dob> actions = Game.convertMovesToActionMap(roles, moves);
			player.advance(turn, actions);
			return PlayerState.DONE;
		}
		
		private boolean validMatch(String match) {
			return current == null || current.equals(match);
		}
	}
		
	/**
	 * A PlayerDemuxer is responsible for handling GGP messages in general.
	 * The DefaultPlayerDemuxer will delegate the logic to a PlayerHandler.
	 * @author ptpham
	 *
	 */
	public static interface PlayerDemuxer { 
		String handleMessage(String message);
	}
	
	/**
	 * The DefaultPlayerDemuxer acts as a layer been a PlayerHandler and 
	 * the outside world. It converts the raw string it receives to logic 
	 * that can be passed down to the PlayerHandler. The PlayerHandler will 
	 * pass back some kind of logic. The demux converts the logic back into 
	 * a String and passes it back to the outside world.
	 * @author ptpham
	 *
	 */
	private static class DefaultPlayerDemuxer implements PlayerDemuxer {
		private final KifFormat fmt = new KifFormat();
		private PlayerHandler handler;
		
		public static String START_NAME = "start";
		public static String PLAY_NAME = "play";
		public static String STOP_NAME = "stop";
		
		public static EnumMap<PlayerState, Dob> PLAYER_STATE_DOBS = 
			new EnumMap<PlayerState, Dob>(PlayerState.class);
		{
			PLAYER_STATE_DOBS.put(PlayerState.READY, new Dob("ready"));
			PLAYER_STATE_DOBS.put(PlayerState.BUSY, new Dob("busy"));
			PLAYER_STATE_DOBS.put(PlayerState.DONE, new Dob("done"));
		}
		
		private DefaultPlayerDemuxer(PlayerHandler handler) {
			this.handler = handler;
		}

		@Override
		public String handleMessage(String message) {
			Dob dob = fmt.dobFromString(message);
			String name = stringAt(dob, 0);
			
			String result = "";
			try {
				if (name.equals(PLAY_NAME)) result = play(dob);
				else if (name.equals(START_NAME)) result = start(dob);
				else if (name.equals(STOP_NAME)) result = stop(dob);
			} catch (Throwable t) { t.printStackTrace(); }
			
			return result;
		}

		private String stop(Dob dob) {
			String result;
			String match = stringAt(dob, 1);
			List<Dob> moves = dob.at(2).childCopy();
			PlayerState state = handler.handleStop(match, moves);
			result = fmt.toString(PLAYER_STATE_DOBS.get(state));
			return result;
		}

		private String start(Dob dob) {
			String result;
			String match = stringAt(dob, 1);
			Dob role = dob.at(2);
			
			// Vacuous rules are generated for dobs that carry a string that 
			// does not properly parse as a rule. This is kind of a gross
			// special case for KIF because it is a poorly designed format. =P
			List<Dob> rawRules = dob.at(3).childCopy();
			List<Rule> rules = Lists.newArrayList();
			for (Dob rawRule : rawRules) { 
				try { rules.add(fmt.ruleFromString(fmt.toString(rawRule))); }
				catch (Exception e) { rules.add(Rule.asVacuousRule(rawRule)); }
			}
			
			rules = deorPass(rules, fmt);
			
			int ggpStart = Colut.parseInt(stringAt(dob, dob.size() - 2))*1000;
			int ggpPlay = Colut.parseInt(stringAt(dob, dob.size() - 1))*1000;
			
			Game.Config config = new Game.Config(ggpStart + ggpPlay, ggpPlay, rules);
			PlayerState state = handler.handleStart(match, role, config);
			result = fmt.toString(PLAYER_STATE_DOBS.get(state));
			return result;
		}
		
		private String play(Dob dob) {
			String result = "";
			List<Dob> moves = dob.at(2).childCopy();
			result = fmt.toString(handler.handlePlay(stringAt(dob, 1), moves));
			return result;
		}
		
		private String stringAt(Dob message, int position) {
			if (message.size() <= position) return "";
			return message.at(position).name.trim().toLowerCase();
		}
	}

	/**
	 *  In a glorious future in which we don't have ORs anymore,
	 *  this class could be made more general and this could be removed.
	 * @param rules
	 * @return
	 */
	public static List<Rule> deorPass(List<Rule> rules, KifFormat fmt) {
		List<Rule> result = Lists.newArrayList();
		for (Rule rule : rules) { 
			for (Rule expanded : fmt.deor(rule)) { 
				Rule cleaned = fmt.ruleFromString(fmt.toString(expanded));
				result.add(cleaned);
			}
		}
		return result;
	}
	
	public static PlayerDemuxer createDefaultPlayerDemuxer(Player player) {
		return new DefaultPlayerDemuxer(new DefaultPlayerHandler(player));
	}
}
