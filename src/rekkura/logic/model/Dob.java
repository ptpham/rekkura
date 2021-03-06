package rekkura.logic.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import rekkura.logic.format.StandardFormat;
import rekkura.util.CachingSupplier;
import rekkura.util.Colut;
import rekkura.util.NestedIterable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * (Discrete Object) A dob represents a node with ordered 
 * children that may have a name attached.
 * @author ptpham
 *
 */
public class Dob {
	public static final ImmutableSet<Dob> EMPTY_SET = ImmutableSet.of();
	public static final ImmutableList<Dob> EMPTY_LIST = ImmutableList.of();
	
	public final String name;
	private List<Dob> children;
	
	public Dob(String name) { 
		this.name = name;
		this.children = Lists.newArrayList();
	}
	
	public Dob(List<Dob> children) { 
		this.name = "";
		this.children = Lists.newArrayList(children);
	}
	
	public Dob(Dob... dobs) {
		this.name = "";
		this.children = Lists.newArrayList(dobs);
	}

	public boolean isTerminal() { return this.children.size() == 0; }
	public Dob at(int pos) { return this.children.get(pos); }
	public int size() { return this.children.size(); }
	public List<Dob> childCopy() { return Lists.newArrayList(this.children); }
	public Dob deepCopy() {
		if (isTerminal()) return new Dob(name);
		List<Dob> copied = Lists.newArrayList();
		for (Dob dob : children) copied.add(dob.deepCopy());
		return new Dob(copied);
	}

	public Iterator<Dob> fullIterator() {
		return new Iterator<Dob>() {
			Stack<Dob> unused = new Stack<Dob>();
			{ unused.push(Dob.this); }
			
			@Override public boolean hasNext() 
			{ return unused.size() > 0; }

			@Override
			public Dob next() {
				Dob result = unused.pop();
				for (Dob dob : result.childIterable()) unused.push(dob);
				return result;
			}

			@Override public void remove() 
			{ throw new IllegalAccessError("Remove not allowed!"); }
		};
	}

	public static int compare(Dob first, Dob second) {
		return compareStructure(first, second, null);
	}
	
	/**
	 * This method will order dobs by structural properties with
	 * respect to variables. A dob that has a variable that is closer
	 * to the surface will come earlier in the ordering than a variable
	 * that is farther from the surface.
	 * @param first
	 * @param second
	 * @param vars
	 * @return
	 */
	public static int compareStructure(Dob first, Dob second, Collection<Dob> vars) {
		boolean firstVar = Colut.contains(vars, first);
		boolean secondVar = Colut.contains(vars, second);
		
		if (firstVar && secondVar) return 0;
		if (firstVar && !secondVar) return -1;
		if (!firstVar && secondVar) return 1;
		
		if (first.size() == second.size()) {
			for (int i = 0; i < first.size(); i++) {
				int childComp = compareStructure(first.at(i), second.at(i), vars);
				if (childComp != 0) return childComp;
			}
		}

		int strComp = first.name.compareTo(second.name);
		if (strComp != 0) return strComp;
		
		return first.size() - second.size();
	}
	
	public static Comparator<Dob> getComparator() { return getComparator(null); }
	public static Comparator<Dob> getComparator(final Collection<Dob> vars) {
		return new Comparator<Dob>() {
			@Override public int compare(Dob arg0, Dob arg1) {
				return Dob.compareStructure(arg0, arg1, vars);
			}
		};
	}
	
	public Iterable<Dob> fullIterable() {
		return new Iterable<Dob>() {
			@Override public Iterator<Dob> iterator() {
				return Dob.this.fullIterator();
			}
		};
	}

	public Iterable<Dob> childIterable() { return this.children; }
	
	public static Iterable<Dob> fullIterable(Iterable<Dob> dobs) {
		return new NestedIterable<Dob, Dob>(dobs) {
			@Override protected Iterator<Dob> prepareNext(Dob dob) {
				return dob.fullIterator();
			}
		};
	}
	
	@Override public String toString() { return StandardFormat.inst.toString(this); }
	
	public static class PrefixedSupplier extends CachingSupplier<Dob> {
		private int current = 0;
		public final String prefix;
		public PrefixedSupplier(String prefix) { this.prefix = prefix; }
		@Override public Dob create() { return new Dob("[" + prefix + current++ + "]"); }
	};
	
	public static List<Dob> sortByStructure(Iterable<Dob> dobs) {
		List<Dob> result = Lists.newArrayList(dobs);
		Collections.sort(result, getComparator());
		return result;
	}
}
