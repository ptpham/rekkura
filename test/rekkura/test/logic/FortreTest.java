package rekkura.test.logic;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.fmt.LogicFormat;
import rekkura.fmt.StandardFormat;
import rekkura.logic.Fortre;
import rekkura.logic.Pool;
import rekkura.model.Dob;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class FortreTest {
	
	@Test
	public void compression() {
		String[] rawDobs = {
			"(X)", "(Y)", "((P)(X)(a))", "((P)(X)(b))"
		};

		LogicFormat fmt = StandardFormat.inst;
		Pool pool = new Pool();
		List<Dob> dobs = stringsToDobs(rawDobs, fmt, pool);
		Set<Dob> vars = Sets.newHashSet(dobs.get(0), dobs.get(1));
		
		Dob first = dobs.get(3);
		Dob second = dobs.get(2);
		
		Fortre fortre = new Fortre(vars, Lists.newArrayList(first, second), pool);
		
		Assert.assertEquals(2, fortre.getTrunk(first).size());
		Assert.assertEquals(2, fortre.getTrunk(second).size());
	}
	
	/**
	 * This is a test to see if a more general dob added
	 * after a more specific dob gets added in the proper 
	 * location.
	 */
	@Test
	public void specificityOrder() {
		String[] rawDobs = {
			"(X)", "(Y)", "((P)(X)(a))", "((P)(X)(Y))"
		};

		LogicFormat fmt = new StandardFormat();
		Pool pool = new Pool();
		List<Dob> dobs = stringsToDobs(rawDobs, fmt, pool);
		Set<Dob> vars = Sets.newHashSet(dobs.get(0), dobs.get(1));
		
		Dob general = dobs.get(3);
		Dob specific = dobs.get(2);
		
		Fortre fortre = new Fortre(vars, Lists.newArrayList(specific, general), pool);
		
		Assert.assertEquals(3, fortre.getTrunk(specific).size());
		Assert.assertEquals(2, fortre.getTrunk(general).size());
	}
	
	@Test
	public void simpleSymmetry() {
		String[] rawDobs = {
			"(X)", "(Y)", "((P)(X)(a))", "((P)(b)(Y))", "((P)(X)(Y))"
		};

		LogicFormat fmt = new StandardFormat();
		Pool pool = new Pool();
		List<Dob> dobs = stringsToDobs(rawDobs, fmt, pool);
		Set<Dob> vars = Sets.newHashSet(dobs.get(0), dobs.get(1));
		
		Dob first = dobs.get(3);
		Dob second = dobs.get(2);
		
		Fortre fortre = new Fortre(vars, Lists.newArrayList(first, second), pool);
		
		Assert.assertEquals(3, fortre.getTrunk(first).size());
		Assert.assertEquals(3, fortre.getTrunk(second).size());
		Assert.assertEquals(2, fortre.getTrunk(dobs.get(4)).size());
	}
	
	@Test
	public void joinSymmetry() {
		String[] variableDobs = { "(X)", "(Y)", "(Z)" };
		String[] bodyDobs =  { "((P)(X)(b)(c))", "((P)(a)(Y)(c))", "((P)(a)(e)(Z))" };
		String[] resultDobs = { "((P)(X)(Y)(Z))" };

		
		LogicFormat fmt = new StandardFormat();
		Pool pool = new Pool();
		List<Dob> varList = stringsToDobs(variableDobs, fmt, pool);
		List<Dob> bodyList = stringsToDobs(bodyDobs, fmt, pool);
		List<Dob> result = stringsToDobs(resultDobs, fmt, pool);
		
		Fortre fortre = new Fortre(varList, bodyList, pool);
		
		for (Dob dob : bodyList) Assert.assertEquals(3, fortre.getTrunk(dob).size());
		Assert.assertEquals(2, fortre.getTrunk(result.get(0)).size());
	}
	
	@Test
	public void partialSymmetry() {
		String[] variableDobs = { "(X)", "(Y)", "(Z)" };
		String[] bodyDobs =  { "((P)(1)(1)(Z))", "((P)(2)(2)(Z))", "((P)(X)(Y)(m))" };
		String[] resultDobs = { "((P)(X)(Y)(Z))" };

		
		LogicFormat fmt = new StandardFormat();
		Pool pool = new Pool();
		List<Dob> varList = stringsToDobs(variableDobs, fmt, pool);
		List<Dob> bodyList = stringsToDobs(bodyDobs, fmt, pool);
		List<Dob> result = stringsToDobs(resultDobs, fmt, pool);
		
		Fortre fortre = new Fortre(varList, bodyList, pool);
		
		for (Dob dob : bodyList) Assert.assertEquals(3, fortre.getTrunk(dob).size());
		Assert.assertEquals(2, fortre.getTrunk(result.get(0)).size());
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
	
	@Test
	public void cognates() {
		String[] rawDobs = {
				"(X)", "(Y)", "((P)(X))", "((P)(Y))"
			};

			LogicFormat fmt = new StandardFormat();
			Pool pool = new Pool();
			List<Dob> dobs = stringsToDobs(rawDobs, fmt, pool);
			Set<Dob> vars = Sets.newHashSet(dobs.get(0), dobs.get(1));
			
			Dob first = dobs.get(3);
			Dob second = dobs.get(2);
			
			Fortre fortre = new Fortre(vars, Lists.newArrayList(first, second), pool);
			
			Assert.assertEquals(2, fortre.getTrunk(first).size());
			Assert.assertEquals(2, fortre.getTrunk(second).size());
	}
	
	// TODO: Write tests for cognates 
	// - make sure that only children that have children are compressed
	// - make sure that all cognates are stored
	
	private List<Dob> stringsToDobs(String[] rawDobs, LogicFormat fmt, Pool pool) {
		return pool.submergeDobs(fmt.dobsFromStrings(Arrays.asList(rawDobs)));
	}
}
