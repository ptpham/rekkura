package rekkura.logic.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import rekkura.logic.format.StandardFormat;

/**
 * An {@link Atom} is simply a combination of a
 * {@link Dob} and a truth value.
 * @author ptpham
 *
 */
public class Atom {
	public static final ImmutableSet<Atom> EMPTY_SET = ImmutableSet.of();
	public static final ImmutableList<Atom> EMPTY_LIST = ImmutableList.of();

	public final Dob dob;
	public final boolean truth;
	
	public Atom(Dob dob, boolean truth) {
		this.dob = dob; 
		this.truth = truth;
	}
	
	public static Comparator<Atom> getComparator(final Collection<Dob> vars) {
		return new Comparator<Atom>() {
			@Override public int compare(Atom arg0, Atom arg1) {
				return Atom.compare(arg0, arg1, vars);
			}
		};
	}

	public static int compare(Atom arg0, Atom arg1, final Collection<Dob> vars) {
		if (arg0.truth && !arg1.truth) return -1;
		if (!arg0.truth && arg1.truth) return 1;
		
		int varComp = Dob.compareStructure(arg0.dob, arg1.dob, vars);
		if (varComp != 0) return varComp;

		return Dob.compare(arg0.dob, arg1.dob);
	}
	
	public static Iterator<Dob> dobIteratorFromAtoms(final Iterable<Atom> atoms) {
		return dobIteratorFromAtoms(atoms.iterator());
	}
	
	public static Iterator<Dob> dobIteratorFromAtoms(final Iterator<Atom> atoms) {
		return new Iterator<Dob>() {
			Iterator<Atom> inner = atoms;
			@Override public boolean hasNext()  { return inner.hasNext(); }
			@Override public Dob next() { return inner.next().dob; }
			@Override public void remove() { }
		};
	}
	
	public static Iterable<Dob> dobIterableFromAtoms(final Iterable<Atom> atoms) {
		return new Iterable<Dob>() {
			@Override public Iterator<Dob> iterator() { return dobIteratorFromAtoms(atoms); }
		};
	}
	
	public static List<Dob> asDobList(Iterable<Atom> atoms) {
		return Lists.newArrayList(dobIterableFromAtoms(atoms));
	}

	public static List<Atom> filterPositives(Collection<Atom> atoms) {
		List<Atom> positives = Lists.newArrayList();
		for (Atom atom : atoms) { if (atom.truth) positives.add(atom); }
		return positives;
	}
	
	public static List<Atom> filterNegatives(Collection<Atom> atoms) {
		List<Atom> negatives = Lists.newArrayList();
		for (Atom atom : atoms) { if (!atom.truth) negatives.add(atom); }
		return negatives;
	}
	
	@Override public String toString() { return StandardFormat.inst.toString(this); }
}
