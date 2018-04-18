package com.mantledillusion.vaadin.cotton.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mantledillusion.data.epiphy.index.PropertyIndex;
import com.mantledillusion.data.epiphy.interfaces.ListedProperty;
import com.mantledillusion.data.epiphy.interfaces.ReadableProperty;
import com.mantledillusion.injection.hura.Blueprint;
import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.injection.hura.Predefinable.Singleton;
import com.mantledillusion.injection.hura.annotation.Inject;
import com.mantledillusion.injection.hura.annotation.Inject.SingletonMode;
import com.mantledillusion.vaadin.cotton.WebEnv;
import com.mantledillusion.vaadin.cotton.component.ComponentFactory;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.model.BindableGrid.BindableTableSelectionEvent.SelectionEventType;
import com.vaadin.data.HasValue;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.selection.MultiSelectionEvent;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.TextField;

/**
 * {@link Grid} extension that is bound to a {@link ListedProperty}, so its
 * table cells display that {@link ListedProperty} element's
 * {@link ReadableProperty}s.
 *
 * @param <RowType>
 *            The item type that represents a row in the grid.
 * @param <ModelType>
 *            The root type of the data model the {@link BindableGrid}'s item
 *            list is contained in.
 */
public final class BindableGrid<RowType, ModelType> extends Composite {

	private static final long serialVersionUID = 1L;

	private static final String TABLE_DATA_SOURCE_SINGLETON_ID = "tableDataSource";

	private final class BindableHasValue implements HasValue<List<RowType>> {

		private static final long serialVersionUID = 1L;

		private boolean isRequiredIndicatorVisible = false;
		private boolean isReadOnly = false;

		@Override
		public List<RowType> getValue() {
			return BindableGrid.this.itemCollection.stream().map(wrapper -> wrapper.item).collect(Collectors.toList());
		}

		@Override
		public void setValue(List<RowType> value) {
			IdentityHashMap<RowType, Integer> newRegistry = new IdentityHashMap<>();
			List<RowWrapper> newCollection = new ArrayList<>();

			if (value != null) {
				int i = 0;
				RowWrapper wrapper;
				for (RowType item : value) {
					if (BindableGrid.this.itemRegistry.containsKey(item)) {
						wrapper = BindableGrid.this.itemCollection.get(BindableGrid.this.itemRegistry.get(item));
						BindableGrid.this.itemRegistry.remove(item);
					} else {
						wrapper = injectItem(item, i);
					}

					newRegistry.put(item, i);
					newCollection.add(i, wrapper);
					i++;
				}
			}

			Set<Integer> selectIndeces = getSelectedIndeces();
			Set<RowType> abandoned = new HashSet<>();
			for (Integer destoryableIndex : BindableGrid.this.itemRegistry.values()) {
				RowType destroyed = destroyItem(BindableGrid.this.itemCollection.get(destoryableIndex));
				if (selectIndeces.contains(destoryableIndex)) {
					abandoned.add(destroyed);
				}
			}

			BindableGrid.this.itemRegistry.clear();
			BindableGrid.this.itemRegistry.putAll(newRegistry);
			BindableGrid.this.itemCollection.clear();
			BindableGrid.this.itemCollection.addAll(newCollection);

			BindableGrid.this.dataProvider.refreshAll();

			if (!abandoned.isEmpty()) {
				fireSelectionEvent(new BindableTableSelectionEvent<RowType>(BindableGrid.this, abandoned));
			}
		}

		private RowWrapper injectItem(RowType item, int index) {
			IndexContext context = IndexContext.of(PropertyIndex.of(BindableGrid.this.listedProperty, index));

			@SuppressWarnings("unchecked")
			RowAccessor<ModelType> accessor = BindableGrid.this.injector
					.instantiate(Blueprint.of(RowAccessor.class, context.asSingleton()));

			return new RowWrapper(item, accessor);
		}

		private RowType destroyItem(RowWrapper wrapper) {
			BindableGrid.this.injector.destroy(wrapper.accessor);
			return wrapper.item;
		}

		@Override
		public Registration addValueChangeListener(ValueChangeListener<List<RowType>> listener) {
			return () -> {
			}; // The given listener is not added anywhere, so the registration can do nothing.
		}

		@Override
		public boolean isRequiredIndicatorVisible() {
			return this.isRequiredIndicatorVisible;
		}

		@Override
		public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
			this.isRequiredIndicatorVisible = requiredIndicatorVisible;
		}

		@Override
		public boolean isReadOnly() {
			return this.isReadOnly;
		}

		@Override
		public void setReadOnly(boolean readOnly) {
			this.isReadOnly = readOnly;
			BindableGrid.this.itemCollection.parallelStream()
					.forEach(wrapper -> wrapper.columnComponents.values().parallelStream().forEach(component -> {
						if (component instanceof HasValue) {
							((HasValue<?>) component).setReadOnly(readOnly);
						}
					}));
		}
	}

	private abstract static class Wrapper<T> {
		final T item;

		private Wrapper(T item) {
			this.item = item;
		}
	}

	private final class RowWrapper extends Wrapper<RowType> {
		final RowAccessor<ModelType> accessor;
		final IdentityHashMap<String, Component> columnComponents = new IdentityHashMap<>();

		public RowWrapper(RowType item, RowAccessor<ModelType> accessor) {
			super(item);
			this.accessor = accessor;
		}
	}

	/**
	 * An {@link ModelAccessor} implementation that is indexed for one specific row
	 * in a {@link BindableGrid}.
	 *
	 * @param <ModelType>
	 *            The root type of the data model the {@link RowAccessor}'s row
	 *            item's list is contained in.
	 */
	public static final class RowAccessor<ModelType> extends ModelAccessor<ModelType> {

		private RowAccessor(
				@Inject(value = TABLE_DATA_SOURCE_SINGLETON_ID, singletonMode = SingletonMode.GLOBAL) ModelProxy<ModelType> parentContainer) {
			super(parentContainer);
		}
	}

	/**
	 * Interface for a grid column that creates a {@link Component} for every row in
	 * the column, or put differently, for every table cell.
	 * <P>
	 * A single {@link PropertyColumn} instance can be added to multiple
	 * {@link BindableGrid}s without any problem, as the raw implementation is
	 * stateless and the configuration is done separately for every time the column
	 * is added to a {@link BindableGrid}.
	 *
	 * @param <ModelType>
	 *            The root type of the data model the {@link PropertyColumn}'s row
	 *            item's list is contained in.
	 */
	public interface PropertyColumn<ModelType> {

		/**
		 * Returns a {@link Component} that will represent a table cell in the column,
		 * hence this method is called for every row.
		 * <P>
		 * Access to the data is provided by the given {@link RowAccessor}; normally, it
		 * should be used to build and bind a {@link Component} that displays data of a
		 * sub-{@link ReadableProperty} of the grid row item's {@link ReadableProperty}.
		 * <P>
		 * The {@link RowAccessor} is automatically indexed for its row.
		 * <P>
		 * For example: if ITEMLIST is the list property, ITEM the property of that
		 * list's elements and ITEM_STRING_FIELD a specific property of a single ITEM,
		 * the given {@link RowAccessor} can be used to bind a {@link TextField} to
		 * ITEM_STRING_FIELD to display that {@link String}'s content. Alternatively,
		 * the {@link RowAccessor} could be used to get data of several properties
		 * inside the item and combine them in a {@link Component} build by
		 * {@link ComponentFactory} to meet a more unique specification; just note that
		 * in this case, there is no binding.
		 * <P>
		 * NOTE: This method is <b>called once per lifetime of an item inside the
		 * table</b>, so it can not be used in hope that it will be called again after
		 * some item data has changed.
		 * 
		 * @param rowAccessor
		 *            A {@link RowAccessor} indexed for a single list item that is used
		 *            on all {@link PropertyColumn}s to display the content of the
		 *            single row it is indexed for; might <b>not</b> be null.
		 * @return A {@link Component} that displays a single table cell's content;
		 *         might be null
		 */
		public Component createBoundComponent(RowAccessor<ModelType> rowAccessor);
	}

	/**
	 * A configuration that can be used any time an {@link PropertyColumn}'s
	 * relation to a {@link BindableGrid} has to be changed.
	 * <P>
	 * NOTE: If the same {@link PropertyColumn} instance is also added to a
	 * different {@link BindableGrid}, changes made to this
	 * {@link PropertyColumnConfiguration} do <b>not</b> have any effect on the
	 * configuration of the column in the other {@link BindableGrid}.
	 * 
	 * @param <TableRowType>
	 *            The item type that represents a row in the grid.
	 */
	public static final class PropertyColumnConfiguration<TableRowType> {

		private final Column<? extends Wrapper<TableRowType>, Component> column;

		private PropertyColumnConfiguration(Column<? extends Wrapper<TableRowType>, Component> column) {
			this.column = column;
		}

		/**
		 * Sets the column's caption to the given one.
		 * <P>
		 * Use of a message id is allowed here to use auto-localization via
		 * {@link WebEnv}.
		 * 
		 * @param captionMsgId
		 *            The caption (or localizable caption message id) to set; might be
		 *            null.
		 */
		public void setCaption(String captionMsgId) {
			this.column.setCaption(WebEnv.localize(captionMsgId));
		}

		/**
		 * Sets the column hidable by the user.
		 * 
		 * @param hidable
		 *            Whether the column should be hidable or not. Default is false.
		 */
		public void setHidable(boolean hidable) {
			this.column.setHidable(hidable);
		}

		/**
		 * Hides the column.
		 * 
		 * @param hidden
		 *            Whether the column should be hidden or not. Default is false.
		 */
		public void setHidden(boolean hidden) {
			this.column.setHidden(hidden);
		}

		/**
		 * Sets the caption to use on the hiding toggle to the given one if hidable=true
		 * and hidden=true.
		 * <P>
		 * Use of a message id is allowed here to use auto-localization via
		 * {@link WebEnv}.
		 * 
		 * @param hidingToggleCaptionMsgId
		 *            The caption (or localizable caption message id) to set.
		 */
		public void setHidingToggleCaption(String hidingToggleCaptionMsgId) {
			this.column.setHidingToggleCaption(WebEnv.localize(hidingToggleCaptionMsgId));
		}

		/**
		 * Sets the width of the column to the given exact pixel value, which actively
		 * forbids it to resize.
		 * 
		 * @param widthPx
		 *            The pixel value to set the width to.
		 */
		public void setWidth(int widthPx) {
			this.column.setWidth(widthPx);
		}

		/**
		 * Sets the width of the column to undefined, which allows it to resize
		 * autonomously.
		 */
		public void setWidthUndefined() {
			this.column.setWidthUndefined();
		}

		/**
		 * Sets the expand ratio of the column when compared to other columns. Has
		 * effect on how the column is resized compared to others when there is
		 * less/more space than the column's natural size.
		 * <P>
		 * NOTE: This only has effect if the column's width is undefined.
		 * 
		 * @param expandRatio
		 *            The expand ratio to set.
		 */
		public void setExpandRatio(int expandRatio) {
			this.column.setExpandRatio(expandRatio);
		}

		/**
		 * Sets the minimum width the column will not shrink under.
		 * <P>
		 * NOTE: This only has effect if the column's width is undefined.
		 * 
		 * @param widthPx
		 *            The pixel value to minimally shrink to.
		 */
		public void setMinimumWidth(int widthPx) {
			this.column.setMinimumWidth(widthPx);
		}

		/**
		 * Instructs the column to use its content as the minimal width to consider when
		 * shrinking.
		 * <P>
		 * NOTE: This only has effect if the column's width is undefined.
		 * 
		 * @param minimumWidthFromContent
		 *            Whether the column should consider its content when shrinking.
		 *            Default is true.
		 */
		public void setMinimumWidthFromContent(boolean minimumWidthFromContent) {
			this.column.setMinimumWidthFromContent(minimumWidthFromContent);
		}

		/**
		 * Sets the maximum width the column will not grow over.
		 * <P>
		 * NOTE: This only has effect if the column's width is undefined.
		 * 
		 * @param widthPx
		 *            The pixel value to maximally grow to.
		 */
		public void setMaximumWidth(int widthPx) {
			this.column.setMaximumWidth(widthPx);
		}

		/**
		 * Sets the column resizable by the user.
		 * 
		 * @param resizable
		 *            Whether the column should be resizable or not. Default is true.
		 */
		public void setResizable(boolean resizable) {
			this.column.setResizable(resizable);
		}

		/**
		 * Sets the column sortable by the user.
		 * 
		 * @param sortable
		 *            Whether the column should be sortable or not. Default is true.
		 */
		public void setSortable(boolean sortable) {
			this.column.setSortable(sortable);
		}

		/**
		 * Sets the {@link Comparator} to use when the column is sorted.
		 * 
		 * @param comparator
		 *            The comparator to use; might <b>not</b> be null.
		 */
		public void setSortComparator(Comparator<TableRowType> comparator) {
			if (comparator == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR, "Cannot set a null comparator.");
			}

			this.column.setComparator((wrapper, wrapper2) -> comparator.compare(wrapper.item, wrapper2.item));
		}
	}

	/**
	 * Interface for configurators of {@link PropertyColumn}s that handle a
	 * {@link PropertyColumnConfiguration} coming in during the columns registration
	 * on a {@link BindableGrid}.
	 *
	 * @param <TableRowType>
	 *            The item type that represents a row in the grid.
	 */
	public interface PropertyColumnConfigurator<TableRowType> {

		/**
		 * Is called when this {@link PropertyColumnConfigurator} is given to a
		 * {@link BindableGrid} along with an {@link PropertyColumn} to add to the grid.
		 * 
		 * @param config
		 *            The configuration this {@link PropertyColumnConfigurator} may work
		 *            with.
		 */
		public void configure(PropertyColumnConfiguration<TableRowType> config);
	}

	private final ModelBinder<ModelType> parent;
	private final ListedProperty<ModelType, RowType> listedProperty;
	private final BindableHasValue hasValue = new BindableHasValue();

	private final Injector injector;

	private final IdentityHashMap<RowType, Integer> itemRegistry = new IdentityHashMap<>();
	private final List<RowWrapper> itemCollection = new ArrayList<>();
	private final ListDataProvider<RowWrapper> dataProvider = new ListDataProvider<>(this.itemCollection);
	private final IdentityHashMap<PropertyColumn<ModelType>, String> columnRegistry = new IdentityHashMap<>();
	private final Grid<RowWrapper> grid = new Grid<>(this.dataProvider);

	private final SelectionListener<RowWrapper> gridSelectionListener = new MultiModeSelectionListener();
	private final Set<BindableTableSelectionEventListener<RowType>> selectionListeners = new HashSet<>();

	BindableGrid(ModelBinder<ModelType> parent, ListedProperty<ModelType, RowType> listedProperty) {
		if (parent == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"The parent binder of a bindable table cannot be null.");
		}

		this.parent = parent;
		this.listedProperty = listedProperty;

		this.injector = Injector.of(Singleton.of(TABLE_DATA_SOURCE_SINGLETON_ID, this.parent));

		setCompositionRoot(this.grid);

		this.grid.addSelectionListener(gridSelectionListener);
	}

	HasValue<List<RowType>> getBindable() {
		return this.hasValue;
	}

	// ######################################################################################################################################
	// ############################################################# COLUMNS ################################################################
	// ######################################################################################################################################

	private int columnCounter = 0;

	/**
	 * Adds a column to this {@link BindableGrid} that is bound to the given
	 * property.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to bind.
	 * @param property
	 *            The property to bind the column to; might <b>not</b> be null.
	 * @return A {@link PropertyColumnConfiguration} to configure the registration
	 *         of the given column in this grid with.
	 */
	public <PropertyType> PropertyColumnConfiguration<RowType> addColumn(
			ReadableProperty<ModelType, PropertyType> property) {
		return addColumn(rowAccessor -> rowAccessor.bindLabelForProperty(property));
	}

	/**
	 * Adds a column to this {@link BindableGrid} that is bound to the given
	 * property.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to bind.
	 * @param property
	 *            The property to bind the column to; might <b>not</b> be null.
	 * @param renderer
	 *            The {@link StringRenderer} to render the bound property with;
	 *            might <b>not</b> be null.
	 * @return A {@link PropertyColumnConfiguration} to configure the registration
	 *         of the given column in this grid with.
	 */
	public <PropertyType> PropertyColumnConfiguration<RowType> addColumn(
			ReadableProperty<ModelType, PropertyType> property, StringRenderer<PropertyType> renderer) {
		return addColumn(rowAccessor -> rowAccessor.bindLabelForProperty(property, renderer));
	}

	/**
	 * Adds a column to this {@link BindableGrid} that is bound to the given
	 * property.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to bind.
	 * @param property
	 *            property The property to bind the column to; might <b>not</b> be
	 *            null.
	 * @param renderer
	 *            The {@link StringRenderer} to render the bound property with;
	 *            might <b>not</b> be null.
	 * @param configurator
	 *            The configurator to use on the given columns registration; might
	 *            be null.
	 * @return A {@link PropertyColumnConfiguration} to configure the registration
	 *         of the given column in this grid with.
	 */
	public <PropertyType> PropertyColumnConfiguration<RowType> addColumn(
			ReadableProperty<ModelType, PropertyType> property, StringRenderer<PropertyType> renderer,
			PropertyColumnConfigurator<RowType> configurator) {
		return addColumn(rowAccessor -> rowAccessor.bindLabelForProperty(property, renderer), configurator);
	}

	/**
	 * Adds the given {@link PropertyColumn} to this {@link BindableGrid}.
	 * 
	 * @param column
	 *            The column to add; might <b>not</b> be null, might not be already
	 *            added.
	 * @return A {@link PropertyColumnConfiguration} to configure the registration
	 *         of the given column in this grid with.
	 */
	public PropertyColumnConfiguration<RowType> addColumn(PropertyColumn<ModelType> column) {
		if (column == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"A column to add to a table can never be null.");
		}

		final String columnId = String.valueOf(columnCounter++);
		this.columnRegistry.put(column, columnId);

		if (this.grid.getColumn(columnId) != null) {
			throw new WebException(HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR,
					"The given column instance is already added to the grid");
		}

		Column<RowWrapper, Component> gridColumn = BindableGrid.this.grid
				.addComponentColumn(new ValueProvider<RowWrapper, Component>() {

					private static final long serialVersionUID = 1L;

					@Override
					public final Component apply(RowWrapper source) {
						if (!source.columnComponents.containsKey(columnId)) {
							Component boundComponent = column.createBoundComponent(source.accessor);
							if (boundComponent instanceof HasValue) {
								((HasValue<?>) boundComponent).setReadOnly(BindableGrid.this.hasValue.isReadOnly);
								((HasValue<?>) boundComponent).setRequiredIndicatorVisible(
										BindableGrid.this.hasValue.isRequiredIndicatorVisible);
							}
							source.columnComponents.put(columnId, boundComponent);
						}
						return source.columnComponents.get(columnId);
					}
				});

		gridColumn.setId(columnId);

		return new PropertyColumnConfiguration<RowType>(gridColumn);
	}

	/**
	 * Adds the given {@link PropertyColumn} to this {@link BindableGrid}.
	 * <P>
	 * Also passes the generated {@link PropertyColumnConfiguration} to the given
	 * {@link PropertyColumnConfigurator}.
	 * <P>
	 * This method makes it possible for a column to be self-configuring, by using a
	 * type that implements {@link PropertyColumn} and
	 * {@link PropertyColumnConfigurator} and then call this method with an instance
	 * of that type as both arguments.
	 * 
	 * @param column
	 *            The column to add; might <b>not</b> be null, might not be already
	 *            added.
	 * @param configurator
	 *            The configurator to use on the given columns registration; might
	 *            be null.
	 * @return A {@link PropertyColumnConfiguration} to configure the registration
	 *         of the given column in this grid with; never null
	 */
	public PropertyColumnConfiguration<RowType> addColumn(PropertyColumn<ModelType> column,
			PropertyColumnConfigurator<RowType> configurator) {
		PropertyColumnConfiguration<RowType> config = addColumn(column);
		if (configurator != null) {
			configurator.configure(config);
		}
		return config;
	}

	/**
	 * Removes the given column from this {@link BindableGrid}.
	 * 
	 * @param column
	 *            The column to remove; might <b>not</b> be null.
	 */
	public void removeColumn(PropertyColumn<ModelType> column) {
		if (column == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"A column to remove from a table can never be null.");
		}
		String columnId = this.columnRegistry.remove(column);
		this.grid.removeColumn(columnId);
	}

	// ######################################################################################################################################
	// ########################################################## CONFIGURATION #############################################################
	// ######################################################################################################################################

	private final class MultiModeSelectionListener implements SelectionListener<RowWrapper> {

		private static final long serialVersionUID = 1L;

		@Override
		public void selectionChange(SelectionEvent<RowWrapper> event) {
			Set<RowType> deselected;
			Set<RowType> selected;
			if (event instanceof SingleSelectionEvent) {
				SingleSelectionEvent<RowWrapper> ssEvent = (SingleSelectionEvent<RowWrapper>) event;
				deselected = ssEvent.getOldValue() != null ? Collections.singleton(ssEvent.getOldValue().item)
						: Collections.emptySet();
				selected = ssEvent.getSelectedItem().isPresent()
						? Collections.singleton(ssEvent.getSelectedItem().get().item)
						: Collections.emptySet();
			} else {
				MultiSelectionEvent<RowWrapper> msEvent = (MultiSelectionEvent<RowWrapper>) event;
				deselected = msEvent.getRemovedSelection().stream().map((wrapper) -> wrapper.item)
						.collect(Collectors.toSet());
				selected = msEvent.getAddedSelection().stream().map((wrapper) -> wrapper.item)
						.collect(Collectors.toSet());
			}

			SelectionEventType eventType;
			if (selected.isEmpty()) {
				eventType = SelectionEventType.DESELECTION;
			} else if (deselected.isEmpty()) {
				eventType = SelectionEventType.SELECTION;
			} else {
				eventType = SelectionEventType.SWITCH;
			}

			fireSelectionEvent(new BindableTableSelectionEvent<>(BindableGrid.this, eventType, selected, deselected));
		}
	}

	/**
	 * Specifies the selection mode a {@link BindableGrid} offers.
	 */
	public static enum SelectionMode {

		/**
		 * Selection is turned off.
		 */
		NO_SELECTION(com.vaadin.ui.Grid.SelectionMode.NONE),

		/**
		 * A single item might be selected, but also none.
		 */
		SINGLE_SELECTION(com.vaadin.ui.Grid.SelectionMode.SINGLE),

		/**
		 * Multiple items might be selected, but also none.
		 */
		MULTI_SELECTION(com.vaadin.ui.Grid.SelectionMode.MULTI);

		private final com.vaadin.ui.Grid.SelectionMode gridMode;

		private SelectionMode(com.vaadin.ui.Grid.SelectionMode gridMode) {
			this.gridMode = gridMode;
		}
	}

	/**
	 * Sets the selection mode to the given one.
	 * 
	 * @param mode
	 *            The mode to set; might <b>not</b> be null.
	 */
	public void setSelectionMode(SelectionMode mode) {
		if (mode == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR, "Cannot set a null selection mode.");
		}

		this.grid.setSelectionMode(mode.gridMode);
		this.grid.addSelectionListener(this.gridSelectionListener);
	}

	// ######################################################################################################################################
	// ######################################################### ITEM SELECTION #############################################################
	// ######################################################################################################################################

	/**
	 * Returns the index of the currently selected item.
	 * <P>
	 * Convenience method for {@link SelectionMode}.SINGLE_SELECTION; if used in
	 * MULTI_SELECTION mode, the index of the first item is returned.
	 * 
	 * @return The index of the (first) selected item, or null, if no item is
	 *         selected.
	 */
	public Integer getSelectedIndex() {
		for (RowWrapper wrapper : this.grid.getSelectedItems()) {
			return this.itemRegistry.get(wrapper.item);
		}
		return null;
	}

	/**
	 * Returns the indeces of the currently selected items.
	 * 
	 * @return The {@link Set} of selected indeces; might be empty if no item is
	 *         selected.
	 */
	public Set<Integer> getSelectedIndeces() {
		Set<Integer> selected = new HashSet<>();
		for (RowWrapper wrapper : this.grid.getSelectedItems()) {
			selected.add(this.itemRegistry.get(wrapper.item));
		}
		return Collections.unmodifiableSet(selected);
	}

	/**
	 * Returns the currently selected item.
	 * <P>
	 * Convenience method for {@link SelectionMode}.SINGLE_SELECTION; if used in
	 * MULTI_SELECTION mode, the first selected item is returned.
	 * 
	 * @return The (first) selected item, or null, if no item is selected.
	 */
	public RowType getSelectedItem() {
		for (RowWrapper wrapper : this.grid.getSelectedItems()) {
			return wrapper.item;
		}
		return null;
	}

	/**
	 * Returns the currently selected items.
	 * 
	 * @return The {@link List} of selected items; might be empty if no item is
	 *         selected.
	 */
	public List<RowType> getSelectedItems() {
		List<RowType> selected = new ArrayList<>();
		for (RowWrapper wrapper : this.grid.getSelectedItems()) {
			selected.add(wrapper.item);
		}
		return Collections.unmodifiableList(selected);
	}

	/**
	 * Selects the item at the given index.
	 * 
	 * @param index
	 *            The index whose item to select.
	 */
	public void selectIndex(int index) {
		this.grid.select(this.itemCollection.get(index));
	}

	/**
	 * Selects the given item.
	 * 
	 * @param item
	 *            The item to select.
	 */
	public void selectItem(RowType item) {
		this.grid.select(this.itemCollection.get(this.itemRegistry.get(item)));
	}

	/**
	 * Deselects the item at the given index.
	 * 
	 * @param index
	 *            The index whose item to deselect.
	 */
	public void deselectIndex(int index) {
		this.grid.deselect(this.itemCollection.get(index));
	}

	/**
	 * Deselects the given item.
	 * 
	 * @param item
	 *            The item to deselect.
	 */
	public void deselectItem(RowType item) {
		this.grid.deselect(this.itemCollection.get(this.itemRegistry.get(item)));
	}

	/**
	 * Deselects all selected items.
	 */
	public void deselectAll() {
		this.grid.deselectAll();
	}

	// ######################################################################################################################################
	// ############################################################## EVENT #################################################################
	// ######################################################################################################################################

	private static abstract class BindableTableEvent extends EventObject {

		private static final long serialVersionUID = 1L;

		private BindableTableEvent(BindableGrid<?, ?> source) {
			super(source);
		}
	}

	// SELECTION

	/**
	 * Event that occurs on selection changes.
	 */
	public static final class BindableTableSelectionEvent<RowType> extends BindableTableEvent {

		private static final long serialVersionUID = 1L;

		/**
		 * Describes the type of {@link BindableTableSelectionEvent}.
		 */
		public static enum SelectionEventType {

			/**
			 * Some items were selected, none were deselected.
			 */
			SELECTION,

			/**
			 * Some items were deselected, none were selected.
			 */
			DESELECTION,

			/**
			 * Some items were selected while others were deselected simultaneously.
			 */
			SWITCH,

			/**
			 * Some items that were selected in the past are removed from the table and thus
			 * lost selection.
			 */
			ABANDON;
		}

		private final SelectionEventType eventType;
		private final Set<RowType> selected;
		private final Set<RowType> deselected;
		private final Set<RowType> abandoned;

		private BindableTableSelectionEvent(BindableGrid<RowType, ?> source, SelectionEventType eventType,
				Set<RowType> selected, Set<RowType> deselected) {
			super(source);
			this.eventType = eventType;
			this.selected = selected;
			this.deselected = deselected;
			this.abandoned = Collections.emptySet();
		}

		private BindableTableSelectionEvent(BindableGrid<RowType, ?> source, Set<RowType> abandoned) {
			super(source);
			this.eventType = SelectionEventType.ABANDON;
			this.selected = Collections.emptySet();
			this.deselected = Collections.emptySet();
			this.abandoned = abandoned;
		}

		/**
		 * Returns the items whose selection is the reason for this
		 * {@link BindableTableSelectionEvent} to occur.
		 * 
		 * @return The newly selected items; might be empty.
		 */
		public Set<RowType> getSelected() {
			return this.selected;
		}

		/**
		 * Returns the items whose deselection is the reason for this
		 * {@link BindableTableSelectionEvent} to occur.
		 * 
		 * @return The now deselected items; might be empty.
		 */
		public Set<RowType> getDeselected() {
			return this.deselected;
		}

		/**
		 * Returns the removed items whose depleted selection is the reason for this
		 * {@link BindableTableSelectionEvent} to occur.
		 * 
		 * @return The abandoned items; might be empty.
		 */
		public Set<RowType> getAbandoned() {
			return abandoned;
		}

		/**
		 * Determines the type of the selection.
		 * <P>
		 * For example, if the table is in {@link SelectionMode}.SINGLE_SELECTION, the
		 * types could occur as follows:<br>
		 * - On the first item selection, it is {@link SelectionEventType}.SELECTION<br>
		 * - On the selection of a different item, it is
		 * {@link SelectionEventType}.SWITCH<br>
		 * - On the deselection of the selected item, it is
		 * {@link SelectionEventType}.DESELECTION<br>
		 * - On removal of the selected item, it is
		 * {@link SelectionEventType}.ABANDON<br>
		 * 
		 * @return The {@link SelectionEventType} of this event.
		 */
		public SelectionEventType getSelectionEventType() {
			return eventType;
		}
	}

	/**
	 * Listener for {@link BindableTableSelectionEvent}s.
	 */
	public interface BindableTableSelectionEventListener<RowType> {

		/**
		 * Has to handle a occurring {@link BindableTableSelectionEvent}.
		 * 
		 * @param event
		 *            The occurring event.
		 */
		public void onSelectionChange(BindableTableSelectionEvent<RowType> event);
	}

	private void fireSelectionEvent(BindableTableSelectionEvent<RowType> event) {
		for (BindableTableSelectionEventListener<RowType> listener : this.selectionListeners) {
			listener.onSelectionChange(event);
		}
		fireEvent(event);
	}

	/**
	 * Adds the given {@link BindableTableSelectionEventListener} to listen for
	 * {@link BindableTableSelectionEvent}s.
	 * 
	 * @param listener
	 *            The listener to add; might <b>not</b> be null.
	 */
	public void addSelectionListener(BindableTableSelectionEventListener<RowType> listener) {
		if (listener == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR, "Cannot add a null listener.");
		}

		this.selectionListeners.add(listener);
	}

	/**
	 * Remove the given {@link BindableTableSelectionEventListener}.
	 * 
	 * @param listener
	 *            The listener to remove.
	 */
	public void removeSelectionListener(BindableTableSelectionEventListener<RowType> listener) {
		this.selectionListeners.remove(listener);
	}
}
