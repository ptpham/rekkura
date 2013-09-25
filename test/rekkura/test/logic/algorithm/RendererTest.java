package rekkura.test.logic.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import rekkura.logic.algorithm.Renderer;
import rekkura.logic.algorithm.Terra;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Pool;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public abstract class RendererTest {

	protected abstract Renderer getExpansion();
	
	@Test
	public void basic() {
		String rawRule = "{(X)|<((Q)(X)),true> :- <((P)(X)),true>}";
		List<String> rawInputs = Lists.newArrayList("((P)(a))", "((P)(b))");
		List<String> rawOutputs = Lists.newArrayList("((Q)(a))", "((Q)(b))");
		runTest(rawRule, rawInputs, rawOutputs);
	}

	@Test
	public void noVars() {
		String rawRule = "{|<(Q),true> :- <(P),true>}";
		List<String> rawInputs = Lists.newArrayList("(P)", "(P)");
		List<String> rawOutputs = Lists.newArrayList("(Q)");
		runTest(rawRule, rawInputs, rawOutputs);
	}
	
	@Test
	public void varFailure() {
		String rawRule = "{(X)|<(Q),true> :- <(P),true>}";
		List<String> rawInputs = Lists.newArrayList("(P)");
		List<String> rawOutputs = Lists.newArrayList();
		runTest(rawRule, rawInputs, rawOutputs);
	}
	
	@Test
	public void cartesian() {
		String rawRule = "{(X)(Y)|<((Q)(X)(Y)),true> :- <((P)(X)),true><((R)(Y)),true>}";
		List<String> rawInputs = Lists.newArrayList("((P)(a))", "((P)(b))", "((R)(a))", "((R)(b))");
		List<String> rawOutputs = Lists.newArrayList("((Q)(a)(a))","((Q)(a)(b))","((Q)(b)(a))","((Q)(b)(b))");
		runTest(rawRule, rawInputs, rawOutputs);
	}

	@Test
	public void negation() {
		String rawRule = "{(X)|<((Q)(X),true> :- <((P)(X)),true><((R)(X)),false>}";
		List<String> rawInputs = Lists.newArrayList("((P)(a))", "((P)(b))", "((R)(b))");
		List<String> rawOutputs = Lists.newArrayList("((Q)(a)");
		runTest(rawRule, rawInputs, rawOutputs);
	}
	
	@Test
	public void distinctVarBoth() {
		String rawRule = "{(X)(Y)|<((Q)(X)(Y)),true> :- <((P)(X)),true><((R)(Y)),true><(X)!=(Y)>}";
		List<String> rawInputs = Lists.newArrayList("((P)(a))", "((P)(b))", "((R)(a))", "((R)(b))");
		List<String> rawOutputs = Lists.newArrayList("((Q)(a)(b))","((Q)(b)(a))");
		runTest(rawRule, rawInputs, rawOutputs);
	}
	
	@Test
	public void distinctVarLeft() {
		String rawRule = "{(X)(Y)|<((Q)(X)(Y)),true> :- <((P)(X)),true><((R)(Y)),true><(X)!=(Y)><(Y)!=(a)>}";
		List<String> rawInputs = Lists.newArrayList("((P)(a))", "((P)(b))", "((R)(a))", "((R)(b))");
		List<String> rawOutputs = Lists.newArrayList("((Q)(a)(b))");
		runTest(rawRule, rawInputs, rawOutputs);
	}
	
	@Test
	public void distinctVarRight() {
		String rawRule = "{(X)(Y)|<((Q)(X)(Y)),true> :- <((P)(X)),true><((R)(Y)),true><(X)!=(Y)><(a)!=(X)>}";
		List<String> rawInputs = Lists.newArrayList("((P)(a))", "((P)(b))", "((R)(a))", "((R)(b))");
		List<String> rawOutputs = Lists.newArrayList("((Q)(b)(a))");
		runTest(rawRule, rawInputs, rawOutputs);
	}
	
	@Test
	public void distinctConstantSuccess() {
		String rawRule = "{(X)(Y)|<((Q)(X)(Y)),true> :- <((P)(X)),true><((R)(Y)),true><(a)!=(b)>}";
		List<String> rawInputs = Lists.newArrayList("((P)(a))", "((P)(b))", "((R)(a))", "((R)(b))");
		List<String> rawOutputs = Lists.newArrayList("((Q)(a)(b))","((Q)(b)(a))", "((Q)(a)(a))", "((Q)(b)(b))");
		runTest(rawRule, rawInputs, rawOutputs);
	}
	
	@Test
	public void distinctConstantFailure() {
		String rawRule = "{(X)(Y)|<((Q)(X)(Y)),true> :- <((P)(X)),true><((R)(Y)),true><(a)!=(a)>}";
		List<String> rawInputs = Lists.newArrayList("((P)(a))", "((P)(b))", "((R)(a))", "((R)(b))");
		List<String> rawOutputs = Lists.newArrayList();
		runTest(rawRule, rawInputs, rawOutputs);
	}
	
	@Test
	public void distinctGrounded() {
		String rawRule = "{(X)(Y)|<((Q)(X)(Y)),true> :- <((P)(X)),true><((R)(Y)),true><(X)!=(a)>}";
		List<String> rawInputs = Lists.newArrayList("((P)(a))", "((P)(b))", "((R)(a))", "((R)(b))");
		List<String> rawOutputs = Lists.newArrayList("((Q)(b)(b))","((Q)(b)(a))");
		runTest(rawRule, rawInputs, rawOutputs);
	}
	
	@Test
	public void joinSimple() {
		String rawRule = "{(X)(Y)|<((Q)(X)(Y)),true> :- <((P)(X)(Y)),true><((R)(Y)),true>}";
		List<String> rawInputs = Lists.newArrayList("((P)(a)(b))", "((P)(b)(b))", "((R)(a))", "((R)(b))");
		List<String> rawOutputs = Lists.newArrayList("((Q)(a)(b))","((Q)(b)(b))");
		runTest(rawRule, rawInputs, rawOutputs);
	}
	
	@Test
	public void joinComplex() {
		String rawRule = "{(X)(Y)(Z)|<((Q)(X)(Y)),true> :- <((P)(X)(Y)(Z)),true><((R)(X)),true><((S)(X)(Y)),true>}";
		List<String> rawInputs = Lists.newArrayList("((P)(a)(b)(c))", "((P)(b)(a)(c))", "((P)(c)(a)(b))",
			"((R)(a))", "((R)(b))", "((S)(a)(b))", "((S)(b)(c))", "((S)(c)(b))", "((S)(b)(a))");
		List<String> rawOutputs = Lists.newArrayList("((Q)(a)(b))", "((Q)(b)(a))");
		runTest(rawRule, rawInputs, rawOutputs);
	}
	
	@Test
	public void noGrounds() {
		String rawRule = "{(X)(Y)|<((Q)(X)(Y)),true> :- <((P)(X)),true><((R)(Y)),true>}";
		List<String> rawInputs = Lists.newArrayList("((P)(a))", "((P)(b))", "((Z)(a))", "((Z)(b))");
		List<String> rawOutputs = Lists.newArrayList();
		runTest(rawRule, rawInputs, rawOutputs);
	}
	
	@Test
	public void marginalize() {
		String rawRule = "{(X)(Y)|<((Q)(X)),true> :- <((P)(X)),true><((R)(Y)),true>}";
		List<String> rawInputs = Lists.newArrayList("((P)(a))", "((P)(b))", "((R)(a))", "((R)(b))");
		List<String> rawOutputs = Lists.newArrayList("((Q)(a))", "((Q)(b))");
		runTest(rawRule, rawInputs, rawOutputs);
	}
	
	private void runTest(String rawRule, List<String> rawInputs, List<String> rawOutputs) {
		Pool pool = new Pool();
		
		Rule rule = pool.rules.submergeString(rawRule);
		Set<Dob> truths = Sets.newHashSet(pool.dobs.submergeStrings(rawInputs));
		Multimap<Atom,Dob> support = Renderer.getNaiveSupport(rule, truths);
		
		Set<Dob> expected = Sets.newHashSet(pool.dobs.submergeStrings(rawOutputs));
		List<Map<Dob,Dob>> unifies = getExpansion().apply(rule, truths, support, pool);
		Set<Dob> generated = Terra.renderHeads(unifies, rule, pool);
		Assert.assertEquals(expected, generated);
	}
}
