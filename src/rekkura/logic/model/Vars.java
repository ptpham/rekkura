package rekkura.logic.model;

import java.util.Set;

import rekkura.util.CachingSupplier;
import rekkura.util.Colut;

import com.google.common.collect.Sets;

public abstract class Vars {
	
	/**
	 * A context makes it nicer to do a large number of logical
	 * operations over a set of rules. It makes it unnecessary to 
	 * recompute the union of variable sets because the context
	 * represents the union of all variable sets.
	 * TODO: Think about where/how to deal with conflicts 
	 * in which rules disagree on whether something is a variable.
	 * @author ptpham
	 *
	 */
	public static interface Context {
		/**
		 * Request a new variable dob that is guaranteed 
		 * to not conflict with an existing variable.
		 * @return
		 */
		public Dob create();
		public Set<Dob> getAll();
	}
	
	public static Set<Dob> request(int num, Context context) {
		while (context.getAll().size() < num) context.create();
		return Sets.newHashSet(Colut.firstK(context.getAll(), num));
	}
	
	public static Dob request(Context context) {
		return request(Dob.EMPTY_SET, context);
	}

	/**
	 * Request a set of dobs from this context that
	 * does not overlap with the given set.
	 * @param conflicts
	 * @return
	 */
	public static Dob request(Set<Dob> conflicts, Context context) {
		for (Dob dob : context.getAll()) { if (!conflicts.contains(dob)) return dob; }
		return context.create();
	}
	
	public static Context asContext(final Set<Dob> dobs, final CachingSupplier<Dob> vargen) {
		return new Context() {
			@Override public Dob create() {
				Dob result = vargen.get();
				vargen.deposit(dobs);
				return result;
			}
			
			@Override public Set<Dob> getAll() { return dobs; }
		};
	}

	public static Vars.Context copy(String prefix, Context context) {
		Dob.PrefixedSupplier vargen = new Dob.PrefixedSupplier(prefix);
		return Vars.asContext(Sets.newHashSet(context.getAll()), vargen);
	}
}
