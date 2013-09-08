
package rekkura.logic.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.model.Unification;
import rekkura.logic.structure.Pool;
import rekkura.util.Cartesian;
import rekkura.util.Cartesian.AdvancingIterator;
import rekkura.util.Colut;
import rekkura.util.Limiter;
import rekkura.util.OtmUtil;
import rekkura.util.RankedCarry;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public abstract class Renderer {
	public abstract List<Map<Dob,Dob>> apply(Rule rule, Set<Dob> truths, Multimap<Atom,Dob> support, Pool pool);
	
	public final Limiter.Operations ops = Limiter.forOperations();
	public static Standard getStandard() { return new Standard(); }
	public static Partitioning getPartitioning() { return new Partitioning(); }
	public static Failover getStandardFailover() {
		Standard standard = getStandard();
		standard.ops.max = 1024;
		return new Failover(standard, getPartitioning());
	}
	
	/**
	 * This method exposes an efficient rendering process for a collection of ground dobs.
	 * If you want to apply a single assignment in a vaccuum, consider applyBodies.
	 * To generate the support for this function, consider using getBodySpace.
	 * @param rule
	 * @param support
	 * @param pool
	 * @param truths
	 * @return
	 */
	protected List<Map<Dob,Dob>> applyUnifications(Rule rule, 
		AdvancingIterator<Unification> iterator, List<Atom> check, Pool pool, Set<Dob> truths) {
		List<Map<Dob,Dob>> result = Lists.newArrayList();
		
		// This block deals with the vacuous rule special case ...
		Dob varless = Terra.applyVarless(rule, truths);
		if (varless != null) {
			result.add(Maps.<Dob,Dob>newHashMap());
			return result;
		}
		
		return Terra.expandUnifications(rule, check, iterator, pool, truths, this.ops);
	}
	
	protected List<Map<Dob,Dob>> apply(Rule rule, Cartesian.AdvancingIterator<Unification> iterator,
		List<Atom> check, Pool pool, Set<Dob> truths) {
		return applyUnifications(rule, iterator, check, pool, truths);
	}
	
	public static class Standard extends Renderer {
		@Override public List<Map<Dob,Dob>> apply(Rule rule,
			Set<Dob> truths, Multimap<Atom, Dob> support, Pool pool) {
			Map<Atom,Integer> sizes = OtmUtil.getNumValues(support);
			ops.begin();

			List<Atom> expanders = Terra.getGreedyExpanders(rule, sizes);
			List<Atom> check = Colut.remove(rule.body, expanders);
			Cartesian.AdvancingIterator<Unification> iterator =
				Terra.getUnificationIterator(rule, expanders, support, truths);
			
			return apply(rule, iterator, check, pool, truths);
		}
	}
	
	public static class Partitioning extends Renderer {
		public int minNonTrival = 1024;
		
		@Override
		public List<Map<Dob,Dob>> apply(Rule rule, Set<Dob> truths, Multimap<Atom, Dob> support, Pool pool) {
			List<Atom> positives = Atom.filterPositives(rule.body);
			List<List<Unification>> space = Terra.getUnificationSpace(rule, support, positives);
			ops.begin();

			// Compute expander set
			Map<Atom,Integer> sizes = OtmUtil.getNumValues(support);
			List<Atom> expanders = Terra.getGreedyExpanders(rule, sizes);
			List<Atom> check = Colut.remove(rule.body, expanders);
			
			// Partition the space based on the greedy ordering of high overlap variables
			List<Integer> selection = Colut.indexOf(positives, expanders); 
			List<Dob> candidates = Terra.getPartitionCandidates(Atom.asDobIterable(positives), rule.vars);
			List<List<List<Unification>>> partitions = partitionSpace(rule.vars, candidates,
				space, selection, minNonTrival, this.ops);
			
			// Do the final rendering
			List<Map<Dob,Dob>> result = Lists.newArrayList();
			for (List<List<Unification>> partition : partitions) {
				List<List<Unification>> subset = Colut.select(partition, selection);
				Cartesian.AdvancingIterator<Unification> iterator = Cartesian.asIterator(subset);
				result.addAll(apply(rule, iterator, check, pool, truths));
			}
	
			return result;
		}	
	}
	
	public static class Failover extends Renderer {
		public final ImmutableList<Renderer> children;
		private Renderer current;
		
		private Failover(Renderer... children) {
			this.children = ImmutableList.copyOf(children);
			this.current = Colut.any(this.children);
			this.ops.max = this.children.size();
			this.ops.begin();
		}
		
		@Override
		public List<Map<Dob, Dob>> apply(Rule rule, Set<Dob> truths,
				Multimap<Atom, Dob> support, Pool pool) {
			List<Map<Dob,Dob>> result = Lists.newArrayList();
			
			while (true) {
				if (current == null) {
					int pos = (int)this.ops.cur;
					if (this.ops.exceeded()) return result;
					current = this.children.get(pos);
				}
	
				result = current.apply(rule, truths, support, pool);
				if (current.ops.failed) current = null;
				else return result;
			}
		}
	}
	
	public static Multimap<Atom,Dob> getTrivialSupport(Rule rule, Set<Dob> truths) {
		Multimap<Atom,Dob> result = HashMultimap.create();
		for (Atom atom : rule.body) {
			if (!atom.truth) continue;
			result.putAll(atom, truths);
		}
		return result;
	}
	
	public static List<List<List<Unification>>> partitionSpace(List<Dob> vars,
		List<Dob> candidates, List<List<Unification>> space, List<Integer> expanders,
		int minNonTrivial, Limiter limiter) {
		
		List<List<List<Unification>>> result = Lists.newArrayList();
		if (Cartesian.size(Colut.select(space, expanders)) < minNonTrivial) {
			result.add(space);
			return result;
		}

		// In the set of atom/variable pairs such that the atom has at least two 
		// assignments in the variable, find the pair such that the variable comes
		// earliest in the candidate list.
		int pos = -1;
		RankedCarry<Integer, Set<Dob>> rc = RankedCarry.createNatural(Integer.MAX_VALUE, null);
		Multiset<Dob> uniques = HashMultiset.create();
		while (candidates.size() > 0 && rc.getCarry() == null) {
			pos = vars.indexOf(Colut.popAny(candidates));
			for (int i = 0; i < space.size(); i++) {
				uniques.clear();
				for (Unification unify : space.get(i)) uniques.add(unify.assigned[pos]);
				if (uniques.elementSet().size() < 2) continue;
				rc.consider(uniques.elementSet().size(), Sets.newHashSet(uniques.elementSet()));
				if (limiter.exceeded()) break;
			}
		}
		
		// If we could not partition the space (i.e. could not find a guide),
		// just return the full space
		if (rc.getCarry() == null) { result.add(space); return result; }
		
		outer:
		// Expand each assignment defined by the guide
		for (Dob assign : rc.getCarry()) {
			List<List<Unification>> partitioned = Lists.newArrayList();
			for (int i = 0; i < space.size(); i++) {
				if (limiter.exceeded()) break;
				List<Unification> slice = null;
				ListMultimap<Dob, Unification> index = Terra.indexBy(space.get(i), pos);
				if (index.containsKey(assign)) slice = index.get(assign);
				else slice = index.get(null);
				if (slice.size() == 0) continue outer;
				partitioned.add(slice);
			}
			
			// Recursive expansion to handle the other variables
			result.addAll(partitionSpace(vars, Lists.newArrayList(candidates), partitioned,
				expanders,  minNonTrivial, limiter));
		}
		return result;
	}
}
