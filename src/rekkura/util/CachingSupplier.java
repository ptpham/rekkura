package rekkura.util;

import java.util.Collection;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.Sets;

/**
 * This supplier retains a record of the objects that it creates
 * for future reference or collection by some other data structure.
 * @author ptpham
 *
 * @param <T>
 */
public abstract class CachingSupplier<T> implements Supplier<T> {
	public final Set<T> created = Sets.newHashSet();
	
	protected abstract T create();
	
	public final T get() {
		T result = create();
		created.add(result);
		return result;
	}
	
	public void deposit(Collection<T> receiver) {
		receiver.addAll(created);
		created.clear();
	}
	
	public Set<T> request(int num) {
		while (created.size() < num) create();
		return Sets.newHashSet(Colut.firstK(created, num));
	}
	
	public T request() { return request(Sets.<T>newHashSet()); }

	/**
	 * Request a set of items from the supplier that
	 * does not overlap with the given set.
	 * @param conflicts
	 * @return
	 */
	public T request(Set<T> conflicts) {
		for (T elem : created) { if (!conflicts.contains(elem)) return elem; }
		return create();
	}
}
