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
import rekkura.util.Colut;

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
	
	@Test
	public void multipleGroundings() {
		String[] rawRules = { 
			"{(X) | <((Q)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((M)(X)),true> :- <((Q)(X)),true> }",
			"{(X)(Y) | <((R)(X)(Y)),true> :- <((Q)(X)),true> <((M)(Y)),true> }"
		};
		
		String[][] rawDobs = {
			{"((P)(a))", "((P)(b))"}, 
			{"((Q)(a))", "((Q)(b))",  "((M)(a))", "((M)(b))", 
			 "((R)(a)(a))", "((R)(a)(b))", "((R)(b)(a))", "((R)(b)(b))" }
		};
		
		overallMatchTest(rawRules, rawDobs[0], rawDobs[1]);
	}
	
	@Test
	public void deepFormTree() {
		String[] rawRules = { 
			"{(X) | <((Q)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((R)(X)),true> :- <((Q)(X)),true> <((P)(a)),false> }"
		};
		
		String[][] rawDobs = {
			{"((P)(a))", "((P)(b))"}, {"((Q)(b))", "((Q)(a))"}
		};
		
		overallMatchTest(rawRules, rawDobs[0], rawDobs[1]);
	}
	
	@Test
	public void interleavedNegation() {
		String[] rawRules = { 
			"{(X) | <((N)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((M)(X)),true> :- <((Q)(X)),true> }",
			"{(X) | <((R)(X)),true> :- <((N)(X)),true> <((M)(X)),false> }"
		};
		
		String[][] rawDobs = {
			{"((P)(a))", "((Q)(b))"}, {"((M)(b))", "((N)(a))", "((R)(a))"}
		};
		
		overallMatchTest(rawRules, rawDobs[0], rawDobs[1]);
	}
	
	@Test
	public void formCognates() {
		String[] rawRules = { 
			"{(X) | <((N)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((M)(X)),true> :- <((Q)(X)),true> }",
			"{(X) | <((K)(X)),true> :- <((M)(X)),true> }",
			"{(Y) | <((R)(Y)),true> :- <((N)(Y)),true> <((M)(Y)),false> }"
		};
		
		String[][] rawDobs = {
			{"((P)(a))", "((Q)(b))"}, {"((M)(b))", "((K)(b))", "((N)(a))", "((R)(a))"}
		};
		
		overallMatchTest(rawRules, rawDobs[0], rawDobs[1]);
	}
	
	@Test
	public void doubleNegation() {
		String[] rawRules = { 
			"{(X) | <((N)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((Q)(X)),true> :- <((J)(X)),true> }",
			"{(X) | <((M)(X)),true> :- <((Q)(X)),true> }",
			"{(X) | <((K)(X)),true> :- <((M)(X)),false> }",
			"{(X) | <((R)(X)),true> :- <((N)(X)),true> <((K)(Y)),false> }"
		};
		
		String[][] rawDobs = {
			{"((P)(a))", "((Q)(b))"}, {"((M)(b))", "((N)(a))", "((R)(a))"}
		};

		overallMatchTest(rawRules, rawDobs[0], rawDobs[1]);
	}
	
	@Test
	public void cartesianProduct() {
		String[] rawRules = { 
				"{| <((index)(1)),true> :- }",
				"{| <((index)(2)),true> :- }",
				"{| <((index)(3)),true> :- }",
				"{(X)(Y) | <((cell)(X)(Y)(empty)),true> :- <((index)(X)),true> <((index)(Y)),true> }",
			};
		String[][] rawDobs = {
				{}, 
				{"((index)(1))", "((index)(2))", "((index)(3))", 
				 "((cell)(1)(1)(empty))", "((cell)(1)(2)(empty))", "((cell)(1)(3)(empty))",
				 "((cell)(2)(1)(empty))", "((cell)(2)(2)(empty))", "((cell)(2)(3)(empty))",
				 "((cell)(3)(1)(empty))", "((cell)(3)(2)(empty))", "((cell)(3)(3)(empty))"}
			};
		overallMatchTest(rawRules, rawDobs[0], rawDobs[1]);
	}
	
	@Test
	public void complex() {
		String[] rawRules = { 
			"{(X) | <((N)(X)),true> :- <((P)(X)),true> }",
			"{(Z) | <((N)(Z)),true> :- <((T)(Z)),true> }",
			"{(X) | <((M)(X)),true> :- <((Q)(X)),true> }",
			"{(X) | <((K)(X)),true> :- <((M)(X)),true> }",
			"{(X) | <((N)(X)),true> :- }",
			"{(Z) | <((N)(Z)),true> :- <((D)((M)(Z))),false>}",
			"{(X)(Y) | <((R)(X)(Y)),true> :- <((N)(X)),true> <((K)(Y)),true> <((N)(Y)),false> }"
		};
		
		String[][] rawDobs = {
			{"((P)(a))", "((Q)(b))", "((T)(c))"}, 
			{"((N)(c))", "((M)(b))", "((K)(b))", 
			 "((N)(a))", "((R)(a)(b))", "((R)(c)(b))"}
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
		List<List<Dob>> allProven = runProver(rawRules, initial);
		
		LogicFormat fmt = new StandardFormat();
		List<Dob> provenList = Colut.collapseAsList(allProven);
		Set<String> provenSet = Sets.newHashSet(fmt.dobsToStrings(provenList));
		
		Set<String> expectedSet = Sets.newHashSet(expected);
		expectedSet.addAll(Arrays.asList(initial));
		
		Assert.assertEquals(expectedSet, provenSet);
	}
	
	public static List<List<Dob>> runProver(String[] rawRules, String[] rawInitial) {
		LogicFormat fmt = new StandardFormat();
		List<Rule> rules = fmt.rulesFromStrings(Arrays.asList(rawRules));
		List<Dob> initial = fmt.dobsFromStrings(Arrays.asList(rawInitial));
		StratifiedForward prover = new StratifiedForward(rules);

		List<List<Dob>> result = Lists.newArrayListWithCapacity(100);
		result.add(initial);
		
		prover.reset(initial);
		while (prover.hasMore()) { result.add(prover.proveNext()); }
		
		return result;
	}
	
	protected void syllogismPrint(String[] rawRules, String[][] rawDobs) {
		LogicFormat fmt = new StandardFormat();
		List<List<Dob>> allProven = runProver(rawRules, rawDobs[0]);
		
		for (int i = 1; i < rawDobs.length; i++) {
			List<Dob> proven = Lists.newArrayList();
			if (i < allProven.size()) proven = allProven.get(i);
			System.out.println("Seen: " + proven.size() + ", Expected: " + rawDobs[i].length);
			System.out.println("Proven: ");
			for (Dob dob : proven) {
				System.out.println(fmt.toString(dob));
			}
			System.out.println("Expected: ");
			for (int j = 0; j < rawDobs[i].length; j++) {
				System.out.println(rawDobs[i][j]);
			}
			System.out.println();
		}
	}
}
