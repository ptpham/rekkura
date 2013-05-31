package rekkura.ggp.net;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Player;
import rekkura.logic.format.KifFormat;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.util.Colut;
import rekkura.util.Reffle;
import rekkura.util.Synchron;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * This class holds the two major layers in the GgpProtocol layer.
 * The top layer communicates purely in strings: {@link PlayerDemuxer}.
 * The bottom layer communicates in logic: {@link PlayerHandler}. 
 * @author ptpham
 *
 */
public class GgpProtocol {

	public static enum PlayerState { READY, BUSY, DONE }
	
	public static class Start {
		public final Game.Config config;
		public final Dob role;
		public final String match;
		
		public Start(Game.Config config, Dob role, String match) {
			this.config = config;
			this.role = role;
			this.match = match;
		}
	}

	public static class Turn {
		public final String match;
		public final List<Dob> moves;
		public Turn(String match, List<Dob> moves) {
			this.match = match;
			this.moves = Lists.newArrayList(moves);
		}
	}
	
	public static String START_NAME = "start";
	public static String PLAY_NAME = "play";
	public static String STOP_NAME = "stop";
	public static String NIL_STRING = "nil";
	
	/**
	 * A PlayerHandler is responsible for handling the logical
	 * essence of the GGP protocol. See {@link DefaultPlayerHandler} for
	 * an example.
	 * @author ptpham
	 *
	 */
	public static interface PlayerHandler {
		PlayerState handleStart(String match, Dob role, Game.Config config);
		Dob handlePlay(String match, List<Dob> moves);
		PlayerState handleStop(String match, List<Dob> moves);
	}
	
	/**
	 * Result in milliseconds.
	 * @param config
	 * @return
	 */
	public static long getGgpStartClock(Game.Config config) {
		return config.startclock - config.playclock;
	}

	/**
	 * Result in milliseconds.
	 * @param config
	 * @return
	 */
	public static long getGgpPlayClock(Game.Config config) {
		return config.playclock;
	}
	
	public static class GgpState {
		public final Player player;
		public final ImmutableList<Dob> roles;
		public final long ggpPlayClock, ggpStartClock;
		public final Thread thread;
		
		public long touch;
		public int turn;

		public GgpState(long ggpPlayClock, long ggpStartClock, 
				Iterable<Dob> roles, Player player, Thread thread) {
			this.ggpPlayClock = ggpPlayClock;
			this.ggpStartClock = ggpStartClock;
			this.roles = ImmutableList.copyOf(roles);
			this.player = player;
			this.thread = thread;
			touch();
		}

		public void touch() {
			this.touch = System.currentTimeMillis();
		}
		
		public boolean isExpired() {
			long interval = System.currentTimeMillis() - this.touch;
			long target = Math.max(ggpStartClock, ggpPlayClock);
			return interval > 2*target;
		}
	}
	
	/**
	 * The DefaultPlayerHandler handles conversions between 
	 * actions (the internal representation) and moves (the GGP 
	 * protocol standard). It also manages time for the player. <br>
	 * <br>
	 * A new player and a new thread will be created for every 
	 * new match received.
	 * @author ptpham
	 *
	 */
	public static class DefaultPlayerHandler<P extends Player> implements PlayerHandler {
		public final Reffle.Factory<P> factory;
		public final Map<String, GgpState> players = Synchron.newHashmap();
		
		private DefaultPlayerHandler(Reffle.Factory<P> factory) { this.factory = factory; }
		private static final int PLAY_EPSILON = 200;
		private static final int START_EPSILON = 500;
		
		@Override
		public PlayerState handleStart(String match, Dob role, Game.Config config) {
			cleanPlayers();
			GgpState state = players.get(match);
			long ggpPlayClock = getGgpPlayClock(config);
			long ggpStartClock = getGgpStartClock(config);
			List<Dob> roles = Game.getRoles(config.rules);
			
			if (state != null) return PlayerState.BUSY;
			
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
			
			state.player.setMatch(role, config);
			
			Synchron.lightSleep(ggpStartClock - START_EPSILON);
			return PlayerState.READY;
		}

		@Override
		public Dob handlePlay(String match, List<Dob> moves) {
			cleanPlayers();
			GgpState state = this.players.get(match);
			if (state == null) return new Dob("Ain't nobody playing that!");
			state.touch();

			// This condition is necessary because the first
			// play move in the GGP protocol doesn't have any moves.
			// Thus, we don't want to advance the state.
			if (moves.size() == state.roles.size()) {
				Map<Dob, Dob> actions = Game.convertMovesToActionMap(state.roles, moves);
				state.player.advance(actions);
				state.turn++;
			}
			Synchron.lightSleep(state.ggpPlayClock - PLAY_EPSILON);
			
			Dob action = state.player.getDecision(state.turn);
			if (action == null) return new Dob("[No Turn]");
			return Game.convertActionToMove(action);
		}

		@Override
		public PlayerState handleStop(String match, List<Dob> moves) {
			cleanPlayers();
			GgpState state = this.players.get(match);
			if (state == null) return PlayerState.BUSY;
			state.touch();

			Map<Dob, Dob> actions = Game.convertMovesToActionMap(state.roles, moves);
			state.player.complete(actions);
			
			state.thread.interrupt();
			this.players.remove(match);
			
			return PlayerState.DONE;
		}
		
		/**
		 * Remove players that have not been touched in a while.
		 */
		protected synchronized void cleanPlayers() {
			Iterator<Map.Entry<String, GgpState>> iterator = this.players.entrySet().iterator();
			while (iterator.hasNext()) {
				GgpState state = iterator.next().getValue();
				if (state.isExpired()) iterator.remove();
			}
		}
	}
		
	/**
	 * A PlayerDemuxer is responsible for handling GGP messages in general.
	 * The {@link DefaultPlayerDemuxer} will delegate the logic to a PlayerHandler.
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
			GgpProtocol.Turn stop = dobToTurn(dob);
			PlayerState state = handler.handleStop(stop.match, stop.moves);
			return fmt.toString(PLAYER_STATE_DOBS.get(state));
		}

		private String start(Dob dob) {
			GgpProtocol.Start config = toStart(dob);
			PlayerState state = handler.handleStart(config.match, config.role, config.config);
			return fmt.toString(PLAYER_STATE_DOBS.get(state));
		}
		
		private String play(Dob dob) {
			GgpProtocol.Turn play = dobToTurn(dob);
			return fmt.toString(handler.handlePlay(stringAt(dob, 1), play.moves));
		}
	}
	
	public static <P extends Player> DefaultPlayerDemuxer createDefaultPlayerDemuxer(Class<P> type) {
		Reffle.Factory<P> factory = Reffle.createFactory(type);
		return new DefaultPlayerDemuxer(new DefaultPlayerHandler<P>(factory));
	}
	
	public static <P extends Player> DefaultPlayerHandler<P> createDefaultPlayerHandler(Class<P> type) {
		Reffle.Factory<P> factory = Reffle.createFactory(type);
		return new DefaultPlayerHandler<P>(factory);
	}
	
	public static GgpProtocol.Start toStart(Dob dob) {
		String match = stringAt(dob, 1);
		Dob role = dob.at(2);
		
		// Vacuous rules are generated for dobs that carry a string that 
		// does not properly parse as a rule. This is kind of a gross
		// special case for KIF because it is a poorly designed format. =P
		List<Dob> rawRules = dob.at(3).childCopy();
		List<Rule> rules = KifFormat.dobsToRules(rawRules);
		
		int ggpStart = Colut.parseInt(stringAt(dob, dob.size() - 2))*1000;
		int ggpPlay = Colut.parseInt(stringAt(dob, dob.size() - 1))*1000;
		
		Game.Config config = new Game.Config(ggpStart + ggpPlay, ggpPlay, rules);
		return new GgpProtocol.Start(config, role, match);
	}

	public static Dob fromStart(GgpProtocol.Start start) {
		Game.Config config = start.config;
		
		Dob match = new Dob(start.match);
		Dob role = start.role;
		Dob ggpStart = new Dob(Long.toString(getGgpStartClock(config)/1000));
		Dob ggpPlay = new Dob(Long.toString(getGgpPlayClock(config)/1000));
		
		KifFormat fmt = KifFormat.inst;
		List<Dob> composed = Lists.newArrayList(new Dob(START_NAME), match, role);
		List<Dob> rules = Lists.newArrayList();
		for (Rule rule : config.rules) {
			if (Rule.isVacuous(rule)) rules.add(rule.head.dob);
			else rules.add(fmt.dobFromString(fmt.toString(rule)));
		}
		
		composed.add(new Dob(rules));
		composed.add(ggpStart);
		composed.add(ggpPlay);
		return new Dob(composed);
	}
	
	public static GgpProtocol.Turn dobToTurn(Dob dob) {
		List<Dob> moves = dob.at(2).childCopy();
		return new Turn(stringAt(dob, 1), moves);
	}
	
	public static Dob dobFromPlay(GgpProtocol.Turn move) { return dobFromTurn(move, false); }
	public static Dob dobFromStop(GgpProtocol.Turn move) { return dobFromTurn(move, true); }
	public static Dob dobFromTurn(GgpProtocol.Turn move, boolean last) {
		String type = last ? STOP_NAME : PLAY_NAME;
		List<Dob> elems = Lists.newArrayList(new Dob(type), new Dob(move.match));
		if (Colut.allNulls(move.moves)) elems.add(new Dob(NIL_STRING));
		else elems.add(new Dob(move.moves));
		return new Dob(elems);
	}

	private static String stringAt(Dob message, int position) {
		if (message.size() <= position) return "";
		return message.at(position).name.trim().toLowerCase();
	}
}
