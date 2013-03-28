package rekkura.ggp.milleu;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import rekkura.ggp.machina.ProverStateMachine;
import rekkura.ggp.machina.StateMachine;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Colut;
import rekkura.util.Synchron;

import com.google.common.collect.Multimap;

public abstract class Player implements Runnable {

	protected Dob role;
	protected Game.Config config;
	
	private final Vector<Dob> moves = Colut.newVector();
	private final Vector<Map<Dob, Dob>> history = Colut.newVector();
	
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
	
	public final synchronized void advance(int turn, Map<Dob, Dob> actions) { 
		Colut.addAt(history, turn, actions);
		this.notifyAll();
	}
	
	protected final synchronized int getHistoryExtent() { return this.history.size(); }
	protected final synchronized Map<Dob, Dob> getMoves(int turn) { return Colut.get(history, turn); }
	
	public final synchronized boolean hasMove(int turn) { return Colut.get(moves, turn) != null; }
	public final synchronized Dob getMove(int turn) { return Colut.get(moves, turn); }
	protected final synchronized void setMove(int turn, Dob dob) { Colut.addAt(moves, turn, dob); }
	protected final synchronized void setMove(Game.Move move) { this.setMove(move.turn, move.dob); }
	
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
		protected abstract void plan();
		protected abstract void move();
		protected abstract void reflect();
		
		protected synchronized Game.Turn getTurn() { return new Game.Turn(this.turn, this.state); }
		
		protected Game.Move anyMove() {
			Game.Turn turn = this.getTurn();
			Multimap<Dob, Dob> actions = this.machine.getActions(turn.state);
			return new Game.Move(turn.turn, Colut.any(actions.get(this.role))); 
		}
		
		@Override
		public final void run() {
			while (!this.isStarted()) Synchron.lightWait(this);
			this.machine = constructMachine(config.rules);
			while (!isTerminal()) {
				updateState();
				if (this.turn == 0) plan();
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
			
			while (this.turn == getHistoryExtent()) Synchron.lightWait(this);
			while (!validState()) {
				this.state = this.machine.nextState(state, getMoves(turn));
				turn++;
			}
		}
		
		protected synchronized boolean validState() { 
			return this.turn == getHistoryExtent(); 
		}
	}
	
	public static abstract class ProverBased extends StateBased<ProverStateMachine> {
		@Override
		protected ProverStateMachine constructMachine(Collection<Rule> rules) {
			return ProverStateMachine.createWithStratifiedBackward(rules);
		}
	}
	
	public static class Unresponsive extends ProverBased {
		@Override protected void plan() { }
		@Override protected void move() { }
		@Override protected void reflect() { }
	}
	
	public static class Legal extends ProverBased {
		@Override protected void plan() { setMove(anyMove()); }
		@Override protected void move() { setMove(anyMove()); }
		@Override protected void reflect() { }
	}
}
