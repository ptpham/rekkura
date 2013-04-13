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
import rekkura.util.Reffle;
import rekkura.util.Synchron;

import com.google.common.collect.ImmutableList;
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

	public static int getGgpPlayClock(Game.Config config) {
		return config.playclock;
	}
	
	public static class GgpState {
		public final Player player;
		public final ImmutableList<Dob> roles;
		public final int ggpPlayClock, ggpStartClock;
		public final Thread thread;
		
		public long touch;
		public int turn;

		public GgpState(int ggpPlayClock, int ggpStartClock, 
				Iterable<Dob> roles, Player player, Thread thread) {
			this.ggpPlayClock = ggpPlayClock;
			this.ggpStartClock = ggpStartClock;
			this.roles = ImmutableList.copyOf(roles);
			this.player = player;
			this.thread = thread;
			this.touch = System.currentTimeMillis();
		}
	}
	
	/**
	 * The DefaultPlayerHandler handles conversions between 
	 * actions (the internal representation) and moves (the GGP 
	 * protocol standard). It also manages time for the player.
	 * 
	 * A new player and a new thread will be created for every 
	 * new match received.
	 * @author ptpham
	 *
	 */
	public static class DefaultPlayerHandler<P extends Player> implements PlayerHandler {
		public final Reffle.Factory<P> factory;
		public final Map<String, GgpState> players = Synchron.newHashmap();
		
		private DefaultPlayerHandler(Reffle.Factory<P> factory) { this.factory = factory; }
		private static final int EPSILON = 200;
		
		@Override
		public PlayerState handleStart(String match, Dob role, Config config) {
			System.out.println("Handling start");
			GgpState state = players.get(match);
			int ggpPlayClock = getGgpPlayClock(config);
			int ggpStartClock = getGgpStartClock(config);
			List<Dob> roles = Game.getRoles(config.rules);
			
			if (state == null) {
				try {
					Player player = factory.create();
					Thread thread = Player.start(player);
					state =  new GgpState(ggpPlayClock, ggpStartClock, roles, player, thread);
					this.players.put(match, state);
				} catch (Throwable e) {
					e.printStackTrace();
					System.err.println("Note: Players must have an empty constructor!");
					return PlayerState.BUSY;
				}
			}
			
			state.player.setMatch(role, config);
			
			Synchron.lightSleep(ggpStartClock - EPSILON);
			return PlayerState.READY;
		}

		@Override
		public Dob handlePlay(String match, List<Dob> moves) {
			System.out.println("Handling play");
			GgpState state = this.players.get(match);
			if (state == null) return new Dob("Ain't nobody playing that!");
			
			// This condition is necessary because the first
			// play move in the GGP protocol doesn't have any moves.
			// Thus, we don't want to advance the state.
			if (moves.size() == state.roles.size()) {
				Map<Dob, Dob> actions = Game.convertMovesToActionMap(state.roles, moves);
				state.player.advance(state.turn++, actions);
			}
			Synchron.lightSleep(state.ggpPlayClock - EPSILON);
			
			Dob action = state.player.getDecision(state.turn);
			return Game.convertActionToMove(action);
		}

		@Override
		public PlayerState handleStop(String match, List<Dob> moves) {
			System.out.println("Handling stop");
			GgpState state = this.players.get(match);
			if (state == null) return PlayerState.BUSY;

			Map<Dob, Dob> actions = Game.convertMovesToActionMap(state.roles, moves);
			state.player.advance(state.turn, actions);
			this.players.remove(match);
			
			return PlayerState.DONE;
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
	public static class DefaultPlayerDemuxer implements PlayerDemuxer {
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
			System.out.println(message);
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
	
	public static <P extends Player> DefaultPlayerDemuxer createDefaultPlayerDemuxer(Class<P> type) {
		Reffle.Factory<P> factory = Reffle.createFactory(type);
		return new DefaultPlayerDemuxer(new DefaultPlayerHandler<P>(factory));
	}
	
	public static <P extends Player> DefaultPlayerHandler<P> createDefaultPlayerHandler(Class<P> type) {
		Reffle.Factory<P> factory = Reffle.createFactory(type);
		return new DefaultPlayerHandler<P>(factory);
	}
}
