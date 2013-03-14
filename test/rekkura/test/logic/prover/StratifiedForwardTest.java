package rekkura.test.logic.prover;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.fmt.LogicFormat;
import rekkura.fmt.StandardFormat;
import rekkura.logic.prover.StratifiedForward;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Sets;

public class StratifiedForwardTest {

	@Test
	public void noVariables() {
		String[] rawRules = { 
			"{| <(Q),true> :- <(P),true> }",
			"{| <(R),true> :- <(Q),true> }"
		};
		
		String[] rawDobs = { "(P)", "(Q)", "(R)" };
		syllogismTest(rawRules, rawDobs);
	}
	
	@Test
	public void vacuouslyTrue() {
		String[] rawRules = { 
				"{| <(Q),true> :-  }",
		};
		
		String[] rawDobs = { "(P)", "(Q)" };
		syllogismTest(rawRules, rawDobs);
	}

	@Test
	public void noVariablesNegation() {
		String[] rawRules = { 
			"{| <(Q),true> :- <(P),false> }",
			"{| <(R),true> :- <(Q),true> }"
		};
		
		String[] rawDobs = { "(Z)", "(Q)", "(R)" };
		syllogismTest(rawRules, rawDobs);
	}


	@Test
	public void variablesRequired() {
		String[] rawRules = { 
			"{(X) | <((Q)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((R)(X)),true> :- <((Q)(X)),true> }"
		};
		
		String[][] rawDobs = { {"((P)(X))"}, {} };
		syllogismTest(rawRules, rawDobs);
	}
	
	@Test
	public void variablesGrounded() {
		String[] rawRules = { 
			"{(X) | <((Q)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((R)(X)),true> :- <((Q)(X)),true> }"
		};
		
		String[] rawDobs = {
			"((P)(a))", "((Q)(a))", "((R)(a))"
		};
		
		syllogismTest(rawRules, rawDobs);
	}
	
	/**
	 * Tests for a set of rules and a set of dobs such that you get
	 * exactly one dob after another in the sequence.
	 * @param rawRules
	 * @param rawDobs
	 */
	private void syllogismTest(String[] rawRules, String[] rawDobs) {
		String[][] dobSets = new String[rawDobs.length][1];
		for (int i = 0; i < rawDobs.length; i++) { dobSets[i][0] = rawDobs[i]; }
		syllogismTest(rawRules, dobSets);
	}
	
	private void syllogismTest(String[] rawRules, String[][] rawDobs) {
		LogicFormat fmt = new StandardFormat();
		List<Rule> rules = fmt.rulesFromStrings(Arrays.asList(rawRules));
		List<Dob> initial = fmt.dobsFromStrings(Arrays.asList(rawDobs[0]));

		StratifiedForward prover = new StratifiedForward(rules);
		prover.reset(initial);
		
		for (int i = 1; i < rawDobs.length; i++) {
			Set<String> next = Sets.newHashSet(rawDobs[i]);
			
			Assert.assertTrue(prover.hasMore());
			
			Set<Dob> proven = prover.proveNext();
			Assert.assertEquals(next.size(), proven.size());
			Assert.assertTrue(next.containsAll(fmt.dobsToStrings(proven)));
		}
		
		Assert.assertFalse(prover.hasMore());
	}
}
