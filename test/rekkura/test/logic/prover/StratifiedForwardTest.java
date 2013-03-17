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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class StratifiedForwardTest {

	@Test
	public void noVariables() {
		String[] rawRules = { 
			"{| <(Q),true> :- <(P),true> }",
			"{| <(R),true> :- <(Q),true> }"
		};
		
		String[][] rawDobs = { {"(P)"}, {"(Q)"}, {"(R)"} };
		syllogismTest(rawRules, rawDobs);
	}
	
	@Test
	public void vacuouslyTrue() {
		String[] rawRules = { 
				"{| <(Q),true> :-  }",
		};
		
		String[][] rawDobs = { {"(P)"}, {"(Q)"} };
		syllogismTest(rawRules, rawDobs);
	}

	@Test
	public void noVariablesNegation() {
		String[] rawRules = { 
			"{| <(Q),true> :- <(P),false> }",
			"{| <(R),true> :- <(Q),true> }"
		};
		
		String[][] rawDobs = { {"(Z)"}, {"(Q)"}, {"(R)"} };
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
		
		String[][] rawDobs = {
			{"((P)(a))"}, {"((Q)(a))"}, {"((R)(a))"}
		};
		
		syllogismTest(rawRules, rawDobs);
	}
	
	@Test
	public void pendingAssignments() {
		String[] rawRules = { 
			"{(X) | <((Q)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((M)(X)),true> :- <((Q)(X)),true> <((N)(X)),true> }",
			"{(X) | <((R)(X)),true> :- <((Q)(X)),true> <((M)(X)),false> }"
		};
		
		String[][] rawDobs = {
			{"((P)(a))"}, {"((Q)(a))"}, {}, {"((R)(a))"}
		};
		
		syllogismTest(rawRules, rawDobs);
	}
	
	@Test
	public void multipleVariables() {
		String[] rawRules = { 
			"{(X) | <((Q)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((M)(X)),true> :- <((Q)(X)),true> }",
			"{(X)(Y) | <((R)(X)(Y)),true> :- <((Q)(X)),true> <((M)(Y)),true> }"
		};
		
		String[][] rawDobs = {
			{"((P)(a))"}, {"((P)(a))", "((Q)(a))", "((M)(a))", "((R)(a)(a))"}
		};
		
		overallMatchTest(rawRules, rawDobs[0], rawDobs[1]);
	}
	
	private void syllogismTest(String[] rawRules, String[][] rawDobs) {
		LogicFormat fmt = new StandardFormat();
		List<List<Dob>> allProven = runProver(rawRules, rawDobs[0]);
		
		for (int i = 1; i < rawDobs.length; i++) {
			List<String> next = Lists.newArrayList(rawDobs[i]);
			
			Assert.assertTrue(allProven.size() > i);
			List<Dob> proven = allProven.get(i);

			Assert.assertEquals(next.size(), proven.size());
			Assert.assertTrue(next.containsAll(fmt.dobsToStrings(proven)));
		}
		
		Assert.assertEquals(rawDobs.length, allProven.size());
	}
	
	private void overallMatchTest(String[] rawRules, String[] initial, String[] expected) {
		LogicFormat fmt = new StandardFormat();
		List<List<Dob>> allProven = runProver(rawRules, initial);

		Set<String> provenSet = Sets.newHashSet();
		for (List<Dob> stage : allProven) {
			provenSet.addAll(fmt.dobsToStrings(stage));
		}
		
		Set<String> expectedSet = Sets.newHashSet(expected);
		
		Assert.assertEquals(expectedSet, provenSet);
	}
	
	private List<List<Dob>> runProver(String[] rawRules, String[] rawInitial) {
		LogicFormat fmt = new StandardFormat();
		List<Rule> rules = fmt.rulesFromStrings(Arrays.asList(rawRules));
		List<Dob> initial = fmt.dobsFromStrings(Arrays.asList(rawInitial));
		StratifiedForward prover = new StratifiedForward(rules);

		List<List<Dob>> result = Lists.newArrayList();
		result.add(initial);
		
		prover.reset(initial);
		while (prover.hasMore()) { result.add(prover.proveNext()); }
		return result;
	}
	
	protected void syllogismPrint(String[] rawRules, String[][] rawDobs) {
		LogicFormat fmt = new StandardFormat();
		List<List<Dob>> allProven = runProver(rawRules, rawDobs[0]);
		
		for (int i = 1; i < rawDobs.length; i++) {
			if (allProven.size() >= i) {
				System.out.println("Prover should have more to prove!");
			} else {
				List<Dob> proven = allProven.get(i);
				System.out.println("Seen: " + proven.size() + ", Expected: " + rawDobs[i].length);
				System.out.println("Proven: ");
				for (Dob dob : proven) {
					System.out.println(fmt.toString(dob));
				}
				System.out.println("Expected: ");
				for (int j = 0; j < rawDobs[i].length; j++) {
					System.out.println(rawDobs[i][j]);
				}
			}
			System.out.println();
		}
	}
}
