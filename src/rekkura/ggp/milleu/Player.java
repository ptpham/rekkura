package rekkura.ggp.milleu;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.model.StateMachine;
import rekkura.util.Colut;
import rekkura.util.Synchron;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public abstract class Player implements Runnable {

	protected Dob role;
	protected Game.Config config;
	
	private final Vector<Dob> decisions = Synchron.newVector();
	private final Vector<Map<Dob, Dob>> history = Synchron.newVector();
	private boolean started = false;
	
	/**
	 * A player may only be started once.
	 * @param role
	 * @param config
	 */
	public final synchronized void setMatch(Dob role, Game.Config config) {
		if (started) throw new IllegalStateException("Player already started a game!");
		this.role = role;
		this.config = config;
		this.started = true;
		this.notifyAll();
	}
	
	public boolean isStarted() { return this.started; }
	
	public final void advance(int turn, Iterable<Dob> actions) {
		Map<Dob, Dob> actionMap = Maps.newHashMap();
		for (Dob action : actions) { actionMap.put(Game.getRoleForAction(action), action); }
		advance(turn, actionMap);
	}
	
	public final synchronized void advance(int turn, Map<Dob, Dob> actions) { 
		Colut.addAt(history, turn, actions);
		this.notifyAll();
	}
	
	protected final synchronized void waitForInput() {
		Synchron.lightWait(this);
	}
	
	protected final synchronized int getHistoryExtent() { return this.history.size(); }
	protected final synchronized Map<Dob, Dob> getMemory(int turn) { return Colut.get(history, turn); }
	
	public final synchronized boolean hasAction(int turn) { return Colut.get(decisions, turn) != null; }
	public final synchronized Dob getAction(int turn) { return Colut.get(decisions, turn); }
	protected final synchronized void setDecision(int turn, Dob dob) { Colut.addAt(decisions, turn, dob); }
	protected final synchronized void setDecision(Game.Decision move) { this.setDecision(move.turn, move.action); }
	
	/**
	 * This represents a player that needs to update the state of the game using a state 
	 * machine. Most players will want to derive from this.
	 * @author ptpham
	 *
	 * @param <M>
	 */
	public static abstract class StateBased<M extends StateMachine<Set<Dob>, Dob>> extends Player {
		private Set<Dob> state;
		private int turn;
		
		protected M machine;
		
		protected abstract M constructMachine(Collection<Rule> rules);
		protected abstract void prepare();
		protected abstract void move();
		protected abstract void reflect();
		
		protected synchronized Game.Turn getTurn() { return new Game.Turn(this.turn, this.state); }
		
		protected Game.Decision anyDecision() {
			Game.Turn turn = this.getTurn();
			Multimap<Dob, Dob> actions = this.machine.getActions(turn.state);
			return new Game.Decision(turn.turn, Colut.any(actions.get(this.role))); 
		}
		
		@Override
		public final void run() {
			while (!this.isStarted()) waitForInput();
			this.machine = constructMachine(config.rules);
			while (true) {
				updateState();
				if (isTerminal()) break;
				if (this.turn == 0) prepare();
				else move();
			}
			reflect();
		}
		
		private synchronized boolean isTerminal() {
			return this.state != null && this.machine.isTerminal(state);
		}
		
		private synchronized void updateState() {
			if (this.state == null) {
				state = this.machine.getInitial();
				return;
			}

			while (validState()) waitForInput();
			
			while (!validState()) {
				this.state = this.machine.nextState(state, getMemory(turn));
				turn++;
			}
		}
		
		protected synchronized boolean validState() { 
			return this.turn == getHistoryExtent(); 
		}
	}
	
	public static abstract class ProverBased extends StateBased<BackwardStateMachine> {
		@Override
		protected BackwardStateMachine constructMachine(Collection<Rule> rules) {
			return BackwardStateMachine.createForRules(rules);
		}
		
		@Override
		protected final void prepare() {
			this.role = machine.prover.pool.submerge(role);
			plan();
		}
		
		protected abstract void plan();
	}
	
	public static class Unresponsive extends ProverBased {
		@Override protected void plan() { }
		@Override protected void move() { }
		@Override protected void reflect() { }
	}

	/**
	 * This player will respond with decisions that are given to it 
	 * when constructed.
	 * @author ptpham
	 *
	 */
	public class FixedPlayer extends Player {
		private final Player inner;
		public FixedPlayer(Iterable<Game.Decision> moves) { this(null, moves); }
		public FixedPlayer(Player inner, Iterable<Game.Decision> moves) {
			this.inner = inner;
			for (Game.Decision move : moves) this.setDecision(move);
		}
	
		@Override public void run() { if (this.inner != null) inner.run(); }
	}
	
	public static class Legal extends ProverBased {
		@Override protected void plan() { setDecision(anyDecision()); }
		@Override protected void move() { setDecision(anyDecision()); }
		@Override protected void reflect() { }
	}
}
