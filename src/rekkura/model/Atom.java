package rekkura.model;

import java.util.Iterator;

import rekkura.fmt.StandardFormat;

public class Atom {
	public final Dob dob;
	public final boolean truth;
	
	public Atom(Dob dob, boolean truth) {
		this.dob = dob; 
		this.truth = truth;
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
	
	@Override public String toString() { return StandardFormat.inst.toString(this); }
}
