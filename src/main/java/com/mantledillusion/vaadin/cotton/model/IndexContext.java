package com.mantledillusion.vaadin.cotton.model;

import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.data.epiphy.ModelPropertyList;
import com.mantledillusion.data.epiphy.index.PropertyIndex;
import com.mantledillusion.injection.hura.Blueprint.TypedBlueprint;
import com.mantledillusion.injection.hura.Predefinable.Singleton;
import com.mantledillusion.injection.hura.annotation.Context;

/**
 * {@link IndexContext} implementation that is used in
 * {@link ModelAccessor}s to allow indexed access to their parent.
 * <P>
 * Basically, {@link IndexContext}s hold
 * {@link ModelPropertyList}-&gt;{@link Integer} mappings; see the
 * documentation of {@link IndexContext} for reference.
 * <p>
 * To supply an instance of {@link IndexContext} to
 * {@link ModelAccessor}s, it has to be given to them during their
 * injection. Use {@link IndexContext#asSingleton()} to retrieve a singleton
 * with a specific singletonId and give the returned {@link Singleton} instance
 * to the {@link TypedBlueprint} the {@link ModelAccessor}s are injected
 * with.
 */
@Context
public final class IndexContext implements com.mantledillusion.data.epiphy.index.IndexContext {

	public static final String SINGLETON_ID = "_IndexContext";

	public static final IndexContext EMPTY = new IndexContext();

	private final Map<ModelPropertyList<?, ?>, Integer> indices;

	private IndexContext() {
		this(new IdentityHashMap<>());
	}

	private IndexContext(IdentityHashMap<ModelPropertyList<?, ?>, Integer> indices) {
		this.indices = indices;
	}

	@Override
	public Integer indexOf(ModelProperty<?, ?> listedProperty) {
		return this.indices.get(listedProperty);
	}

	@Override
	public boolean contains(ModelProperty<?, ?> listedProperty) {
		return this.indices.containsKey(listedProperty);
	}

	/**
	 * Returns whether this {@link IndexContext} completely contains all the indices
	 * of the given {@link IndexContext}, or in other words, whether the given
	 * context is a subset of this context.
	 * 
	 * @param other
	 *            The context whose indices are checked for; might be null.
	 * @return True if the given context is completely contained by this
	 *         {@link IndexContext}, false otherwise
	 */
	public boolean contains(IndexContext other) {
		if (other != null) {
			for (ModelPropertyList<?, ?> listedProperty : other.indices.keySet()) {
				if (!this.indices.containsKey(listedProperty)
						|| !this.indices.get(listedProperty).equals(other.indices.get(listedProperty))) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Gathers the indices of all of the given properties and builds a new
	 * {@link IndexContext} out of them.
	 * <P>
	 * Speaking in set theory, this operation is an intersection.
	 * <P>
	 * Properties that are not indexed by this {@link IndexContext} are ignored,
	 * which causes the created context to be a subset of this context.
	 * <P>
	 * That being said, the method {@link #contains(IndexContext)} called on this
	 * {@link IndexContext} with the returned context as the parameter would return
	 * true.
	 * 
	 * @param <M>
	 *            The model type of the given properties.
	 * @param properties
	 *            The properties whose indices to gather; might be null or contain
	 *            null as an element.
	 * @return A new sub context containing indices for the given properties that
	 *         were indexed by this context; never null
	 */
	public <M> IndexContext intersection(Set<ModelProperty<M, ?>> properties) {
		IdentityHashMap<ModelPropertyList<?, ?>, Integer> indices = new IdentityHashMap<>();
		if (properties != null) {
			for (ModelProperty<?, ?> property : properties) {
				if (this.indices.containsKey(property)) {
					indices.put((ModelPropertyList<?, ?>) property, this.indices.get(property));
				}
			}
		}
		return new IndexContext(indices);
	}

	/**
	 * Creates an extension of this {@link IndexContext} with the indices of this
	 * context extended by the given ones.
	 * <P>
	 * Speaking in set theory, this operation is an union.
	 * <P>
	 * That being said, the method {@link #contains(IndexContext)} called on the
	 * returned context with this {@link IndexContext} as the parameter would return
	 * true.
	 * 
	 * @param indices
	 *            The indices to add to the set of this {@link IndexContext}s'
	 *            indices for creating the extended context; might be null or
	 *            contain null values.
	 * @return The extended {@link IndexContext}; never null
	 */
	public IndexContext union(PropertyIndex... indices) {
		IdentityHashMap<ModelPropertyList<?, ?>, Integer> newIndices = new IdentityHashMap<>(this.indices);
		if (indices != null) {
			for (PropertyIndex index : indices) {
				if (index != null) {
					newIndices.put(index.getProperty(), index.getIndex());
				}
			}
		}
		return new IndexContext(newIndices);
	}

	/**
	 * Creates an extension of this {@link IndexContext} with the indices of this
	 * context extended by the ones of the given context.
	 * <P>
	 * Speaking in set theory, this operation is an union.
	 * <P>
	 * That being said, the method {@link #contains(IndexContext)} called on the
	 * returned context with the given {@link IndexContext} as the parameter would
	 * return true.
	 * <P>
	 * Note that if this {@link IndexContext} contains an index for the same
	 * property as the given context does, the given context's index will be taken,
	 * which is why the method {@link #contains(IndexContext)} called on the
	 * returned context with this {@link IndexContext} as the parameter would not
	 * necessarily return true.
	 * 
	 * @param other
	 *            The context's indices to add to the set of this
	 *            {@link IndexContext}s' indices for creating the extended context;
	 *            might be null.
	 * @return The extended {@link IndexContext}; never null
	 */
	public IndexContext union(IndexContext other) {
		IdentityHashMap<ModelPropertyList<?, ?>, Integer> indices = new IdentityHashMap<>(this.indices);
		if (other != null) {
			indices.putAll(other.indices);
		}
		return new IndexContext(indices);
	}

	/**
	 * Applies an index update to this indices of this {@link IndexContext},
	 * creating an updated one.
	 * <P>
	 * Note that the given parameters represent the <B>update</B> on an index,
	 * <b>not</b> the value to set. If the given property is not indexed by this
	 * context or the index in this context is smaller than the given base index,
	 * nothing will be changed.
	 * <P>
	 * For example, if this context contains the index (A-&gt;5) for the property A,
	 * an update (A,7,+1) would change nothing while an update (A,2,-2) would change
	 * the index to (A-&gt;3).
	 * 
	 * @param property
	 *            The property to update the index for; might be null.
	 * @param baseIndex
	 *            The base index from which an update will be taking place. An
	 *            update of the index in this {@link IndexContext} will only be done
	 *            if the index contained by this index for the given property is
	 *            &gt;= this base index.
	 * @param modification
	 *            The modification amount.
	 * @return The updated {@link IndexContext}; never null
	 */
	public IndexContext update(ModelProperty<?, ?> property, int baseIndex, int modification) {
		IdentityHashMap<ModelPropertyList<?, ?>, Integer> indices = new IdentityHashMap<>(this.indices);
		if (property != null && indices.containsKey(property) && indices.get(property) >= baseIndex) {
			indices.put((ModelPropertyList<?, ?>) property, indices.get(property) + modification);
		}
		return new IndexContext(indices);
	}

	/**
	 * Creates a {@link Singleton} instance containing this {@link IndexContext}
	 * with a special singletonId that is meant for {@link ModelAccessor}s.
	 * 
	 * @return A new {@link IndexContext} {@link Singleton}; never null
	 */
	public Singleton asSingleton() {
		return Singleton.of(SINGLETON_ID, this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((indices == null) ? 0 : indices.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndexContext other = (IndexContext) obj;
		if (indices == null) {
			if (other.indices != null)
				return false;
		} else if (!indices.equals(other.indices))
			return false;
		return true;
	}

	/**
	 * Builder {@link Method} for {@link IndexContext}s.
	 * 
	 * @param indices
	 *            {@link PropertyIndex} instances to build an {@link IndexContext}
	 *            from; might be null or contain null as a value.
	 * @return A new {@link IndexContext} containing the given
	 *         {@link PropertyIndex}es; never null
	 */
	public static IndexContext of(PropertyIndex... indices) {
		if (indices == null || indices.length == 0) {
			return EMPTY;
		} else {
			IndexContext context = new IndexContext();
			for (PropertyIndex index : indices) {
				if (index != null) {
					context.indices.put(index.getProperty(), index.getIndex());
				}
			}
			return context;
		}
	}
}
