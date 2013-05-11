package rekkura.test.logic;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.logic.Lattice;
import rekkura.logic.Lattice.Varpar;
import rekkura.logic.Pool;
import rekkura.model.Dob;
import rekkura.util.Colut;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class LatticeTest {

	@Test
	public void partition() {
		List<String> raw = Lists.newArrayList("((X)(J))",
			"((I)(J))", "(X)", "(I)", "(J)");
		
		Pool pool = new Pool();
		List<Dob> dobs = pool.dobs.submergeStrings(raw);
		List<Dob> vars = Colut.slice(dobs, 2, raw.size());
		
		Varpar vp = Lattice.partition(dobs.get(0), dobs.get(1), vars);
		Assert.assertEquals(Lists.newArrayList(dobs.get(2)), vp.first);
		Assert.assertEquals(Lists.newArrayList(dobs.get(3)), vp.second);
		Assert.assertEquals(Lists.newArrayList(dobs.get(4)), vp.both);
	}
	
	@Test
	public void represent() {
		List<String> raw = Lists.newArrayList("((f)(Y)(X))",
			"(X)", "(Y)", "((f)(a)(b))", "((f)(b)(c))", "((b)(a))", "((c)(b))");
		
		Pool pool = new Pool();
		List<Dob> dobs = pool.dobs.submergeStrings(raw);
		List<Dob> vars = Colut.slice(dobs, 1, 3);
		List<Dob> grounds = Colut.slice(dobs, 3, 5);
		List<Dob> lattice = Colut.slice(dobs, 5, 7);
		
		Map<Dob, Dob> result = Lattice.represent(grounds, dobs.get(0), vars, pool);
		Map<Dob, Dob> expected = Maps.newHashMap();
		expected.put(grounds.get(0), lattice.get(0));
		expected.put(grounds.get(1), lattice.get(1));
		Assert.assertEquals(expected, result);
	}
	
	@Test
	public void extractJoined() {
		List<String> raw = Lists.newArrayList("((g)(X))", "((f)(Y))", "((r)(X)(Y))",
			"(X)", "(Y)", "((r)(a)(b))", "((r)(b)(c))", "((a))", "((b))", "((c))");
		
		Pool pool = new Pool();
		List<Dob> dobs = pool.dobs.submergeStrings(raw);
		List<Dob> vars = Colut.slice(dobs, 3, 5);
		List<Dob> grounds = Colut.slice(dobs, 5, 7);
		
		Lattice.Request req = new Lattice.Request(dobs.get(0), dobs.get(1), dobs.get(2));
		Lattice lattice = Lattice.extract(req, vars, grounds, pool);

		Multimap<Dob, Dob> expected = HashMultimap.create();
		expected.put(dobs.get(8), dobs.get(9));
		expected.put(dobs.get(7), dobs.get(8));
		
		Assert.assertEquals(expected, lattice.edges);
	}
	
}
