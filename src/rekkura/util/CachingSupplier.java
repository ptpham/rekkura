package rekkura.util;

import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.Sets;

public abstract class CachingSupplier<T> implements Supplier<T> {
	public final Set<T> created = Sets.newHashSet();
	
	protected abstract T create();
	
	public final T get() {
		T result = create();
		created.add(result);
		return result;
	}
}
