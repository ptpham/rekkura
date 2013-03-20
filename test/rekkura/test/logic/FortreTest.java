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
		List<Dob> dobs = pool.submergeDobs(fmt.dobsFromStrings(Arrays.asList(rawDobs)));
		Set<Dob> vars = Sets.newHashSet(dobs.get(0), dobs.get(1));
		
		Dob general = dobs.get(3);
		Dob specific = dobs.get(2);
		
		Fortre fortre = new Fortre(vars, Lists.newArrayList(specific, general), pool);
		
		Assert.assertEquals(3, fortre.getTrunk(specific).size());
		Assert.assertEquals(2, fortre.getTrunk(general).size());
	}
	
	@Test
	public void compressionTest() {
		String[] rawDobs = {
			"(X)", "(Y)", "((P)(X)(a))", "((P)(X)(b))"
		};

		LogicFormat fmt = new StandardFormat();
		Pool pool = new Pool();
		List<Dob> dobs = pool.submergeDobs(fmt.dobsFromStrings(Arrays.asList(rawDobs)));
		Set<Dob> vars = Sets.newHashSet(dobs.get(0), dobs.get(1));
		
		Dob first = dobs.get(3);
		Dob second = dobs.get(2);
		
		Fortre fortre = new Fortre(vars, Lists.newArrayList(first, second), pool);
		
		Assert.assertEquals(2, fortre.getTrunk(first).size());
		Assert.assertEquals(2, fortre.getTrunk(second).size());
	}
	
	@Test
	public void symmetricGeneralization() {
		String[] rawDobs = {
			"(X)", "(Y)", "((P)(X)(a))", "((P)(b)(Y))", "((P)(X)(Y))"
		};

		LogicFormat fmt = new StandardFormat();
		Pool pool = new Pool();
		List<Dob> dobs = pool.submergeDobs(fmt.dobsFromStrings(Arrays.asList(rawDobs)));
		Set<Dob> vars = Sets.newHashSet(dobs.get(0), dobs.get(1));
		
		Dob first = dobs.get(3);
		Dob second = dobs.get(2);
		
		Fortre fortre = new Fortre(vars, Lists.newArrayList(first, second), pool);
		
		Assert.assertEquals(3, fortre.getTrunk(first).size());
		Assert.assertEquals(3, fortre.getTrunk(second).size());
		Assert.assertEquals(2, fortre.getTrunk(dobs.get(4)).size());
	}
	
	
	// TODO: Write tests for cognates 
	// - make sure that only children that have children are compressed
	// - make sure that all cognates are stored
}
