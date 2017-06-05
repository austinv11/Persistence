package com.austinv11.persistence;

import kotlin.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This is a DAO interface for dealing with local persistence.
 * NOTE: It is expected that implementations are thread-safe.
 */
public interface Store<T> extends Iterable<T>, RandomAccess {
	
	/**
	 * Inserts an object into the storeFor.
	 * 
	 * @param obj The object to insert.
	 * @return The object replaced (if there was a hash collision).
	 */
	@Nullable T insert(@Nonnull T obj);
	
	/**
	 * Inserts an object into the storeFor quietly (does not notify peers).
	 *
	 * @param obj The object to insert.
	 * @return The object replaced (if there was a hash collision).
	 */
	@Nullable T insertQuietly(@Nonnull T obj);
	
	/**
	 * Un-persists an object.
	 * 
	 * @param obj The object to remove.
	 * @return Whether this was successful.
	 */
	boolean remove(@Nonnull T obj);
	
	/**
	 * Un-persists an object given its PPPP hash.
	 *
	 * @param hash The hash to remove.
	 * @return Whether this was successful.
	 * 
	 * @see PersistenceManager#generateHash(Object)
	 */
	boolean removeHash(long hash);
	
	/**
	 * Un-persists an object quietly (does not notify peers).
	 *
	 * @param obj The object to remove.
	 * @return Whether this was successful.
	 */
	boolean removeQuietly(@Nonnull T obj);
	
	/**
	 * Un-persists an object given its PPPP hash quietly (does not notify peers).
	 *
	 * @param hash The hash to remove.
	 * @return Whether this was successful.
	 *
	 * @see PersistenceManager#generateHash(Object)
	 */
	boolean removeHashQuietly(long hash);
	
	/**
	 * Checks if the provided object is already stored.
	 * 
	 * @param obj The object to look for.
	 * @return True if stored, false if otherwise.
	 */
	boolean contains(@Nonnull T obj);
	
	/**
	 * Checks if the provided object PPPP hash is already stored.
	 *
	 * @param hash The hash to look for.
	 * @return True if stored, false if otherwise.
	 */
	boolean containsHash(long hash);
	
	/**
	 * Gets a persisted object from its hash.
	 * 
	 * @param hash The hash representing the object to get.
	 * @return The object, or null if not found.
	 */
	@Nullable T get(long hash);
	
	/**
	 * Updates an object which is already stored.
	 * 
	 * @param originalHash The hash of the original object.
	 * @param obj The new object.
	 * @param hint The hint for the changed field. This is a pair where the first value is the property type and the 
	 * last value is the property name.
	 * @return The original object, or null if the operation failed.
	 * 
	 * @throws java.util.NoSuchElementException Thrown when the original hash doesn't correspond to anything stored.
	 */
	@Nullable T update(long originalHash, @Nonnull T obj, @Nonnull Pair<Class<?>, String> hint) throws NoSuchElementException;
	
	/**
	 * Updates an object which is already stored quietly (does not notify peers).
	 *
	 * @param originalHash The hash of the original object.
	 * @param obj The new object.
	 * @return The original object, or null if the operation failed.
	 *
	 * @throws java.util.NoSuchElementException Thrown when the original hash doesn't correspond to anything stored.
	 */
	@Nullable T updateQuietly(long originalHash, @Nonnull T obj) throws NoSuchElementException;
	
	/**
	 * Gets the amount of elements stored.
	 * 
	 * @return The number of elements stored.
	 */
	int size();
	
	/**
	 * Gets if this storeFor is empty.
	 * 
	 * @return True if empty, false if otherwise.
	 */
	default boolean isEmpty() {
		return size() == 0;
	}
	
	/**
	 * Clears all objects from the storeFor.
	 */
	void clear();
	
	/**
	 * Clears all objects from the storeFor quietly (does not notify peers).
	 */
	void clearQuietly();
	
	/**
	 * Collects the current storeFor into a collection.
	 * 
	 * @return The collection represent the current contents.
	 */
	@Nonnull Collection<T> collect();
	
	//Taken from Collection
	
	/**
	 * Creates a {@link Spliterator} over the elements in this collection.
	 *
	 * Implementations should document characteristic values reported by the
	 * spliterator.  Such characteristic values are not required to be reported
	 * if the spliterator reports {@link Spliterator#SIZED} and this collection
	 * contains no elements.
	 *
	 * <p>The default implementation should be overridden by subclasses that
	 * can return a more efficient spliterator.  In order to
	 * preserve expected laziness behavior for the {@link #stream()} and
	 * {@link #parallelStream()}} methods, spliterators should either have the
	 * characteristic of {@code IMMUTABLE} or {@code CONCURRENT}, or be
	 * <em><a href="Spliterator.html#binding">late-binding</a></em>.
	 * If none of these is practical, the overriding class should describe the
	 * spliterator's documented policy of binding and structural interference,
	 * and should override the {@link #stream()} and {@link #parallelStream()}
	 * methods to create streams using a {@code Supplier} of the spliterator,
	 * as in:
	 * <pre>{@code
	 *     Stream<E> s = StreamSupport.stream(() -> spliterator(), spliteratorCharacteristics)
	 * }</pre>
	 * <p>These requirements ensure that streams produced by the
	 * {@link #stream()} and {@link #parallelStream()} methods will reflect the
	 * contents of the collection as of initiation of the terminal stream
	 * operation.
	 *
	 * @implSpec
	 * The default implementation creates a
	 * <em><a href="Spliterator.html#binding">late-binding</a></em> spliterator
	 * from the collections's {@code Iterator}.  The spliterator inherits the
	 * <em>fail-fast</em> properties of the collection's iterator.
	 * <p>
	 * The created {@code Spliterator} reports {@link Spliterator#SIZED}.
	 *
	 * @implNote
	 * The created {@code Spliterator} additionally reports
	 * {@link Spliterator#SUBSIZED}.
	 *
	 * <p>If a spliterator covers no elements then the reporting of additional
	 * characteristic values, beyond that of {@code SIZED} and {@code SUBSIZED},
	 * does not aid clients to control, specialize or simplify computation.
	 * However, this does enable shared use of an immutable and empty
	 * spliterator instance (see {@link Spliterators#emptySpliterator()}) for
	 * empty collections, and enables clients to determine if such a spliterator
	 * covers no elements.
	 *
	 * @return a {@code Spliterator} over the elements in this collection
	 * @since 1.8
	 */
	@Override
	default Spliterator<T> spliterator() {
		return Spliterators.spliterator(collect(), 0);
	}
	
	/**
	 * Returns a sequential {@code Stream} with this collection as its source.
	 *
	 * <p>This method should be overridden when the {@link #spliterator()}
	 * method cannot return a spliterator that is {@code IMMUTABLE},
	 * {@code CONCURRENT}, or <em>late-binding</em>. (See {@link #spliterator()}
	 * for details.)
	 *
	 * @implSpec
	 * The default implementation creates a sequential {@code Stream} from the
	 * collection's {@code Spliterator}.
	 *
	 * @return a sequential {@code Stream} over the elements in this collection
	 * @since 1.8
	 */
	default Stream<T> stream() {
		return StreamSupport.stream(spliterator(), false);
	}
	
	/**
	 * Returns a possibly parallel {@code Stream} with this collection as its
	 * source.  It is allowable for this method to return a sequential stream.
	 *
	 * <p>This method should be overridden when the {@link #spliterator()}
	 * method cannot return a spliterator that is {@code IMMUTABLE},
	 * {@code CONCURRENT}, or <em>late-binding</em>. (See {@link #spliterator()}
	 * for details.)
	 *
	 * @implSpec
	 * The default implementation creates a parallel {@code Stream} from the
	 * collection's {@code Spliterator}.
	 *
	 * @return a possibly parallel {@code Stream} over the elements in this
	 * collection
	 * @since 1.8
	 */
	default Stream<T> parallelStream() {
		return StreamSupport.stream(spliterator(), true);
	}
}
