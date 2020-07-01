package de.fraunhofer.aisec.cpg.processing;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A visitor that uses a {@code IStrategy} to traverse a visitable structure and applies an {@code IAction} to each node.
 *
 * @param <V> Type of nodes.
 */
public abstract class AbstractVisitor<V extends IVisitable> implements IVisitor<V> {
	@NonNull protected IStrategy<V> iterator;
	@NonNull protected final IAction<V> action;

	public AbstractVisitor(@NonNull IAction<V> action, @NonNull IStrategy<V> iterator) {
		this.action = action;
		this.iterator = iterator;
	}
}
