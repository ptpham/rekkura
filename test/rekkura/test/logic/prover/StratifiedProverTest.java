package rekkura.test.logic.prover;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import rekkura.logic.format.LogicFormat;
import rekkura.logic.format.StandardFormat;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.prover.StratifiedProver;

import com.google.common.collect.Sets;

public abstract class StratifiedProverTest {

	protected abstract StratifiedProver.Factory getFactory();
	
	@Test
	public void noVariables() {
		String[] rules = { 
			"{| <(Q),true> :- <(P),true> }",
			"{| <(R),true> :- <(Q),true> }"
		};
		
		String[] initial = {"(P)"};
		String[] expected = {"(Q)", "(R)"};
		overallMatchTest(rules, initial, expected);
	}
	
	@Test
	public void vacuouslyTrue() {
		String[] rules = { "{| <(Q),true> :-  }", };
		
		String[] initial = {"(P)"};
		String[] expected = {"(Q)"};
		overallMatchTest(rules, initial, expected);
	}
	
	@Test
	public void multipleVacuous() {
		String[] rules =  {
			"{|<((role)(robot)),true>:-}",
			"{|<((init)(p)),true>:-}",
			"{|<((legal)(p)(sing)),true>:-}",
			"{|<((legal)(p)(noop)),true>:-}",
		};
		
		String[] initial = {};
		String[] expected = {"((role)(robot))", "((init)(p))", 
				 "((legal)(p)(sing))", "((legal)(p)(noop))"};
		overallMatchTest(rules, initial, expected);
	}

	@Test
	public void noVariablesNegation() {
		String[] rules =  {
			"{| <(Q),true> :- <(P),false> }",
			"{| <(R),true> :- <(Q),true> }"
		};
			
		String[] initial = { "(Z)" };
		String[] expected = { "(Q)", "(R)" };
		overallMatchTest(rules, initial, expected);
	}


	@Test
	public void variablesRequired() {
		String[] rules =  {
			"{(X) | <((Q)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((R)(X)),true> :- <((Q)(X)),true> }"
		};
			
		String[] initial = { "((P)(X))" };
		String[] expected = { };
		overallMatchTest(rules, initial, expected);
	}
	
	@Test
	public void variablesGrounded() {
		String[] rules =  {
			"{(X) | <((Q)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((R)(X)),true> :- <((Q)(X)),true> }"
		};
			
		String[] initial = { "((P)(a))" };
		String[] expected = { "((Q)(a))", "((R)(a))" };
		overallMatchTest(rules, initial, expected);
	}
	
	@Test
	public void pendingAssignments() {
		String[] rules =  {
			"{(X) | <((Q)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((M)(X)),true> :- <((Q)(X)),true> <((N)(X)),true> }",
			"{(X) | <((R)(X)),true> :- <((Q)(X)),true> <((M)(X)),false> }"
		};
			
		String[] initial = { "((P)(a))" };
		String[] expected = { "((Q)(a))", "((R)(a))" };
		overallMatchTest(rules, initial, expected);
	}
	
	@Test
	public void distinct() {
		String[] rules =  {
			"{(X)(Y) | <((Q)(X)(Y)),true> :- <((P)(X)(Y)),true> <(X)!=(Y)>}",
			"{(X) | <((Q)(X)),true> :- <((P)(X)),true> <(X)!=(a)>}"
		};
			
		String[] initial = { "((P)(a)(b))", "((P)(b))" };
		String[] expected = { "((Q)(a)(b))", "((Q)(b))" };
		overallMatchTest(rules, initial, expected);
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
			"{(X) | <(M),true> :- <((Q)(X)),true> }",
			"{    | <(K),true> :- <(M),false> }",
			"{(X) | <((R)(X)),true> :- <((N)(X)),true> <(K),false> }"
		};

		String[][] rawDobs = {
			{"((P)(a))", "((Q)(b))"}, {"(M)", "((N)(a))", "((R)(a))"}
		};

		overallMatchTest(rawRules, rawDobs[0], rawDobs[1]);
	}

	@Test
	public void alternatingSpecificity() {
		String[] rawRules = {
			"{(X)(Y) | <((N)(a)(Y)),true> :- <((P)(X)(Y)),true> }",
			"{(X) | <((Q)(X)),true> :- <((N)(X)(1)),true> }",
		};
		
		String[][] rawDobs = {
			{ "((P)(a)(1))" }, { "((N)(a)(1))", "((Q)(a))" }
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
	public void complexDoubleNegation() {
		String[] rawRules = { 
			"{(X) | <((N)(X)),true> :- <((P)(X)),true> }",
			"{(Z) | <((N)(Z)),true> :- <((T)(Z)),true> }",
			"{(X) | <((M)(X)),true> :- <((Q)(X)),true> }",
			"{(X) | <((K)(X)),true> :- <((M)(X)),true> }",
			"{(X) | <((N)(X)),true> :- }",
			"{    | <(N),true> :- <(D),false>}",
			"{(X)(Y) | <((R)(X)(Y)),true> :- <((N)(X)),true> <((K)(Y)),true> <(N),false> }"
		};
		
		String[][] rawDobs = {
			{"((P)(a))", "((Q)(b))", "((T)(c))"}, 
			{"((N)(c))", "((M)(b))", "((K)(b))", 
			 "((N)(a))", "(N)" }
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
			"{(X)(Y) | <((R)(X)(Y)),true> :- <((N)(X)),true> <((K)(Y)),true> <(N),false> }"
		};
		
		String[][] rawDobs = {
			{"((P)(a))", "((Q)(b))", "((T)(c))"}, 
			{"((N)(c))", "((M)(b))", "((K)(b))", 
			 "((N)(a))", "((R)(a)(b))", "((R)(c)(b))"}
		};

		overallMatchTest(rawRules, rawDobs[0], rawDobs[1]);
	}
	
	@Test
	public void recursionOneRule() {
		String[] rawRules = { 
			"{(X)(Y) | <((P)(Y)),true> :- <((Q)(X)(Y)),true> <((P)(X)),true> }",
		};
		
		String[][] rawDobs = {
			{"((Q)(1)(2))", "((Q)(2)(3))", "((P)(1))"}, 
			{"((P)(2))", "((P)(3))" }
		};

		overallMatchTest(rawRules, rawDobs[0], rawDobs[1]);
	}
	
	@Test
	public void recursionTwoRules() {
		String[] rawRules = { 
			"{(X)(Y) | <((R)(Y)),true> :- <((Q)(X)(Y)),true> <((P)(X)),true> }",
			"{(X) | <((P)(X)),true> :- <((R)(X)),true> }",
		};
		
		String[][] rawDobs = {
			{"((Q)(1)(2))", "((Q)(2)(3))", "((P)(1))"}, 
			{"((P)(2))", "((P)(3))", "((R)(2))", "((R)(3))" }
		};

		overallMatchTest(rawRules, rawDobs[0], rawDobs[1]);
	}
	
	private void overallMatchTest(String[] rules, String[] initial, String[] expected) {
		Set<Dob> allProven = runProver(rules, initial);
		
		LogicFormat fmt = new StandardFormat();
		Set<String> provenSet = Sets.newHashSet(fmt.dobsToStrings(allProven));
		
		Set<String> expectedSet = Sets.newHashSet(expected);
		expectedSet.addAll(Arrays.asList(initial));
		
		assertEquals(expectedSet, provenSet);
	}
	
	public Set<Dob> runProver(String[] rawRules, String[] rawInitial) {
		LogicFormat fmt = new StandardFormat();
		List<Rule> rules = fmt.rulesFromStrings(Arrays.asList(rawRules));
		List<Dob> initial = fmt.dobsFromStrings(Arrays.asList(rawInitial));
		StratifiedProver prover = getFactory().create(rules);

		Set<Dob> result = Sets.newHashSet();
		result.addAll(initial);
		result.addAll(prover.proveAll(initial));
		
		return result;
	}
}
