package rekkura.model;

import java.util.Collection;
import java.util.Iterator;

public class Atom {
	public final Dob dob;
	public final boolean truth;
	
	public Atom(Dob dob, boolean truth) {
		this.dob = dob; 
		this.truth = truth;
	}
	
	public static Iterator<Dob> dobIteratorFromAtoms(final Collection<Atom> atoms) {
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
	
	public static Iterable<Dob> dobIterableFromAtoms(final Collection<Atom> atoms) {
		return new Iterable<Dob>() {
			@Override public Iterator<Dob> iterator() { return dobIteratorFromAtoms(atoms); }
		};
	}
}
