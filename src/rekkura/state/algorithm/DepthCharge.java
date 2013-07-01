package rekkura.state.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import rekkura.ggp.machina.GgpStateMachine;
import rekkura.logic.model.Dob;
import rekkura.state.model.StateMachine;
import rekkura.util.Colut;
import rekkura.util.OtmUtil;
import rekkura.util.Synchron;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

public class DepthCharge {

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
		List<S> result = Lists.newArrayList();
		result.add(state);
		
		while (!machine.isTerminal(state)) {
			ListMultimap<Dob, A> actions = machine.getActions(state);
			Map<Dob, A> joint = OtmUtil.randomAssignment(actions, fixed, rand);
			fixed = null;
			
			state = machine.nextState(state, joint);
			result.add(state);
		}
		
		return result;
	}
	
	public static double measureCps(GgpStateMachine machine, int charges) {
		Set<Dob> initial = machine.getInitial();
		long begin = System.currentTimeMillis();
		for (int i = 0; i < charges; i++) DepthCharge.fire(initial, machine);
		long interval = System.currentTimeMillis() - begin;
		return 1000*charges/(double)interval;
	}
	
	public static double measureCpsThreaded(GgpStateMachine[] machines,
		final Set<Dob> initial, final int perMachine) {

		List<Thread> threads = Lists.newArrayList();
		for (int i = 0; i < machines.length; i++) {
			final GgpStateMachine machine = machines[i];
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
}
