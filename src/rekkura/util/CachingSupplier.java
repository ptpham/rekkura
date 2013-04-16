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
}
