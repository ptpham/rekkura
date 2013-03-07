package rekkura.model;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * (Discrete Object) A dob represents a node with ordered 
 * children that may have a name attached.
 * @author ptpham
 *
 */
public class Dob implements Iterable<Dob> {
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

	public boolean isTerminal() { return this.children.size() == 0; }
	public Dob at(int pos) { return this.children.get(pos); }
	public int size() { return this.children.size(); }
	public List<Dob> childCopy() { return Lists.newArrayList(this.children); }
	
	@Override public Iterator<Dob> iterator() { return this.children.iterator(); }

	@Override public String toString() {
		if (isTerminal()) return super.toString() + "(" + this.name + ")";
		return super.toString() + "(" + this.children + ")";
	}

}
