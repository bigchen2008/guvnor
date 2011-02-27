/*
 * Copyright 2011 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.drools.guvnor.client.widgets.decoratedgrid;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.drools.guvnor.client.resources.DecisionTableResources;
import org.drools.guvnor.client.resources.DecisionTableResources.DecisionTableStyle;
import org.drools.guvnor.client.widgets.decoratedgrid.MergableGridWidget.CellExtents;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;

/**
 * Abstract grid, decorated with DecoratedGridHeaderWidget and
 * DecoratedGridSidebarWidget encapsulating basic operation: keyboard navigation
 * and column resizing.
 * 
 * @param <T>
 *            The type of domain columns represented by the Grid
 */
public abstract class DecoratedGridWidget<T> extends Composite {

    // Widgets for UI
    protected Panel                               mainPanel;
    protected Panel                               bodyPanel;
    protected ScrollPanel                         scrollPanel;
    protected MergableGridWidget<T>               gridWidget;
    protected DecoratedGridHeaderWidget<T>        headerWidget;
    protected DecoratedGridSidebarWidget<T>       sidebarWidget;
    protected HasSystemControlledColumns          hasSystemControlledColumns;

    protected int                                 height;
    protected int                                 width;

    // Resources
    protected static final DecisionTableResources resource = GWT.create( DecisionTableResources.class );
    protected static final DecisionTableStyle     style    = resource.cellTableStyle();

    /**
     * Construct at empty DecoratedGridWidget, without DecoratedGridHeaderWidget
     * or DecoratedGridSidebarWidget These should be set before the grid is
     * displayed using setHeaderWidget and setSidebarWidget respectively.
     */
    public DecoratedGridWidget() {

        mainPanel = getMainPanel();
        bodyPanel = getBodyPanel();
        gridWidget = getGridWidget();
        if ( mainPanel == null ) {
            throw new IllegalArgumentException( "mainPanel cannot be null" );
        }
        if ( bodyPanel == null ) {
            throw new IllegalArgumentException( "bodyPanel cannot be null" );
        }
        if ( gridWidget == null ) {
            throw new IllegalArgumentException( "gridWidget cannot be null" );
        }

        scrollPanel = new ScrollPanel();
        scrollPanel.add( gridWidget );
        scrollPanel.addScrollHandler( getScrollHandler() );

        initWidget( mainPanel );

        //Add handler for when the selected cell changes
        gridWidget.addSelectedCellChangeHandler( new SelectedCellChangeHandler() {

            public void onSelectedCellChange(SelectedCellChangeEvent event) {
                cellSelected( event.getCellExtents() );
            }

        } );

    }

    /**
     * Append a column to the end of the column list
     * 
     * @param column
     * @param columnData
     * @param bRedraw
     *            Redraw the grid after the column has been appended
     */
    public void appendColumn(DynamicColumn<T> column,
                             List<CellValue< ? >> columnData,
                             boolean bRedraw) {
        DynamicData data = gridWidget.getData();
        if ( column == null ) {
            throw new IllegalArgumentException(
                                                "Column cannot be null." );
        }
        if ( columnData == null ) {
            throw new IllegalArgumentException( "columnData cannot be null" );
        }
        if ( columnData.size() != data.size() ) {
            throw new IllegalArgumentException( "columnData contains a different number of rows to the grid" );
        }
        insertColumnBefore( null,
                            column,
                            columnData,
                            bRedraw );
    }

    /**
     * Append a row to the end of the grid
     * 
     * @param row
     */
    public void appendRow(DynamicDataRow row) {
        if ( row == null ) {
            throw new IllegalArgumentException( "row cannot be null" );
        }
        gridWidget.clearSelection();
        insertRowBefore( null,
                         row );
    }

    /**
     * Resize the DecoratedGridHeaderWidget and DecoratedGridSidebarWidget when
     * DecoratedGridWidget shows scrollbars
     */
    public void assertDimensions() {
        headerWidget.setWidth( scrollPanel.getElement().getClientWidth()
                               + "px" );
        sidebarWidget.setHeight( scrollPanel.getElement().getClientHeight()
                                 + "px" );
    }

    /**
     * Delete the given column
     * 
     * @param column
     */
    public void deleteColumn(DynamicColumn<T> column) {
        if ( column == null ) {
            throw new IllegalArgumentException(
                                                "Column cannot be null." );
        }
        deleteColumn( column,
                      true );
    }

    /**
     * Delete the given row
     * 
     * @param row
     */
    public void deleteRow(DynamicDataRow row) {
        if ( row == null ) {
            throw new IllegalArgumentException( "row cannot be null" );
        }
        DynamicData data = gridWidget.getData();
        int index = data.indexOf( row );
        if ( index == -1 ) {
            throw new IllegalArgumentException(
                                                "DynamicDataRow does not exist in table data." );
        }
        gridWidget.clearSelection();

        data.remove( index );
        sidebarWidget.deleteSelector( index );

        // Partial redraw
        if ( !gridWidget.isMerged() ) {
            // Single row when not merged
            gridWidget.deleteRow( index );
            gridWidget.assertModelIndexes();
        } else {
            // Affected rows when merged
            gridWidget.deleteRow( index );

            if ( data.size() > 0 ) {
                gridWidget.assertModelMerging();
                int minRedrawRow = findMinRedrawRow( index - 1 );
                int maxRedrawRow = findMaxRedrawRow( index - 1 ) + 1;
                if ( maxRedrawRow > data.size() - 1 ) {
                    maxRedrawRow = data.size() - 1;
                }
                gridWidget.redrawRows( minRedrawRow,
                                       maxRedrawRow );
            }
        }

        assertDimensions();

    }

    /**
     * Get the DecoratedGridWidget inner panel to which the
     * DecoratedGridHeaderWidget will be added. This allows subclasses to have
     * some control over the internal layout of the grid.
     * 
     * @return
     */
    public abstract Panel getBodyPanel();

    /**
     * Return the Widget responsible for rendering the DecoratedGridWidget
     * "grid".
     * 
     * @return
     */
    public abstract MergableGridWidget<T> getGridWidget();

    /**
     * Return the Widget responsible for rendering the DecoratedGridWidget
     * "header".
     * 
     * @return
     */
    public DecoratedGridHeaderWidget<T> getHeaderWidget() {
        return headerWidget;
    }

    /**
     * Return the DecoratedGridWidget outer most panel to which all child
     * widgets is added. This allows subclasses to have some control over the
     * internal layout of the grid.
     * 
     * @return
     */
    public abstract Panel getMainPanel();

    /**
     * Return the ScrollPanel in which the DecoratedGridWidget "grid" is nested.
     * This allows ScrollEvents to be hooked up to other defendant controls
     * (e.g. the Header).
     * 
     * @return
     */
    public abstract ScrollHandler getScrollHandler();

    /**
     * Return the Widget responsible for rendering the DecoratedGridWidget
     * "sidebar".
     * 
     * @return
     */
    public DecoratedGridSidebarWidget<T> getSidebarWidget() {
       return sidebarWidget;
    }

    /**
     * Insert a column before that specified
     * 
     * @param columnBefore
     * @param newColumn
     * @param columnData
     * @param bRedraw
     *            Redraw the grid after the column has been inserted
     */
    public void insertColumnBefore(DynamicColumn<T> columnBefore,
                                   DynamicColumn<T> newColumn,
                                   List<CellValue< ? >> columnData,
                                   boolean bRedraw) {

        final DynamicData data = gridWidget.getData();
        final List<DynamicColumn<T>> columns = gridWidget.getColumns();

        int index = columns.size();
        if ( columnBefore != null ) {
            index = columns.indexOf( columnBefore );
            if ( index == -1 ) {
                throw new IllegalArgumentException(
                                                    "columnBefore does not exist in table data." );
            }
            index++;
        }
        if ( newColumn == null ) {
            throw new IllegalArgumentException( "newColumn cannot be null" );
        }
        if ( columnData == null ) {
            throw new IllegalArgumentException( "columnData cannot be null" );
        }
        if ( columnData.size() != data.size() ) {
            throw new IllegalArgumentException( "columnData contains a different number of rows to the grid" );
        }

        // Add column definition
        columns.add( index,
                     newColumn );
        reindexColumns();

        // Add column data
        for ( int iRow = 0; iRow < columnData.size(); iRow++ ) {
            CellValue< ? > cv = columnData.get( iRow );
            data.get( iRow ).add( index,
                                  cv );
        }
        gridWidget.assertModelIndexes();

        // Redraw
        if ( bRedraw ) {
            gridWidget.redrawColumns( index,
                                      columns.size() - 1 );
            headerWidget.redraw();
            assertDimensions();
        }

    }

    /**
     * Insert a row before that specified
     * 
     * @param rowBefore
     * @param newRow
     */
    public void insertRowBefore(DynamicDataRow rowBefore,
                                DynamicDataRow newRow) {

        final DynamicData data = gridWidget.getData();
        final List<DynamicColumn<T>> columns = gridWidget.getColumns();

        int index = data.size();
        if ( rowBefore != null ) {
            index = data.indexOf( rowBefore );
            if ( index == -1 ) {
                throw new IllegalArgumentException(
                                                    "rowBefore does not exist in table data." );
            }
        }
        if ( newRow == null ) {
            throw new IllegalArgumentException( "newRow cannot be null" );
        }
        if ( newRow.size() != columns.size() ) {
            throw new IllegalArgumentException( "newRow contains a different number of columns to the grid" );
        }

        // Find rows that need to be (re)drawn
        int minRedrawRow = index;
        int maxRedrawRow = index;
        if ( gridWidget.isMerged() ) {
            if ( index < data.size() ) {
                minRedrawRow = findMinRedrawRow( index );
                maxRedrawRow = findMaxRedrawRow( index ) + 1;
            } else {
                minRedrawRow = findMinRedrawRow( (index > 0 ? index - 1 : index) );
                maxRedrawRow = index;
            }
        }

        data.add( index,
                  newRow );
        sidebarWidget.insertSelectorBefore( newRow,
                                                      index );

        // Partial redraw
        if ( !gridWidget.isMerged() ) {
            // Only new row when not merged
            gridWidget.assertModelIndexes();
            gridWidget.insertRowBefore( index,
                                        newRow );
        } else {
            // Affected rows when merged
            gridWidget.assertModelMerging();

            // This row is overwritten by the call to redrawRows()
            gridWidget.insertRowBefore( index,
                                        newRow );
            gridWidget.redrawRows( minRedrawRow,
                                   maxRedrawRow );
        }

        assertDimensions();

    }

    /**
     * Redraw any columns that have their values programmatically manipulated
     */
    public void redrawSystemControlledColumns() {
        final List<DynamicColumn<T>> columns = gridWidget.getColumns();
        for ( DynamicColumn< ? > col : columns ) {
            if ( col.isSystemControlled() ) {
                gridWidget.redrawColumn( col.getColumnIndex() );
            }
        }
    }

    /**
     * Set the visibility of a column
     * 
     * @param index
     *            The index of the column to hide
     * @param isVisible
     *            true if the column is to be visible
     */
    public void setColumnVisibility(int index,
                                    boolean isVisible) {

        final List<DynamicColumn<T>> columns = gridWidget.getColumns();

        if ( index < 0
             || index > columns.size() ) {
            throw new IllegalArgumentException(
                                                "Column index must be greater than zero and less than then number of declared columns." );
        }

        if ( isVisible
             && !columns.get( index ).isVisible() ) {
            columns.get( index ).setVisible( isVisible );
            gridWidget.assertModelIndexes();
            gridWidget.showColumn( index );
            headerWidget.redraw();
        } else if ( !isVisible
                    && columns.get( index ).isVisible() ) {
            columns.get( index ).setVisible( isVisible );
            gridWidget.assertModelIndexes();
            gridWidget.hideColumn( index );
            headerWidget.redraw();
        }
    }

    /**
     * Some implementations may require the values of cells within the
     * DecoratedGridWidget to be programmatically manipulated (such as
     * "Row Number", which has to be recalculated after a sort operation). Such
     * implementations can register themselves here to receive requests to
     * update cell values when necessary (currently only after a sort).
     * 
     * @param hasSystemControlledColumns
     */
    public void setHasSystemControlledColumns(HasSystemControlledColumns hasSystemControlledColumns) {
        this.hasSystemControlledColumns = hasSystemControlledColumns;
    }

    /**
     * Set the "Header" for the DecoratedGridWidget and perform any
     * initialisation, such as registering event handlers.
     * 
     * @param headerWidget
     */
    public abstract void setHeaderWidget(DecoratedGridHeaderWidget<T> headerWidget);

    /**
     * This should be used instead of setHeight(String) and setWidth(String) as
     * various child Widgets of the DecisionTable need to have their sizes set
     * relative to the outermost Widget (i.e. this).
     */
    @Override
    public void setPixelSize(int width,
                             int height) {
        if ( width < 0 ) {
            throw new IllegalArgumentException( "width cannot be less than zero" );
        }
        if ( height < 0 ) {
            throw new IllegalArgumentException( "height cannot be less than zero" );
        }
        super.setPixelSize( width,
                            height );
        this.height = height;
        setHeight( height );
        setWidth( width );
    }

    /**
     * Set the "Sidebar" for the DecoratedGridWidget and perform any
     * initialisation, such as registering event handlers.
     * 
     * @param sidebarWidget
     */
    public abstract void setSidebarWidget(final DecoratedGridSidebarWidget<T> sidebarWidget);

    /**
     * Sort data based upon information stored in Columns
     */
    public void sort() {

        final DynamicData data = gridWidget.getData();
        final List<DynamicColumn<T>> columns = gridWidget.getColumns();

        final DynamicColumn< ? >[] sortOrderList = new DynamicColumn[columns.size()];
        int index = 0;
        for ( DynamicColumn<T> column : columns ) {
            int sortIndex = column.getSortIndex();
            if ( sortIndex != -1 ) {
                sortOrderList[sortIndex] = column;
                index++;
            }
        }
        final int sortedColumnCount = index;

        Collections.sort( data,
                          new Comparator<DynamicDataRow>() {

                              @SuppressWarnings({"rawtypes", "unchecked"})
                              public int compare(DynamicDataRow leftRow,
                                                 DynamicDataRow rightRow) {
                                  int comparison = 0;
                                  for ( int index = 0; index < sortedColumnCount; index++ ) {
                                      DynamicColumn sortableHeader = sortOrderList[index];
                                      Comparable leftColumnValue = leftRow.get( sortableHeader
                                              .getColumnIndex() );
                                      Comparable rightColumnValue = rightRow.get( sortableHeader
                                              .getColumnIndex() );
                                      comparison = (leftColumnValue == rightColumnValue) ? 0
                                          : (leftColumnValue == null) ? -1
                                              : (rightColumnValue == null) ? 1
                                                  : leftColumnValue
                                                          .compareTo( rightColumnValue );
                                      if ( comparison != 0 ) {
                                          switch ( sortableHeader.getSortDirection() ) {
                                              case ASCENDING :
                            break;
                        case DESCENDING :
                            comparison = -comparison;
                            break;
                        default :
                            throw new IllegalStateException(
                                                             "Sorting can only be enabled for ASCENDING or"
                                                                     + " DESCENDING, not sortDirection ("
                                                                     + sortableHeader.getSortDirection()
                                                                     + ") ." );
                    }
                    return comparison;
                }
            }
            return comparison;
        }
                          } );

        gridWidget.assertModelMerging();

        // Request dependent children update cell values accordingly
        if ( hasSystemControlledColumns != null ) {
            hasSystemControlledColumns.updateSystemControlledColumnValues();
        }
        gridWidget.redraw();
        sidebarWidget.redraw();

    }

    //Ensure the selected cell is visible
    private void cellSelected(CellExtents ce) {

        //Left extent
        if ( ce.getOffsetX() < scrollPanel.getHorizontalScrollPosition() ) {
            scrollPanel.setHorizontalScrollPosition( ce.getOffsetX() );
        }

        //Right extent
        int scrollWidth = scrollPanel.getElement().getClientWidth();
        if ( ce.getOffsetX() + ce.getWidth() > scrollWidth + scrollPanel.getHorizontalScrollPosition() ) {
            int delta = ce.getOffsetX() + ce.getWidth() - scrollPanel.getHorizontalScrollPosition() - scrollWidth;
            scrollPanel.setHorizontalScrollPosition( scrollPanel.getHorizontalScrollPosition() + delta );
        }

        //Top extent
        if ( ce.getOffsetY() < scrollPanel.getScrollPosition() ) {
            scrollPanel.setScrollPosition( ce.getOffsetY() );
        }

        //Bottom extent
        int scrollHeight = scrollPanel.getElement().getClientHeight();
        if ( ce.getOffsetY() + ce.getHeight() > scrollHeight + scrollPanel.getScrollPosition() ) {
            int delta = ce.getOffsetY() + ce.getHeight() - scrollPanel.getScrollPosition() - scrollHeight;
            scrollPanel.setScrollPosition( scrollPanel.getScrollPosition() + delta );
        }

    }

    // Delete column from table with optional redraw
    private void deleteColumn(DynamicColumn<T> column,
                              boolean bRedraw) {

        final DynamicData data = gridWidget.getData();
        final List<DynamicColumn<T>> columns = gridWidget.getColumns();

        // Lookup UI column
        int index = columns.indexOf( column );
        if ( index == -1 ) {
            throw new IllegalArgumentException(
                                                "Column not found in declared columns." );
        }

        // Clear any selections
        gridWidget.clearSelection();

        // Delete column data
        for ( int iRow = 0; iRow < data.size(); iRow++ ) {
            DynamicDataRow row = data.get( iRow );
            row.remove( index );
        }

        // Delete column from grid
        columns.remove( index );
        reindexColumns();

        // Redraw
        if ( bRedraw ) {
            gridWidget.assertModelIndexes();
            gridWidget.redraw();
            headerWidget.redraw();
        }

    }

    // Re-index columns
    private void reindexColumns() {
        final List<DynamicColumn<T>> columns = gridWidget.getColumns();
        for ( int iCol = 0; iCol < columns.size(); iCol++ ) {
            DynamicColumn<T> col = columns.get( iCol );
            col.setColumnIndex( iCol );
        }
    }

    // Set height of outer most Widget and related children
    private void setHeight(final int height) {
        mainPanel.setHeight( height
                             + "px" );

        // The Sidebar and Header sizes are derived from the ScrollPanel
        Scheduler.get().scheduleDeferred( new ScheduledCommand() {

            public void execute() {
                assertDimensions();
            }

        } );
    }

    // Set width of outer most Widget and related children
    private void setWidth(int width) {
        mainPanel.setWidth( width
                            + "px" );
        scrollPanel.setWidth( (width - style.sidebarWidth())
                              + "px" );

        // The Sidebar and Header sizes are derived from the ScrollPanel
        Scheduler.get().scheduleDeferred( new ScheduledCommand() {

            public void execute() {
                assertDimensions();
            }

        } );
    }

}
