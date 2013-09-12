package rekkura.ggp.milleu;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.ggp.machina.GgpStateMachine;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.state.algorithm.DepthCharger;
import rekkura.state.model.StateMachine;
import rekkura.util.Colut;
import rekkura.util.Synchron;

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;

/**
 * A {@link Player} instance is responsible for playing
 * exactly one game. A player can make moves using the 
 * {@code setDecision} method. 
 * <br><br>
 * If you are using a {@link StateMachine.Standard} in any way,
 * consider subclassing from {@link Player.StateBased}
 * or {@link Player.ProverBased}.
 * @author ptpham
 *
 */
public abstract class Player implements Runnable {

	protected Dob role;
	protected Game.Config config;
	
	private final Vector<Dob> moves = Synchron.newVector();
	private final Vector<Map<Dob, Dob>> history = Synchron.newVector();
	private volatile boolean started = false, complete = false;
	private final Object inputLock = new Object();
	
	public volatile Logger logger = Logger.getGlobal();
	{ logger.setUseParentHandlers(false); }
	
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
		notifyInput();
	}
	
	public boolean isStarted() { return this.started; }
	
	public final synchronized void advance(Map<Dob, Dob> actions) {
		Preconditions.checkArgument(!complete, "Player's game is over!");
		appendToHistory(actions);
	}
	
	public final synchronized void complete(Map<Dob, Dob> actions) {
		Preconditions.checkArgument(!complete, "Player's game is already over!");
		complete = true;
		appendToHistory(actions);
	}
	
	public final boolean isComplete() { return complete; }
	
	protected final synchronized boolean waitForInput() { return Synchron.lightWait(this.inputLock); }
	protected final synchronized int getHistoryExtent() { return this.history.size(); }
	protected final synchronized Map<Dob, Dob> getMemory(int turn) { return Colut.get(history, turn); }
	protected final synchronized Map<Dob, Dob> getLatestMemory() { return Colut.end(history); }
	
	public final synchronized boolean hasDecision(int turn) { return Colut.get(moves, turn) != null; }
	public final synchronized Dob getDecision(int turn) { return Colut.get(moves, turn); }
	public final synchronized Dob getLatestDecision() { return Colut.end(moves); }
	protected final synchronized void setDecision(int turn, Dob dob) { Colut.addAt(moves, turn, dob); }
	protected final synchronized void setDecision(Game.Decision decision) { this.setDecision(decision.turn, decision.action); }

	private void appendToHistory(Map<Dob, Dob> actions) {
		Colut.addAt(history, history.size(), actions);
		notifyInput();
	}

	private void notifyInput() {
		synchronized(this.inputLock) { this.inputLock.notifyAll(); }
	}
	
	/**
	 * This represents a player that needs to update the state of the game using a state 
	 * machine. If you are using a prover state machine, you probably want to 
	 * subclass from {@link Player.ProverBased}. One finicky thing about the layering right 
	 * now is that the role {@code Dob} needs to be submerged for provers. {@code Player.ProverBased}
	 * will do this for you.<br>
	 * <br>
	 * {@code prepare} gets called at the beginning of the game and the first move should be set here. <br>
	 * {@code move} gets called on every move after the first. <br>
	 * {@code reflect} gets called once the game is over.<br>
	 * <br>
	 * Use the {@code validState} method to check if you need to return from {@code prepare} or
	 * from {@code move} for this class to handle updating the state properly.
	 * @author ptpham
	 *
	 * @param <M>
	 */
	public static abstract class StateBased<M extends StateMachine.Standard<Set<Dob>, Dob>> extends Player {
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
			ListMultimap<Dob, Dob> actions = this.machine.getActions(turn.state);
			return new Game.Decision(turn.turn, Colut.randomSelection(actions.get(this.role))); 
		}
		
		@Override
		public void run() {
			while (!this.isStarted()) waitForInput();
			
			if (!setup()) return;
			while (!isComplete()) {
				updateState();
				if (isTerminal()) break;
				if (this.turn == 0) prepare();
				else move();
			}
			reflect();
		}
		
		private boolean setup() {
			logger.info("Starting machine construction ...");
			
			// Make sure we can do a typical suite of actions
			// in a reasonable time -- if not, just bail.
			this.machine = constructMachine(config.rules);
			try { DepthCharger.runBasicSuite(this.machine); }
			catch (Exception e) {
				logger.severe("Unable to complete machine construction");
				return false;
			}
			
			logger.info("Finished machine construction.");
			return true;
		}
		
		private synchronized boolean isTerminal() {
			return this.state != null && this.machine.isTerminal(state);
		}
		
		private synchronized void updateState() {
			if (this.state == null) {
				state = this.machine.getInitial();
				return;
			}

			while (!isComplete() && validState()) waitForInput();
			
			while (!isComplete() && !validState()) {
				this.state = this.machine.nextState(state, getMemory(turn));
				turn++;
			}
		}
		
		protected synchronized boolean validState() { 
			return this.turn == getHistoryExtent() && !this.isComplete(); 
		}
	}
	
	public static abstract class ProverBased extends StateBased<BackwardStateMachine> {
		@Override
		protected BackwardStateMachine constructMachine(Collection<Rule> rules) {
			return GgpStateMachine.BACKWARD_PROVER_OSTD.create(rules);
		}
		
		@Override
		protected final void prepare() {
			this.role = machine.prover.pool.dobs.submerge(role);
			plan();
		}
		
		protected abstract void plan();
	}
	
	public static class Unresponsive extends ProverBased {
		@Override protected void plan() { }
		@Override protected void move() { }
		@Override protected void reflect() { }
	}
	
	public static class Legal extends ProverBased {
		@Override protected void plan() { makeAnyMove(); }
		@Override protected void move() { makeAnyMove(); }
		@Override protected void reflect() { }
		
		private void makeAnyMove() { setDecision(anyDecision()); }
	}
	
	private static Runnable getWrappedPlayer(final Player player) {
		return new Runnable() {
			@Override public void run() {
				try {
					player.run();
				} catch (Throwable e) {
					player.logger.severe(e.toString());
					throw e;
				}
			}
		};
	}
	
	public static Thread start(Player player) {
		Thread thread = new Thread(getWrappedPlayer(player));
		thread.start();
		return thread;
	}
}
