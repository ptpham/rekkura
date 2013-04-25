package rekkura.test.logic;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.fmt.LogicFormat;
import rekkura.fmt.StandardFormat;
import rekkura.logic.Fortre;
import rekkura.logic.Pool;
import rekkura.model.Dob;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
	 * after a more specific dob gets added in the proper 
	 * location.
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
		
		Assert.assertEquals(trunks.get(rawDobs[0]), trunks.get(rawDobs[1]));
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

		Map<String, Integer> expected = ImmutableMap.of(rawDobs[0], 3, rawDobs[1], 3, rawDobs[2], 3, rawGenerated[0], 2);
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
	public void multipleDownward() {
		String[] variableDobs = { "(X)", "(Y)", "(Z)" };
		String[] bodyDobs =  { "((P)(1)(X)(Y))", "((P)(X)(1)(Y))" };
		String[] queryDobs = { "((P)(1)(1)(Z))" };
		
		LogicFormat fmt = new StandardFormat();
		Pool pool = new Pool();
		List<Dob> varList = stringsToDobs(variableDobs, fmt, pool);
		List<Dob> bodyList = stringsToDobs(bodyDobs, fmt, pool);
		List<Dob> queryList = stringsToDobs(queryDobs, fmt, pool);
		
		Assert.assertNull(Fortre.downwardUnify(queryList.get(0), bodyList, Sets.newHashSet(varList)));
	}
	
	private Map<String, List<Dob>> makeAndCheckTrunks(String[] rawVars, String[] rawDobs, String[] rawGenerated) {
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
		Assert.assertEquals(expected.size(), actual.size());
		for (String key : expected.keySet()) {
			Assert.assertEquals(expected.get(key).intValue(), actual.get(key).size());
		}
	}
	
	private List<Dob> stringsToDobs(String[] rawDobs, LogicFormat fmt, Pool pool) {
		return pool.dobs.submerge(fmt.dobsFromStrings(Arrays.asList(rawDobs)));
	}
}
