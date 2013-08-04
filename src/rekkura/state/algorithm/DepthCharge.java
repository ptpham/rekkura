package rekkura.state.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Random;

import rekkura.logic.model.Dob;
import rekkura.state.model.StateMachine;
import rekkura.util.Colut;
import rekkura.util.OtmUtil;
import rekkura.util.Synchron;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

public class DepthCharge<S,A> {
	
	public final StateMachine<S,A> machine;
	public Random rand = new Random();
	public int maxDepth = Integer.MAX_VALUE;
	public long maxTime = Long.MAX_VALUE;
	Map<Dob, A> fixed = null;
	
	private DepthCharge(StateMachine<S,A> machine) {
		this.machine = machine;
	}
	
	public static <S,A> DepthCharge<S,A> create(StateMachine<S,A> machine) {
		return new DepthCharge<S,A>(machine);
	}
	
	public List<S> fire(S state) {
		List<S> result = Lists.newArrayList();
		result.add(state);
		
		int depth = 0;
		long begin = System.currentTimeMillis();
		while (!machine.isTerminal(state)) {
			ListMultimap<Dob, A> actions = machine.getActions(state);
			Map<Dob, A> joint = OtmUtil.randomAssignment(actions, fixed, rand);
			fixed = null;
			
			state = machine.nextState(state, joint);
			result.add(state);
			
			if (++depth >= maxDepth) break;
			if (System.currentTimeMillis() - begin > maxTime) break;
		}
		
		return result;
	}

	public static <S, A> List<S> fire(S state, StateMachine<S, A> machine) {
		return fire(state, machine, null, new Random());
	}
	
	public static <S, A> List<S> fire(S state, StateMachine<S, A> machine, Random rand) {
		return fire(state, machine, null, rand);
	}
	
	public static <S, A> List<S> fire(S state, StateMachine<S, A> machine, Map<Dob, A> fixed) {
		return fire(state, machine, fixed, new Random());
	}
	
	/**
	 * 
	 * @param state
	 * @param machine
	 * @param fixed This forces a certain set of actions on the first advancement
	 * in the depth charge.
	 * @param rand
	 * @return
	 */
	public static <S, A> List<S> fire(S state, StateMachine<S, A> machine, 
			Map<Dob, A> fixed, Random rand) {
		DepthCharge<S, A> charger = new DepthCharge<S,A>(machine);
		if (rand != null) charger.rand = rand;
		charger.fixed = fixed;
		return charger.fire(state);
	}
	
	public static <S,A> double measureCps(StateMachine<S,A> machine, int charges) {
		S initial = machine.getInitial();
		long begin = System.currentTimeMillis();
		for (int i = 0; i < charges; i++) DepthCharge.fire(initial, machine);
		long interval = System.currentTimeMillis() - begin;
		return 1000*charges/(double)interval;
	}
	
	public static <S,A>double measureCpsThreaded(StateMachine<S,A>[] machines,
		final S initial, final int perMachine) {

		List<Thread> threads = Lists.newArrayList();
		for (int i = 0; i < machines.length; i++) {
			final StateMachine<S,A> machine = machines[i];
			threads.add(new Thread(new Runnable() {
				@Override public void run() {
					for (int i = 0; i < perMachine; i++) {
						DepthCharge.fire(initial, machine);
					}
				}
			}));
		}
		
		long begin = System.currentTimeMillis();
		for (Thread thread : threads) thread.start();
		while (threads.size() > 0) { Synchron.lightJoin(Colut.popAny(threads)); }
		long interval = System.currentTimeMillis() - begin;
		return 1000*machines.length*perMachine/(double)interval;
	}
	
	public static <S, A> void runBasicSuite(StateMachine<S,A> machine) {
		S initial = machine.getInitial();
		Map<Dob, A> joint = OtmUtil.randomAssignment(machine.getActions(initial));
		S next = machine.nextState(initial, joint);
		machine.isTerminal(next);
	}
}
