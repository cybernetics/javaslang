/**    / \____  _    ______   _____ / \____   ____  _____
 *    /  \__  \/ \  / \__  \ /  __//  \__  \ /    \/ __  \   Javaslang
 *  _/  // _\  \  \/  / _\  \\_  \/  // _\  \  /\  \__/  /   Copyright 2014 Daniel Dietrich
 * /___/ \_____/\____/\_____/____/\___\_____/_/  \_/____/    Licensed under the Apache License, Version 2.0
 */
package javaslang.collection;

import static java.util.stream.Collectors.joining;
import static javaslang.Requirements.requireNonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javaslang.Requirements.UnsatisfiedRequirementException;
import javaslang.Strings;
import javaslang.Tuples;
import javaslang.Tuples.Tuple2;

/**
 * An immutable List implementation, suitable for concurrent programming.
 * <p>
 * A List is composed of a {@code head()} element and a {@code tail()} List.
 * <p>
 * There are two implementations of the interface List:
 * <ul>
 * <li>{@link EmptyList}, which represents a List containing no elements.</li>
 * <li>{@link LinearList}, which represents a List containing elements.</li>
 * </ul>
 * <p>
 * Use {@code List.of(1, 2, 3)} instead of
 * {@code new LinearList(1, new LinearList(2, new LinearList(3, EmptyList.instance())))}.
 * <p>
 * Use {@code List.empty()} instead of {@code EmptyList.instance()}.
 * <p>
 * In contrast to the mutable List variant {@link java.util.ArrayList}, it does not make sense for immutable Lists to
 * implement the interface {@link java.lang.Cloneable} because of the following conclusion: <blockquote>
 * "[...] , it doesn’t make sense for immutable classes to support object copying, because copies would be virtually indistinguishable from the original."
 * </blockquote> <em>(see Effective Java, 2nd ed., p. 61)</em>.
 * 
 * @param <E> Component type of the List.
 */
public interface List<E> extends Iterable<E> {

	/**
	 * Returns the first element of this List in O(1).
	 * 
	 * @return The head of this List.
	 * @throws UnsupportedOperationException if this is EmptyList.
	 */
	E head();

	/**
	 * Returns all elements except the first element of this List in O(1).
	 * 
	 * @return The tail of this List.
	 * @throws UnsupportedOperationException if this is EmptyList.
	 */
	List<E> tail();

	/**
	 * Tests whether this List is empty in O(1).
	 * 
	 * @return true, if this List is empty, false otherwise.
	 */
	boolean isEmpty();

	/**
	 * Reverses this List and returns a new List in O(n).
	 * <p>
	 * The result is equivalent to
	 * 
	 * <pre>
	 * <code>List&lt;E&gt; reverse(List&lt;E&gt; reversed, List&lt;E&gt; remaining) {
	 *     if (remaining.isEmpty()) {
	 *        return reversed; 
	 *     } else {
	 *        return reverse(reversed.prepend(remaining.head()), remaining.tail());
	 *     }
	 * }
	 * reverse(EmptyList.instance(), this);</code>
	 * </pre>
	 * 
	 * but implemented without recursion.
	 * 
	 * @return A new List containing the elements of this List in reverse order.
	 */
	default List<E> reverse() {
		List<E> result = EmptyList.instance();
		for (List<E> list = this; !list.isEmpty(); list = list.tail()) {
			result = result.prepend(list.head());
		}
		return result;
	}

	/**
	 * Calculates the size of a List in O(n).
	 * <p>
	 * The result is equivalent to {@code isEmpty() ? 0 : 1 + tail().size()} but implemented without recursion.
	 * 
	 * @return The size of this List.
	 */
	default int size() {
		int result = 0;
		for (List<E> list = this; !list.isEmpty(); list = list.tail(), result++)
			;
		return result;
	}

	/**
	 * Appends an element to this List in O(2n).
	 * <p>
	 * The result is equivalent to {@code reverse().prepend(element).reverse()}.
	 * 
	 * @param element An element.
	 * @return A new List containing the elements of this list, appended the given element.
	 */
	default List<E> append(E element) {
		if (isEmpty()) {
			return new LinearList<>(element, this);
		} else {
			return reverse().prepend(element).reverse();
		}
	}

	/**
	 * Appends all elements of a given List to this List in O(2n). This implementation returns
	 * {@code elements.prependAll(this)}.
	 * <p>
	 * Example: {@code List.of(1,2,3).appendAll(List.of(4,5,6))} equals {@code List.of(1,2,3,4,5,6)} .
	 * 
	 * @param elements Elements to be appended.
	 * @return A new List containing the given elements appended to this List.
	 * @throws javaslang.Requirements.UnsatisfiedRequirementException if elements is null
	 */
	@SuppressWarnings("unchecked")
	default List<E> appendAll(Iterable<? extends E> elements) {
		requireNonNull(elements, "elements is null");
		return ((List<E>) List.of(elements)).prependAll(this);
	}

	/**
	 * Prepends an element to this List in O(1).
	 * <p>
	 * The result is equivalent to {@code new LinearList<>(element, this)}.
	 * 
	 * @param element An element.
	 * @return A new List containing the elements of this list, prepended the given element.
	 */
	default List<E> prepend(E element) {
		return new LinearList<>(element, this);
	}

	/**
	 * Prepends all elements of a given List to this List in O(2n).
	 * <p>
	 * If this.isEmpty(), elements is returned. If elements.isEmpty(), this is returned. Otherwise elements are
	 * prepended to this.
	 * <p>
	 * Example: {@code List.of(4,5,6).prependAll(List.of(1,2,3))} equals {@code List.of(1,2,3,4,5,6)}.
	 * <p>
	 * The result is equivalent to
	 * {@code elements.isEmpty() ? this : prependAll(elements.tail()).prepend(elements.head())} but implemented without
	 * recursion.
	 * 
	 * @param elements Elements to be prepended.
	 * @return A new List containing the given elements prepended to this List.
	 * @throws javaslang.Requirements.UnsatisfiedRequirementException if elements is null
	 */
	@SuppressWarnings("unchecked")
	default List<E> prependAll(Iterable<? extends E> elements) {
		requireNonNull(elements, "elements is null");
		final List<? extends E> elementList = List.of(elements);
		if (isEmpty()) {
			return (List<E>) elementList;
		} else if (elementList.isEmpty()) {
			return this;
		} else {
			List<E> result = this;
			for (List<? extends E> list = elementList.reverse(); !list.isEmpty(); list = list.tail()) {
				result = result.prepend(list.head());
			}
			return result;
		}
	}

	/**
	 * Inserts the given element at the specified index into this List in O(n).
	 * <p>
	 * Examples:
	 * 
	 * <pre>
	 * <code>().insert(0, 1) = (1)
	 * (4).insert(0, 1) = (1,4)
	 * (4).insert(1, 1) = (4,1)
	 * (1,2,3).insert(2, 4) = (1,2,4,3)</code>
	 * </pre>
	 * 
	 * @param index The insertion index.
	 * @param element An element to be inserted.
	 * @return This List with the given element inserted at the given index.
	 * @throws IndexOutOfBoundsException if the index &lt; 0 or index &gt; size()
	 */
	default List<E> insert(int index, E element) {
		if (index < 0) {
			throw new IndexOutOfBoundsException("insert(" + index + ", e)");
		}
		List<E> preceding = EmptyList.instance();
		List<E> tail = this;
		for (int i = index; i > 0; i--, tail = tail.tail()) {
			if (tail.isEmpty()) {
				throw new IndexOutOfBoundsException("insert(" + index + ", e) on list of size " + size());
			}
			preceding = preceding.prepend(tail.head());
		}
		List<E> result = tail.prepend(element);
		for (E next : preceding) {
			result = result.prepend(next);
		}
		return result;
	}

	/**
	 * Inserts all of the given elements at the specified index into this List in O(n).
	 * <p>
	 * Examples:
	 * 
	 * <pre>
	 * <code>().insertAll(0, (1,2,3)) = (1,2,3)
	 * (4).insertAll(0, (1,2,3)) = (1,2,3,4)
	 * (4).insertAll(1, (1,2,3)) = (4,1,2,3)
	 * (1,2,3).insertAll(2, (4,5)) = (1,2,4,5,3)</code>
	 * </pre>
	 * <p>
	 * The result is roughly (without bounds check) equivalent to
	 * 
	 * <pre>
	 * <code>if (isEmpty()) {
	 *     return elements;
	 * } else if (index == 0) {
	 *     if (elements.isEmpty()) {
	 *         return this;
	 *     } else {
	 *         return new LinearList(elements.head(), insertAll(0, elements.tail()));
	 *     }
	 * } else {
	 *     return new LinearList(head(), tail().insertAll(index - 1, elements));
	 * }</code>
	 * </pre>
	 * 
	 * @param index The insertion index.
	 * @param elements The elements to be inserted.
	 * @return This List with the given elements inserted at the given index.
	 * @throws IndexOutOfBoundsException if the index &lt; 0 or index &gt; size()
	 */
	default List<E> insertAll(int index, Iterable<? extends E> elements) {
		if (index < 0) {
			throw new IndexOutOfBoundsException("insertAll(" + index + ", elements)");
		}
		List<E> preceding = EmptyList.instance();
		List<E> tail = this;
		for (int i = index; i > 0; i--, tail = tail.tail()) {
			if (tail.isEmpty()) {
				throw new IndexOutOfBoundsException("insertAll(" + index + ", elements) on list of size " + size());
			}
			preceding = preceding.prepend(tail.head());
		}
		List<E> result = tail.prependAll(elements);
		for (E next : preceding) {
			result = result.prepend(next);
		}
		return result;
	}

	/**
	 * Removes the first occurrence of the given element from this list if it is present in O(n).
	 * <p>
	 * Example: {@code List.of(1,2,3).remove(2)} equals {@code List.of(1,3)}.
	 * <p>
	 * The result is equivalent to
	 * 
	 * <pre>
	 * <code>if (isEmpty()) {
	 *     return this;
	 * } else if (head().equals(element)) {
	 *     return tail();
	 * } else {
	 *     return new LinearList(head(), tail().remove(element));
	 * }</code>
	 * </pre>
	 * 
	 * but implemented without recursion.
	 * 
	 * @param element An element to be removed from this List.
	 * @return A new list where the first occurrence of the element is removed or the same list, if the given element is
	 *         not part of the list.
	 */
	default List<E> remove(E element) {
		List<E> preceding = List.empty();
		List<E> tail = this;
		boolean found = false;
		while (!found && !tail.isEmpty()) {
			final E head = tail.head();
			if (head.equals(element)) {
				found = true;
			} else {
				preceding = preceding.prepend(head);
			}
			tail = tail.tail();
		}
		List<E> result = tail;
		for (E next : preceding) {
			result = result.prepend(next);
		}
		return result;
	}

	/**
	 * Removes all occurrences of the given elements from this List in O(n^2).
	 * <p>
	 * Example: {@code List.of(1,2,3,1,2,3).removeAll(List.of(1,2))} is equal to {@code List.of(3,3)}.
	 * <p>
	 * The result is equivalent to
	 * 
	 * <pre>
	 * <code>if (isEmpty())
	 *     return this;
	 * } else if (elements.contains(head())) {
	 *     return tail().removeAll(elements);
	 * } else {
	 *     return new LinearList(head(), tail().removeAll(elements));
	 * }</code>
	 * </pre>
	 * 
	 * @param elements Elements to be removed.
	 * @return A List containing all of this elements except the given elements.
	 */
	default List<E> removeAll(Iterable<? extends E> elements) {
		@SuppressWarnings("unchecked")
		List<E> removed = (List<E>) List.of(elements);
		List<E> result = List.empty();
		for (E element : this) {
			if (!removed.contains(element)) {
				result = result.prepend(element);
			}
		}
		return result.reverse();
	}

	/**
	 * Keeps all occurrences of the given elements from this List in O(n^2).
	 * <p>
	 * Example: {@code List.of(1,2,3,1,2,3).retainAll(List.of(1,2))} is equal to {@code List.of(1,2,1,2)}.
	 * <p>
	 * The result is equivalent to
	 * 
	 * <pre>
	 * <code>if (isEmpty())
	 *     return this;
	 * } else if (elements.contains(head())) {
	 *     return new LinearList(head(), tail().retainAll(elements));
	 * } else {
	 *     return tail().retainAll(elements);
	 * }</code>
	 * </pre>
	 * 
	 * @param elements Elements to be retained.
	 * @return A List containing all of this elements which are also in the given elements.
	 */
	default List<E> retainAll(Iterable<? extends E> elements) {
		@SuppressWarnings("unchecked")
		List<E> keeped = (List<E>) List.of(elements);
		List<E> result = List.empty();
		for (E element : this) {
			if (keeped.contains(element)) {
				result = result.prepend(element);
			}
		}
		return result.reverse();
	}

	/**
	 * Replaces the first occurrence (if exists) of the given currentElement with newElement in O(2n).
	 * <p>
	 * Example: {@code List.of(1,2,3,2).replace(2,4)} equals {List.of(1,4,3,2)}.
	 * <p>
	 * The result is equivalent to:
	 * {@code isEmpty() ? this : Objects.equals(head(), currentElement) ? new LinearList(newElement, tail()) : new LinearList(head(), tail().replace(currentElement, newElement))}.
	 * 
	 * @param currentElement The element to be replaced.
	 * @param newElement The replacement for currentElement.
	 * @return A List of elements, where the first occurrence (if exists) of currentElement is replaced with newElement.
	 */
	default List<E> replace(E currentElement, E newElement) {
		List<E> preceding = EmptyList.instance();
		List<E> tail = this;
		while (!tail.isEmpty() && !Objects.equals(tail.head(), currentElement)) {
			preceding = preceding.prepend(tail.head());
			tail = tail.tail();
		}
		if (tail.isEmpty()) {
			return this;
		}
		// skip the current head element because it is replaced
		List<E> result = tail.tail().prepend(newElement);
		for (E next : preceding) {
			result = result.prepend(next);
		}
		return result;
	}

	/**
	 * Replaces all occurrences (if any) of the given currentElement with newElement in O(2n).
	 * <p>
	 * Example: {@code List.of(1,2,3,2).replaceAll(2,4)} equals {List.of(1,4,3,4)}.
	 * <p>
	 * The result is equivalent to:
	 * {@code isEmpty() ? this : new LinearList(Objects.equals(head(), currentElement) ? newElement : head(), tail().replaceAll(currentElement, newElement))}.
	 * 
	 * @param currentElement The element to be replaced.
	 * @param newElement The replacement for currentElement.
	 * @return A List of elements, where all occurrences (if any) of currentElement are replaced with newElement.
	 */

	default List<E> replaceAll(E currentElement, E newElement) {
		List<E> result = EmptyList.instance();
		for (List<E> list = this; !list.isEmpty(); list = list.tail()) {
			final E head = list.head();
			final E elem = Objects.equals(head, currentElement) ? newElement : head;
			result = result.prepend(elem);
		}
		return result.reverse();
	}

	/**
	 * Applies an {@link java.util.function.UnaryOperator} to all elements of this List and returns the result as new
	 * List (of same order) in O(2n).
	 * <p>
	 * Example: {@code List.of(1,2,3).replaceAll(i -> i + 1)} equals {List.of(2,3,4)}.
	 * <p>
	 * The result is equivalent to:
	 * {@code isEmpty() ? this : new LinearList(operator.apply(head()), tail().replaceAll(operator))}.
	 * 
	 * @param operator An unary operator.
	 * @return A List of elements transformed by the given operator.
	 */
	default List<E> replaceAll(UnaryOperator<E> operator) {
		List<E> result = EmptyList.instance();
		for (E element : this) {
			result = result.prepend(operator.apply(element));
		}
		return result.reverse();
	}

	/**
	 * Convenience method, well known from java.util collections. It has no effect on the original List, it just returns
	 * EmptyList.instance().
	 * 
	 * @return EmptyList.instance()
	 */
	default List<E> clear() {
		return EmptyList.instance();
	}

	/**
	 * Tests if this List contains a given value as an element in O(n).
	 * <p>
	 * The result is equivalent to {@code indexOf(element) != -1}.
	 * 
	 * @param element An Object of type E, may be null.
	 * @return true, if element is in this List, false otherwise.
	 */
	default boolean contains(E element) {
		return indexOf(element) != -1;
	}

	/**
	 * Tests if this List contains all given values as elements in O(n^2).
	 * <p>
	 * The result is equivalent to
	 * {@code elements.isEmpty() ? true : contains(elements.head()) && containsAll(elements.tail())} but implemented
	 * without recursion.
	 * 
	 * @param elements A List of values of type E.
	 * @return true, if this List contains all given elements, false otherwise.
	 * @throws javaslang.Requirements.UnsatisfiedRequirementException if elements is null
	 */
	default boolean containsAll(Iterable<? extends E> elements) {
		requireNonNull(elements, "elements is null");
		for (E element : elements) {
			if (!this.contains(element)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the index of the given element in O(n). The result is -1, if the element is not contained.
	 * <p>
	 * The result is equivalent to {@code head().equals(element) ? 0 : 1 + tail().indexOf(element)} but implemented
	 * without recursion.
	 * 
	 * @param element An Object of type E, may be null.
	 * @return The index of element or -1.
	 */
	default int indexOf(E element) {
		int index = 0;
		for (List<E> list = this; !list.isEmpty(); list = list.tail(), index++) {
			if (Objects.equals(list.head(), element)) {
				return index;
			}
		}
		return -1;
	}

	/**
	 * Returns the last index of the given element in O(n). The result is -1, if the element is not contained.
	 * <p>
	 * The result is equivalent to {@code (reverse().indexOf(element) == -1) ? -1 : size() - reverse().indexOf(element)}
	 * but implemented without recursion.
	 * 
	 * @param element An Object of type E, may be null.
	 * @return The index of element or -1.
	 */
	default int lastIndexOf(E element) {
		int result = -1, index = 0;
		for (List<E> list = this; !list.isEmpty(); list = list.tail(), index++) {
			if (Objects.equals(list.head(), element)) {
				result = index;
			}
		}
		return result;
	}

	/**
	 * Returns the element of this List at the specified index in O(n).
	 * <p>
	 * The result is roughly equivalent to {@code (index == 0) ? head() : tail().get(index - 1)} but implemented without
	 * recursion.
	 * 
	 * @param index An index, where 0 &lt;= index &lt; size()
	 * @return The element at the specified index.
	 * @throws IndexOutOfBoundsException if this List is empty, index &lt; 0 or index &gt;= size of this List.
	 */
	default E get(int index) {
		if (isEmpty()) {
			throw new IndexOutOfBoundsException("get(" + index + ") on empty list");
		}
		if (index < 0) {
			throw new IndexOutOfBoundsException("get(" + index + ")");
		}
		List<E> list = this;
		for (int i = index - 1; i >= 0; i--) {
			list = list.tail();
			if (list.isEmpty()) {
				throw new IndexOutOfBoundsException(String.format("get(%s) on list of size %s", index, index - i));
			}
		}
		return list.head();
	}

	/**
	 * Replaces the element at the specified index in O(n).
	 * <p>
	 * The result is roughly equivalent to
	 * {@code (index == 0) ? tail().prepend(element) : new LinearList(head(), tail().set(index - 1, element))} but
	 * implemented without recursion.
	 * 
	 * @param index An index, where 0 &lt;= index &lt; size()
	 * @param element A new element.
	 * @return A list containing all of the elements of this List but the given element at the given index.
	 * @throws IndexOutOfBoundsException if this List is empty, index &lt; 0 or index &gt;= size of this List.
	 */
	default List<E> set(int index, E element) {
		if (isEmpty()) {
			throw new IndexOutOfBoundsException("set(" + index + ", e) on empty list");
		}
		if (index < 0) {
			throw new IndexOutOfBoundsException("set(" + index + ", e)");
		}
		List<E> preceding = EmptyList.instance();
		List<E> tail = this;
		for (int i = index; i > 0; i--, tail = tail.tail()) {
			if (tail.isEmpty()) {
				throw new IndexOutOfBoundsException("set(" + index + ", e) on list of size " + size());
			}
			preceding = preceding.prepend(tail.head());
		}
		if (tail.isEmpty()) {
			throw new IndexOutOfBoundsException("set(" + index + ", e) on list of size " + size());
		}
		// skip the current head element because it is replaced
		List<E> result = tail.tail().prepend(element);
		for (E next : preceding) {
			result = result.prepend(next);
		}
		return result;
	}

	/**
	 * Returns a new List which contains all elements starting at beginIndex (inclusive). The sublist is computed in
	 * O(n).
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>{@code List.empty().sublist(0)} returns {@code List.empty()}</li>
	 * <li>{@code List.of(1).sublist(0)} returns {@code List.of(1)}</li>
	 * <li>{@code List.of(1).sublist(1)} returns {@code List.empty()}</li>
	 * <li>{@code List.of(1,2,3).sublist(1)} returns {@code List.of(2,3)}</li>
	 * <li>{@code List.of(1,2,3).sublist(3)} returns {@code List.empty()}</li>
	 * </ul>
	 * <p>
	 * The following calls are illegal:
	 * <ul>
	 * <li>{@code List.empty().sublist(1)} throws</li>
	 * <li>{@code List.of(1,2,3).sublist(-1)} throws}</li>
	 * <li>{@code List.of(1,2,3).sublist(4)} throws}</li>
	 * </ul>
	 * <p>
	 * The result is equivalent to {@code (index == 0) ? this : tail().sublist(index - 1)} but implemented without
	 * recursion.
	 * <p>
	 * If you do not want the bounds to be checked, use the fail-safe variant {@code drop(beginIndex)} instead.
	 * 
	 * @param beginIndex Start index of the sublist, where 0 &lt;= beginIndex &lt;= size()
	 * @return The sublist of the List, starting at beginIndex (inclusive).
	 * @see #drop(int)
	 * @see #take(int)
	 */
	default List<E> sublist(int beginIndex) {
		if (beginIndex < 0) {
			throw new IndexOutOfBoundsException("sublist(" + beginIndex + ")");
		}
		List<E> result = this;
		for (int i = 0; i < beginIndex; i++, result = result.tail()) {
			if (result.isEmpty()) {
				throw new IndexOutOfBoundsException(String.format("sublist(%s) on list of size %s", beginIndex, i));
			}
		}
		return result;
	}

	/**
	 * Returns a new List which contains the elements from beginIndex (inclusive) to endIndex (exclusive) of this List.
	 * The sublist is computed in O(2n).
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>{@code List.empty().sublist(0,0)} returns {@code List.empty()}</li>
	 * <li>{@code List.of(1).sublist(0,0)} returns {@code List.empty()}</li>
	 * <li>{@code List.of(1).sublist(0,1)} returns {@code List.of(1)}</li>
	 * <li>{@code List.of(1).sublist(1,1)} returns {@code List.empty()}</li>
	 * <li>{@code List.of(1,2,3).sublist(1,3)} returns {@code List.of(2,3)}</li>
	 * <li>{@code List.of(1,2,3).sublist(3,3)} returns {@code List.empty()}</li>
	 * </ul>
	 * <p>
	 * The following calls are illegal:
	 * <ul>
	 * <li>{@code List.of(1,2,3).sublist(1,0)} throws}</li>
	 * <li>{@code List.of(1,2,3).sublist(-1,2)} throws}</li>
	 * <li>{@code List.of(1,2,3).sublist(1,4)} throws}</li>
	 * </ul>
	 * <p>
	 * The result is equivalent to
	 * {@code (beginIndex == 0) ? reverse().sublist(size() - endIndex).reverse() : tail().sublist(beginIndex - 1, endIndex)}
	 * but implemented without recursion.
	 * <p>
	 * If you do not want the bounds to be checked, use the fail-safe variant
	 * {@code drop(beginIndex).take(endIndex - beginIndex)} instead.
	 * 
	 * @param beginIndex Start index of the sublist, where 0 &lt;= beginIndex &lt;= size()
	 * @param endIndex End index of the sublist, where beginIndex &lt;= endIndex &lt;= size()
	 * @return The sublist of the List, starting at beginIndex (inclusive) and ending at endIndex (exclusive).
	 * @see #drop(int)
	 * @see #take(int)
	 */
	default List<E> sublist(int beginIndex, int endIndex) {
		if (beginIndex < 0 || endIndex - beginIndex < 0) {
			throw new IndexOutOfBoundsException(String.format("sublist(%s, %s) on list of size %s", beginIndex,
					endIndex, size()));
		}
		List<E> result = EmptyList.instance();
		List<E> list = this;
		for (int i = 0; i < endIndex; i++, list = list.tail()) {
			if (list.isEmpty()) {
				throw new IndexOutOfBoundsException(String.format("sublist(%s, %s) on list of size %s", beginIndex,
						endIndex, i));
			}
			if (i >= beginIndex) {
				result = result.prepend(list.head());
			}
		}
		return result.reverse();
	}

	/**
	 * Drops the first n elements of this list or the whole list, if this size &lt; n. The elements are dropped in O(n).
	 * <p>
	 * The result is equivalent to {@code sublist(n)} but does not throw if n &lt; 0 or n &gt; size(). In the case of n
	 * &lt; 0 this List is returned, in the case of n &gt; size() the EmptyList is returned.
	 * 
	 * @param n The number of elements to drop.
	 * @return A list consisting of all elements of this list except the first n ones, or else the empty list, if this
	 *         list has less than n elements.
	 */
	default List<E> drop(int n) {
		List<E> result = this;
		for (int i = 0; i < n && !result.isEmpty(); i++, result = result.tail())
			;
		return result;
	}

	/**
	 * Takes the first n elements of this list or the whole list, if this size &lt; n. The elements are taken in O(n).
	 * <p>
	 * The result is equivalent to {@code sublist(0, n)} but does not throw if n &lt; 0 or n &gt; size(). In the case of
	 * n &lt; 0 the EmptyList is returned, in the case of n &gt; size() this List is returned.
	 * 
	 * @param n The number of elements to take.
	 * @return A list consisting of the first n elements of this list or the whole list, if it has less than n elements.
	 */
	default List<E> take(int n) {
		List<E> result = EmptyList.instance();
		List<E> list = this;
		for (int i = 0; i < n && !list.isEmpty(); i++, list = list.tail()) {
			result = result.prepend(list.head());
		}
		return result.reverse();
	}

	/**
	 * Returns a List formed from this List and another Iterable collection by combining corresponding elements in
	 * pairs. If one of the two collections is longer than the other, its remaining elements are ignored.
	 * 
	 * @param <T> The type of the second half of the returned pairs.
	 * @param that The Iterable providing the second half of each result pair.
	 * @return a new List containing pairs consisting of corresponding elements of this list and that. The length of the
	 *         returned collection is the minimum of the lengths of this List and that.
	 * @throws UnsatisfiedRequirementException if that is null.
	 */
	default <T> List<Tuple2<E, T>> zip(Iterable<T> that) {
		requireNonNull(that, "that is null");
		List<Tuple2<E, T>> result = EmptyList.instance();
		List<E> list1 = this;
		Iterator<T> list2 = that.iterator();
		while (!list1.isEmpty() && list2.hasNext()) {
			result = result.prepend(Tuples.of(list1.head(), list2.next()));
			list1 = list1.tail();
		}
		return result.reverse();
	}

	/**
	 * Returns a List formed from this List and another Iterable collection by combining corresponding elements in
	 * pairs. If one of the two collections is shorter than the other, placeholder elements are used to extend the
	 * shorter collection to the length of the longer.
	 * 
	 * @param <T> The type of the second half of the returned pairs.
	 * @param that The Iterable providing the second half of each result pair.
	 * @param thisElem The element to be used to fill up the result if this List is shorter than that.
	 * @param thatElem The element to be used to fill up the result if that is shorter than this List.
	 * @return A new List containing pairs consisting of corresponding elements of this List and that. The length of the
	 *         returned collection is the maximum of the lengths of this List and that. If this List is shorter than
	 *         that, thisElem values are used to pad the result. If that is shorter than this List, thatElem values are
	 *         used to pad the result.
	 * @throws UnsatisfiedRequirementException if that is null.
	 */
	default <T> List<Tuple2<E, T>> zipAll(Iterable<T> that, E thisElem, T thatElem) {
		requireNonNull(that, "that is null");
		List<Tuple2<E, T>> result = EmptyList.instance();
		List<E> list1 = this;
		Iterator<T> list2 = that.iterator();
		while (!(list1.isEmpty() && !list2.hasNext())) {
			final E elem1;
			if (list1.isEmpty()) {
				elem1 = thisElem;
			} else {
				elem1 = list1.head();
				list1 = list1.tail();
			}
			final T elem2 = list2.hasNext() ? list2.next() : thatElem;
			result = result.prepend(Tuples.of(elem1, elem2));
		}
		return result.reverse();
	}

	/**
	 * Zips this List with its indices.
	 * 
	 * @return A new List containing all elements of this List paired with their index, starting with 0.
	 */
	default List<Tuple2<E, Integer>> zipWithIndex() {
		List<Tuple2<E, Integer>> result = EmptyList.instance();
		int index = 0;
		for (List<E> list = this; !list.isEmpty(); list = list.tail()) {
			result = result.prepend(Tuples.of(list.head(), index++));
		}
		return result.reverse();
	}

	/**
	 * Returns an array containing all elements of this List in the same order. The array is created in O(2n).
	 * 
	 * @return The elements of this List as array.
	 */
	default Object[] toArray() {
		final Object[] result = new Object[size()];
		int i = 0;
		for (List<E> list = this; !list.isEmpty(); list = list.tail(), i++) {
			result[i] = list.head();
		}
		return result;
	}

	/**
	 * Returns the given array filled with this elements in the same order or a new Array containing this elements, if
	 * array.length &lt; size(). This takes O(2n).
	 * <p>
	 * According to {@link java.util.ArrayList#toArray(Object[])}, the element in the array immediately following the
	 * end of the List is set to null.
	 * 
	 * @param array An Array to be filled with this elements.
	 * @return The given array containing this elements or a new one if array.length &lt; size().
	 */
	default E[] toArray(E[] array) {
		return toArrayList().toArray(array);
	}

	/**
	 * Converts this List into an {@link java.util.ArrayList} which is mutable.
	 * 
	 * @return An ArrayList of the same size, containing this elements.
	 */
	default java.util.ArrayList<E> toArrayList() {
		final java.util.ArrayList<E> result = new java.util.ArrayList<>();
		for (E element : this) {
			result.add(element);
		}
		return result;
	}

	/**
	 * Sorts the elements of this List according to their natural order.
	 * <p>
	 * This call is equivalent to {@code stream().sorted().collect(List.collector())}.
	 * 
	 * @return An ordered List.
	 */
	default List<E> sort() {
		return stream().sorted().collect(List.collector());
	}

	/**
	 * Sorts the elements of this List according to the provided {@link java.util.Comparator}.
	 * <p>
	 * This call is equivalent to {@code stream().sorted(c).collect(List.collector())}.
	 * 
	 * @param c An element Comparator.
	 * @return An ordered List.
	 */
	default List<E> sort(Comparator<? super E> c) {
		return stream().sorted(c).collect(List.collector());
	}

	/**
	 * Returns a sequential {@link java.util.stream.Stream} representation of this List.
	 * <p>
	 * This call is equivalent to {@code StreamSupport.stream(spliterator(), false)}.
	 * 
	 * @return A sequential Stream of elements of this List.
	 */
	default Stream<E> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * Returns a parallel {@link java.util.stream.Stream} representation of this List.
	 * <p>
	 * This call is equivalent to {@code StreamSupport.stream(spliterator(), true)}.
	 * 
	 * @return A parallel Stream of elements of this List.
	 */
	default Stream<E> parallelStream() {
		return StreamSupport.stream(spliterator(), true);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#spliterator()
	 */
	@Override
	default Spliterator<E> spliterator() {
		return Spliterators.spliterator(iterator(), size(), Spliterator.ORDERED | Spliterator.IMMUTABLE);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	default Iterator<E> iterator() {

		final class ListIterator implements Iterator<E> {

			List<E> list;

			ListIterator(List<E> list) {
				requireNonNull(list, "list is null");
				this.list = list;
			}

			@Override
			public boolean hasNext() {
				return !list.isEmpty();
			}

			@Override
			public E next() {
				if (list.isEmpty()) {
					throw new NoSuchElementException();
				} else {
					final E result = list.head();
					list = list.tail();
					return result;
				}
			}
		}

		return new ListIterator(this);
	}

	/**
	 * Shortcut for {@code sublist(index).iterator()}.
	 * 
	 * @param index The start index of the iterator.
	 * @return An iterator, starting at the given index.
	 */
	default Iterator<E> iterator(int index) {
		return sublist(index).iterator();
	}

	/**
	 * Equivalent to {@link java.util.List#equals(Object)}.
	 */
	@Override
	boolean equals(Object o);

	/**
	 * Equivalent to {@link java.util.List#hashCode()}.
	 */
	@Override
	int hashCode();

	/**
	 * Returns a String representation of this List.
	 * <p>
	 * If this is EmptyList, {@code "()"} is returned.
	 * <p>
	 * If this is an LinearList containing the elements e1, ..., en, then {@code "(" + Strings.toString(e1)
	 * + ", " + ... + ", " + Strings.toString(en) + ")"} is returned.
	 * 
	 * @return This List as String.
	 */
	@Override
	String toString();

	/**
	 * Returns the single instance of EmptyList. Convenience method for {@code EmptyList.instance()} .
	 * 
	 * @param <T> Component type of EmptyList, determined by type inference in the particular context.
	 * @return The empty list.
	 */
	static <T> List<T> empty() {
		return EmptyList.instance();
	}

	/**
	 * Creates a List of the given elements.
	 * 
	 * <pre>
	 * <code>  List.of(1, 2, 3, 4)
	 * = EmptyList.instance().prepend(4).prepend(3).prepend(2).prepend(1)
	 * = new LinearList(1, new LinearList(2, new LinearList(3, new LinearList(4, EmptyList.instance()))))</code>
	 * </pre>
	 *
	 * @param <T> Component type of the List.
	 * @param elements Zero or more elements.
	 * @return A list containing the given elements in the same order.
	 */
	@SafeVarargs
	static <T> List<T> of(T... elements) {
		List<T> result = EmptyList.instance();
		for (int i = elements.length - 1; i >= 0; i--) {
			result = result.prepend(elements[i]);
		}
		return result;
	}

	/**
	 * Creates a List of the given elements.
	 * 
	 * @param <T> Component type of the List.
	 * @param elements An Iterable of elements.
	 * @return A list containing the given elements in the same order.
	 */
	static <T> List<T> of(Iterable<T> elements) {
		if (elements instanceof List) {
			return (List<T>) elements;
		} else {
			List<T> result = EmptyList.instance();
			for (T element : elements) {
				result = result.prepend(element);
			}
			return result.reverse();
		}
	}

	/**
	 * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
	 * {@link Stream#collect(Collector)} to obtain a {@link javaslang.collection.List}.
	 * 
	 * @param <T> Component type of the List.
	 * @return A List Collector.
	 */
	static <T> Collector<T, ArrayList<T>, List<T>> collector() {
		final Supplier<ArrayList<T>> supplier = ArrayList::new;
		final BiConsumer<ArrayList<T>, T> accumulator = ArrayList::add;
		final BinaryOperator<ArrayList<T>> combiner = (left, right) -> {
			left.addAll(right);
			return left;
		};
		final Function<ArrayList<T>, List<T>> finisher = elements -> {
			List<T> result = EmptyList.instance();
			for (T element : elements) {
				result = result.prepend(element);
			}
			return result.reverse();
		};
		return Collector.of(supplier, accumulator, combiner, finisher);
	}

	/**
	 * This class is needed because the interface {@link List} cannot use default methods to override Object's non-final
	 * methods equals, hashCode and toString.
	 * <p>
	 * See <a href="http://mail.openjdk.java.net/pipermail/lambda-dev/2013-March/008435.html">Allow default methods to
	 * override Object's methods</a>.
	 *
	 * @param <E> Component type of the List.
	 */
	static abstract class AbstractList<E> implements List<E> {

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof List)) {
				return false;
			} else {
				List<?> list1 = this;
				List<?> list2 = (List<?>) o;
				while (!list1.isEmpty() && !list2.isEmpty()) {
					final boolean isEqual = Objects.equals(list1.head(), list2.head());
					if (!isEqual) {
						return false;
					}
					list1 = list1.tail();
					list2 = list2.tail();
				}
				return list1.isEmpty() && list2.isEmpty();
			}
		}

		@Override
		public int hashCode() {
			int hashCode = 1;
			for (List<E> list = this; !list.isEmpty(); list = list.tail()) {
				final E element = list.head();
				hashCode = 31 * hashCode + Objects.hashCode(element);
			}
			return hashCode;
		}

		@Override
		public String toString() {
			return stream().map(Strings::toString).collect(joining(", ", "List(", ")"));
		}

	}

}
