package rekkura.test.logic.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import rekkura.logic.format.LogicFormat;
import rekkura.logic.format.StandardFormat;
import rekkura.logic.model.Dob;
import rekkura.logic.structure.Fortre;
import rekkura.logic.structure.Pool;
import rekkura.util.Colut;

import com.google.common.collect.*;

public class FortreTest {
	
	@Test
	public void conflicting() {
		String[] rawVars = { "(X)", "(Y)" };
		String[] rawDobs = { "((P)(X)(a))", "((P)(X)(b))" };
		String[] rawGenerated = { };

		Map<String, Integer> expected = ImmutableMap.of(rawDobs[0], 2, rawDobs[1], 2);
		Map<String, List<Dob>> trunks = makeAndCheckTrunks(rawVars, rawDobs, rawGenerated);
		checkTrunkLengths(expected, trunks);
	}
	
	/**
	 * This is a test to see if a more general dob added
	 * after a more specific dob gets appropriately removed.
	 */
	@Test
	public void specificityOrder() {
		String[] rawVars = { "(X)", "(Y)" };
		String[] rawDobs = { "((P)(X)(a))", "((P)(X)(Y))" };
		String[] rawGenerated = { };
		
		Map<String, Integer> expected = ImmutableMap.of(rawDobs[0], 3, rawDobs[1], 2);
		Map<String, List<Dob>> trunks = makeAndCheckTrunks(rawVars, rawDobs, rawGenerated);
		checkTrunkLengths(expected, trunks);
	}
	
	@Test
	public void cognates() {
		String[] rawVars = { "(X)", "(Y)" };
		String[] rawDobs = { "((P)(X))", "((P)(Y))" };
		String[] rawGenerated = { };

		Map<String, Integer> expected = ImmutableMap.of(rawDobs[0], 2, rawDobs[1], 2);
		Map<String, List<Dob>> trunks = makeAndCheckTrunks(rawVars, rawDobs, rawGenerated);
		checkTrunkLengths(expected, trunks);
		
		assertEquals(trunks.get(rawDobs[0]), trunks.get(rawDobs[1]));
	}

	public void simpleSymmetry() {
		String[] rawVars = { "(X)", "(Y)" };
		String[] rawDobs = { "((P)(X)(a))", "((P)(b)(Y))" };
		String[] rawGenerated = { "((P)(X)(Y))" };
		
		Map<String, Integer> expected = ImmutableMap.of(rawDobs[0], 3, rawDobs[1], 3, rawGenerated[0], 2);
		Map<String, List<Dob>> trunks = makeAndCheckTrunks(rawVars, rawDobs, rawGenerated);
		checkTrunkLengths(expected, trunks);
	}
	
	@Test
	public void joinSymmetry() {
		String[] rawVars = { "(X)", "(Y)", "(Z)" };
		String[] rawDobs =  { "((P)(X)(b)(c))", "((P)(a)(Y)(c))", "((P)(a)(e)(Z))" };
		String[] rawGenerated = { "((P)(X)(Y)(Z))" };

		Map<String, Integer> expected = ImmutableMap.of(rawDobs[0], 4, rawDobs[1], 2, rawDobs[2], 4, rawGenerated[0], 2);
		Map<String, List<Dob>> trunks = makeAndCheckTrunks(rawVars, rawDobs, rawGenerated);
		checkTrunkLengths(expected, trunks);
	}
	
	@Test
	public void partialSymmetry() {
		String[] rawVars = { "(X)", "(Y)", "(Z)" };
		String[] rawDobs =  { "((P)(1)(1)(Z))", "((P)(2)(2)(Z))", "((P)(X)(Y)(m))" };
		String[] rawGenerated = { "((P)(X)(Y)(Z))" };

		Map<String, Integer> expected = ImmutableMap.of(rawDobs[0], 3, rawDobs[1], 3, rawDobs[2], 3, rawGenerated[0], 2);
		Map<String, List<Dob>> trunks = makeAndCheckTrunks(rawVars, rawDobs, rawGenerated);
		checkTrunkLengths(expected, trunks);
	}
	
	@Test
	public void variableNotInUnification() {
		List<String> raw = Lists.newArrayList("((true)((cell)(S)(X)(Y)))",
				"((true)((cell)(R)(X)(1)))", "((true)((cell)(X)(X)(Y)))", 
				"(X)", "(Y)", "(S)");
		
		Pool pool = new Pool();
		List<Dob> dobs = pool.dobs.submergeStrings(raw);
		pool.allVars.addAll(Colut.slice(dobs, 3, dobs.size()));
		Dob var = dobs.get(3);
		
		Multimap<Dob, Dob> edges = HashMultimap.create();
		for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) edges.put(dobs.get(i), dobs.get(j));
		Set<Dob> generalization = Fortre.computeGeneralization(dobs, var, pool);
		Assert.assertTrue(generalization.size() > 0);
	}
	
	@Test
	public void multipleDownward() {
		String[] bodyDobs =  { "((P)(1)(X)(Y))", "((P)(X)(1)(Y))" };
		String[] queryDobs = { "((P)(1)(1)(Z))" };
		
		LogicFormat fmt = new StandardFormat();
		Pool pool = new Pool();
		List<Dob> bodyList = stringsToDobs(bodyDobs, fmt, pool);
		List<Dob> queryList = stringsToDobs(queryDobs, fmt, pool);
		
		assertNull(Fortre.downwardUnify(queryList.get(0), bodyList, pool));
	}
	
	private Map<String, List<Dob>> makeAndCheckTrunks(String[] rawVars,
		String[] rawDobs, String[] rawGenerated) {
		
		LogicFormat fmt = new StandardFormat();
		Pool pool = new Pool();
		List<Dob> dobs = stringsToDobs(rawDobs, fmt, pool);
		List<Dob> vars = stringsToDobs(rawVars, fmt, pool);
		List<Dob> generated = stringsToDobs(rawGenerated, fmt, pool);
		pool.allVars.addAll(vars);
		
		Fortre fortre = new Fortre(dobs, pool);

		Map<String, List<Dob>> result = Maps.newHashMap();
		for (Dob dob : Iterables.concat(dobs, generated)) {
			List<Dob> trunk = fortre.getTrunk(dob);
			result.put(fmt.toString(dob), trunk);
		}
		
		return result;
	}

	private void checkTrunkLengths(Map<String, Integer> expected, Map<String, List<Dob>> actual) {
		assertEquals(expected.size(), actual.size());
		for (String key : expected.keySet()) {
			assertEquals(expected.get(key).intValue(), actual.get(key).size());
		}
	}
	
	private List<Dob> stringsToDobs(String[] rawDobs, LogicFormat fmt, Pool pool) {
		return pool.dobs.submerge(fmt.dobsFromStrings(Arrays.asList(rawDobs)));
	}
}
