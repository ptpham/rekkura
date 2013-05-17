package rekkura.state.algorithms;

import java.util.List;
import java.util.Map;
import java.util.Random;

import rekkura.logic.model.Dob;
import rekkura.state.model.StateMachine;
import rekkura.util.OtmUtil;

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
	
}
