package rekkura.test.logic.prover;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.google.common.collect.Lists;

import rekkura.fmt.LogicFormat;
import rekkura.fmt.StandardFormat;
import rekkura.logic.prover.StratifiedForward;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Colut;

public class StratifiedForwardTest {

	@Test
	public void noVariables() {
		String[] rawRules = { 
			"{| <(Q),true> :- <(P),true> }",
			"{| <(R),true> :- <(Q),true> }"
		};
		
		String[] rawDobs = {
			"(P)", "(Q)", "(R)"
		};
		
		syllogismTest(rawRules, rawDobs);
	}

	@Test
	public void noVariablesNegation() {
		String[] rawRules = { 
			"{| <(Q),true> :- <(P),false> }",
			"{| <(R),true> :- <(Q),true> }"
		};
		
		String[] rawDobs = {
			"(Z)", "(Q)", "(R)"
		};
		
		syllogismTest(rawRules, rawDobs);
	}


	@Test
	public void variablesRequired() {
		String[] rawRules = { 
			"{(X) | <((Q)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((R)(X)),true> :- <((Q)(X)),true> }"
		};
		
		String[] rawDobs = {
			"((P)(X))", "((Q)(X))", "((R)(X))"
		};
		
		LogicFormat fmt = new StandardFormat();
		List<Rule> rules = fmt.rulesFromStrings(Arrays.asList(rawRules));
		List<Dob> dobs = fmt.dobsFromStrings(Arrays.asList(rawDobs));
		
		StratifiedForward prover = new StratifiedForward(rules);
		prover.reset(Lists.newArrayList(dobs.get(0)));
		Assert.assertTrue(prover.hasMore());
		
		Set<Dob> proven = prover.proveNext();
		Assert.assertEquals(0, proven.size());
		Assert.assertFalse(prover.hasMore());
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
		LogicFormat fmt = new StandardFormat();
		List<Rule> rules = fmt.rulesFromStrings(Arrays.asList(rawRules));
		List<Dob> dobs = fmt.dobsFromStrings(Arrays.asList(rawDobs));

		StratifiedForward prover = new StratifiedForward(rules);
		prover.reset(Lists.newArrayList(dobs.get(0)));
		
		for (int i = 1; i < dobs.size(); i++) {
			Assert.assertTrue(prover.hasMore());
			Set<Dob> proven = prover.proveNext();
			Assert.assertEquals(1, proven.size());
			Assert.assertEquals(rawDobs[i], fmt.toString(Colut.any(proven)));
		}
		
		Assert.assertFalse(prover.hasMore());
	}
}
