/**    / \____  _    ______   _____ / \____   ____  _____
 *    /  \__  \/ \  / \__  \ /  __//  \__  \ /    \/ __  \   Javaslang
 *  _/  // _\  \  \/  / _\  \\_  \/  // _\  \  /\  \__/  /   Copyright 2014 Daniel Dietrich
 * /___/ \_____/\____/\_____/____/\___\_____/_/  \_/____/    Licensed under the Apache License, Version 2.0
 */
package javaslang.match;

import java.util.function.DoubleFunction;
import java.util.function.IntFunction;
import java.util.function.LongFunction;

import javaslang.Tuples.Tuple;
import javaslang.lambda.Functions.Function1;
import javaslang.lambda.Functions.Function2;
import javaslang.match.Match.BooleanFunction;
import javaslang.match.Match.ByteFunction;
import javaslang.match.Match.CharFunction;
import javaslang.match.Match.FloatFunction;
import javaslang.match.Match.ShortFunction;

/**
 * Extension methods for {@link Match}.
 */
public final class Matchs {

	/**
	 * This class is not intended to be instantiated.
	 */
	private Matchs() {
		throw new AssertionError(Matchs.class.getName() + " is not intended to be instantiated.");
	}

	/**
	 * Shortcut for {@code new Match.Builder<R>().caze(function)}.
	 * 
	 * @param <R> return type of the matcher function
	 * @param function A function which is applied to a matched object.
	 * @return A Match of type T
	 */
	public static <R> Match.Builder<R> caze(Function1<?, R> function) {
		return new Match.Builder<R>().caze(function);
	}

	/**
	 * Shortcut for {@code new Match.Builder<R>().caze(prototype, function)}.
	 * 
	 * @param <T> type of the prototype object
	 * @param <R> return type of the matcher function
	 * @param prototype An object which matches by equality.
	 * @param function A function which is applied to a matched object.
	 * @return A Match of type T
	 */
	public static <T, R> Match.Builder<R> caze(T prototype, Function1<T, R> function) {
		return new Match.Builder<R>().caze(prototype, function);
	}

	public static <T, D extends Tuple, R> Match.Builder<R> caze(Pattern<T, ?, D> pattern, Function2<T, D, R> function) {
		return new Match.Builder<R>().caze(pattern, function);
	}

	public static <R> Match.Builder<R> caze(BooleanFunction<R> function) {
		return new Match.Builder<R>().caze(function);
	}

	public static <R> Match.Builder<R> caze(ByteFunction<R> function) {
		return new Match.Builder<R>().caze(function);
	}

	public static <R> Match.Builder<R> caze(CharFunction<R> function) {
		return new Match.Builder<R>().caze(function);
	}

	public static <R> Match.Builder<R> caze(DoubleFunction<R> function) {
		return new Match.Builder<R>().caze(function);
	}

	public static <R> Match.Builder<R> caze(FloatFunction<R> function) {
		return new Match.Builder<R>().caze(function);
	}

	public static <R> Match.Builder<R> caze(IntFunction<R> function) {
		return new Match.Builder<R>().caze(function);
	}

	public static <R> Match.Builder<R> caze(LongFunction<R> function) {
		return new Match.Builder<R>().caze(function);
	}

	public static <R> Match.Builder<R> caze(ShortFunction<R> function) {
		return new Match.Builder<R>().caze(function);
	}

}
