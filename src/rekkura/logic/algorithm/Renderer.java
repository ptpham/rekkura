
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

/**
 * A renderer is responsible for generating all possible implicated dobs
 * from a single rule and the current state of the world.
 * @author ptpham
 *
 */
public abstract class Renderer {
	
	/**
	 * This method exposes an efficient rendering process for a collection of ground dobs.
	 * If you want to apply a single assignment in a vacuum, consider Terra.applyBodies.
	 * To generate the support for this function, consider using Terra.getBodySpace.
	 * @param rule
	 * @param support
	 * @param pool
	 * @param truths
	 * @return
	 */
	public abstract List<Map<Dob,Dob>> apply(Rule rule, Set<Dob> truths, Multimap<Atom,Dob> support, Pool pool);
	
	public final Limiter.Operations ops = Limiter.forOperations();
	public static Standard getStandard() { return new Standard(); }
	public static Partitioning getPartitioning() { return new Partitioning(); }
	public static Failover getStandardFailover() {
		Standard standard = getStandard();
		standard.ops.max = 1024;
		return new Failover(standard, getPartitioning());
	}
	
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
	
	/**
	 * The partitioning renderer separates collections of dobs based on
	 * common variable assignments. It has more overhead than the Standard
	 * renderer but it will succeed where the Standard renderer fails.
	 * @author ptpham
	 *
	 */
	public static class Partitioning extends Renderer {
		public int minNonTrivial = 1024;
		public int maxPartitionDepth = 3;
		
		@Override
		public List<Map<Dob,Dob>> apply(Rule rule, Set<Dob> truths, Multimap<Atom, Dob> support, Pool pool) {
			List<Atom> positives = Atom.filterPositives(rule.body);
			Map<Atom,Integer> sizes = OtmUtil.getNumValues(support);
			List<List<Unification>> space = Terra.getUnificationSpace(rule, support, positives);
			ops.begin();
			
			// Compute expander set
			List<Atom> expanders = Terra.getGreedyExpanders(rule, sizes);
			List<Atom> check = Colut.remove(rule.body, expanders);
			
			// Partition the space based on the greedy ordering of high overlap variables
			List<Integer> selection = Colut.indexOf(positives, expanders); 
			List<Dob> candidates = Terra.getPartitionCandidates(Atom.asDobIterable(positives), rule.vars);
			List<List<List<Unification>>> partitions = partitionSpace(rule.vars, candidates,
				space, selection, maxPartitionDepth);
			
			// Do the final rendering
			List<Map<Dob,Dob>> result = Lists.newArrayList();
			for (List<List<Unification>> partition : partitions) {
				List<List<Unification>> subset = Colut.select(partition, selection);
				Cartesian.AdvancingIterator<Unification> iterator = Cartesian.asIterator(subset);
				result.addAll(apply(rule, iterator, check, pool, truths));
			}
	
			return result;
		}
		
		/**
		 * Performs a greedy recursive partitioning of the given space. At each
		 * level of the recursion, at most one variable is selected on which to
		 * partition.
		 * @param vars the list of all variables in the rule -- it will not be modified
		 * @param candidates the list of variables remaining to be selected in this invocation
		 * @param space the current space to partition in this invocation
		 * @param expanders the list of positions in the space that we are claiming will
		 * actually contribute to the size of our space. This will be used to determine
		 * whether a space is "too small" to be expanded further.
		 * @return
		 */
		public List<List<List<Unification>>> partitionSpace(List<Dob> vars,
			List<Dob> candidates, List<List<Unification>> space, List<Integer> expanders, int depthRemain) {
			
			List<List<List<Unification>>> result = Lists.newArrayList();
			if (depthRemain == 0 || Cartesian.size(Colut.select(space, expanders)) < minNonTrivial) {
				result.add(space);
				return result;
			}

			// If we could not partition the space (i.e. could not find a guide),
			// just return the full space
			Guide guide = selectGuide(candidates, vars, space);
			if (guide.values == null) { result.add(space); return result; }
			int pos = guide.pos;

			outer:
			// Expand each assignment defined by the guide
			for (Dob assign : guide.values) {
				List<List<Unification>> partitioned = Lists.newArrayList();
				for (int i = 0; i < space.size(); i++) {
					List<Unification> slice = null;
					ListMultimap<Dob, Unification> index = Terra.indexBy(space.get(i), pos);
					if (index.containsKey(assign)) slice = index.get(assign);
					else slice = index.get(null);
					if (slice.size() == 0) continue outer;
					partitioned.add(slice);
				}
				
				// Recursive expansion to handle the other variables
				result.addAll(partitionSpace(vars, Lists.newArrayList(candidates), partitioned,
					expanders, depthRemain - 1));
			}
			return result;
		}


		public static class Guide {
			Set<Dob> values;
			int pos;
		}
		
		/**
		 * In the set of atom/variable pairs such that the atom has at least two 
		 * assignments in the variable, find the pair such that the variable comes
		 * earliest in the candidate list.
		 * @param candidates
		 * @param vars
		 * @param space
		 * @return a ranked carry in which the integer is the position of the selected
		 * variable in the vars list and the carry is the set of assignments in that dimension
		 */
		public static Guide selectGuide(List<Dob> candidates,
			List<Dob> vars, List<List<Unification>> space) {
			
			Guide result = new Guide();
			Multiset<Dob> uniques = HashMultiset.create();
			while (candidates.size() > 0 && result.values == null) {
				int pos = vars.indexOf(Colut.popAny(candidates));
				RankedCarry<Integer, Set<Dob>> seen = RankedCarry.createNatural(Integer.MAX_VALUE, null);
				for (int i = 0; i < space.size(); i++) {
					List<Unification> slice = space.get(i);
					uniques.clear();
					
					if (slice.size() == 0) continue;
					if (Colut.any(slice).assigned[pos] == null) continue;
					for (Unification unify : space.get(i)) uniques.add(unify.assigned[pos]);
					seen.consider(uniques.elementSet().size(), Sets.newHashSet(uniques));
				}
				if (seen.ranker < 2) continue;
				result.values = seen.carry;
				result.pos = pos;
			}
			return result;
		}
	}
	
	/**
	 * Represents the composition of various renderers. If a renderer fails
	 * to render, it will be discarded and the next renderer will take its
	 * place. This renderer will fail when it runs out of renderers.
	 * @author ptpham
	 *
	 */
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
	
	public static Multimap<Atom,Dob> getNaiveSupport(Rule rule, Set<Dob> truths) {
		Multimap<Atom,Dob> result = HashMultimap.create();
		for (Atom atom : rule.body) {
			if (!atom.truth) continue;
			result.putAll(atom, truths);
		}
		return result;
	}
	

}
