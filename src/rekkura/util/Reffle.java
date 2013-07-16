package rekkura.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.google.common.base.Joiner;

/**
 * This class holds some super magical reflection.
 * @author ptpham
 *
 */
public class Reffle {

	/**
	 * Implementations of this interface should try their
	 * best to construct an object of the given type from 
	 * the arguments provided.
	 * @author ptpham
	 *
	 * @param <U>
	 */
	public interface Factory<U> { U create(Object... args); }
	
	public static Object lightInvoke(Method method, Object target, Object... args) {
		try { return method.invoke(target, args); }
		catch (Exception e) { return e; }
	}
	
	/**
	 * This method finds the constructor that satisfies the given prototype.
	 * @param type
	 * @param args
	 * @return
	 */
	public static <U> Constructor<?> findConstructor(final Class<U> type,
			final Class<?>... args) {
		Constructor<?>[] constructors = type.getConstructors();
		Constructor<?> chosen = null;
		for (Constructor<?> constructor : constructors) {
			Class<?>[] types = constructor.getParameterTypes();
			if (types.length != args.length) continue;
			
			boolean valid = true;
			for (int i = 0; i < types.length && valid; i++) {
				if (!types[i].equals(args[i])) valid = false;
			}
			if (!valid) continue;
			
			chosen = constructor;
			break;
		}
		return chosen;
	}
	
	/**
	 * Converts object arrays to an array of their types.
	 * @param objs
	 * @return
	 */
	public static Class<?>[] getClasses(Object[] objs) {
		Class<?>[] types = new Class<?>[objs.length];
		for (int i = 0; i < objs.length; i++) {
			types[i] = objs[i].getClass();
		}
		return types;
	}

	/**
	 * This method creates a factory for the given class that will
	 * choose from among the available constructors to create new objects.
	 * @param type
	 * @return
	 */
	public static <U> Factory<U> createFactory(final Class<?> type) {
		return new Factory<U>() {
			@SuppressWarnings("unchecked")
			@Override public U create(Object... args) {
				U result = null;
				Class<?>[] classes = getClasses(args);
				Constructor<?> constructor = findConstructor(type, classes);
				if (constructor == null) {
					String msg = "No constructor found for " + type.getCanonicalName() +
							" that takes " + Joiner.on(",").join(classes);
					throw new Error(msg);
				}
				
				try { result = (U) constructor.newInstance(args); }
				catch (Exception e) { throw new Error(e); }
				return result;
			}
		};
	}
	
	public static Class<?> lightForName(String name) {
		try { return Class.forName(name); }
		catch (ClassNotFoundException e) { return null; }
	}
	
	public static <U> Factory<? extends U> getFactory(String name, Class<U> base) {
		Class<?> raw = lightForName(name);
		if (!base.isAssignableFrom(raw)) return null;
		if (raw == null) return null;
		return createFactory(raw);
	}
}
