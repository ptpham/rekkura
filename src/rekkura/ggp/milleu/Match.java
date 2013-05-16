package rekkura.ggp.milleu;

import java.net.URL;
import java.util.Collection;
import java.util.List;

import rekkura.ggp.milleu.Game.Config;
import rekkura.ggp.net.RemotePlayer;
import rekkura.model.Dob;
import rekkura.util.Netut;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class Match {
	public final Game.Config config;
	public final String id, game;
	public final ImmutableList<PlayerPointer> pointers;
	
	private Match(String id, String game, Game.Config config, List<PlayerPointer> pointers) {
		this.pointers = ImmutableList.copyOf(pointers);
		this.config = config;
		this.game = game;
		this.id = id;
	}

	public static class PlayerPointer {
		public final Player local;
		public final URL remote;
		public PlayerPointer(Player local) { this.local = local; this.remote = null; }
		public PlayerPointer(URL remote) { this.remote = remote; this.local = null; }
		public PlayerPointer() { this.remote = null; this.local = null; }
	}

	public static class Builder {
		public String match, game;
		public Game.Config config;
		public List<PlayerPointer> pointers = Lists.newArrayList();
		public Match build() { return new Match(match, game, config, pointers); }
		public MatchRunnable buildRunnable() { return new MatchRunnable(build()); }
	}
	
	public static List<PlayerPointer> wrap(Player... players) {
		return wrap(Lists.newArrayList(players));
	}
	
	public static List<PlayerPointer> wrap(Collection<Player> players) {
		List<PlayerPointer> result = Lists.newArrayList();
		for (Player player : players) result.add(new PlayerPointer(player));
		return result;
	}
	
	public static Builder newBuilder(Game.Config config) {
		return newBuilder(config, Lists.<PlayerPointer>newArrayList());
	}

	public static Builder newBuilder(Config config, List<PlayerPointer> players) {
		Builder result = new Builder();
		result.pointers.addAll(players);
		result.config = config;
		return result;
	}
	
	public static PlayerPointer getRemote(URL url) { return new PlayerPointer(url); }
	public static PlayerPointer getRemote(String url) { return getRemote(Netut.lightUrl(url)); }

	/**
	 * Render players and fills in missing players with legals
	 * @param match
	 * @param roles
	 * @return
	 */
	public List<Player> renderPlayers() {
		List<Dob> roles = Game.getRoles(config.rules);
		return renderPlayerPointers(this.pointers, roles, this.id);
	}

	public static List<Player> renderPlayerPointers(
		List<PlayerPointer> pointers, List<Dob> roles, String id) {
		
		pointers = Lists.newArrayList(pointers);
		while (pointers.size() < roles.size()) pointers.add(new Match.PlayerPointer());
		
		List<Player> players = Lists.newArrayList();
		for (Match.PlayerPointer pointer : pointers) {
			if (pointer.local != null) {
				players.add(pointer.local);
			} else if (pointer.remote != null) {
				players.add(new RemotePlayer(id, pointer.remote));
			} else {
				players.add(new Player.Legal());
			}
		}
		return players;
	}
}
