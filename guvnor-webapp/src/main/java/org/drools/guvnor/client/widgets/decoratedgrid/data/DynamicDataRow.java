/*
 * Copyright 2011 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.drools.guvnor.client.widgets.decoratedgrid.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.drools.guvnor.client.widgets.decoratedgrid.CellValue;

/**
 * A row of data in the Decision Table
 */
public class DynamicDataRow
    implements
    Iterable<CellValue< ? extends Comparable< ? >>> {

    private List<CellValue< ? extends Comparable< ? >>> rowData;

    DynamicDataRow() {
        rowData = new ArrayList<CellValue< ? extends Comparable< ? >>>();
    }

    public CellValue< ? extends Comparable< ? >> get(int index) {
        return rowData.get( index );
    }

    public Iterator<CellValue< ? extends Comparable< ? >>> iterator() {
        return rowData.iterator();
    }

    public CellValue< ? extends Comparable< ? >> set(int index,
                                                     CellValue< ? extends Comparable< ? >> cell) {
        return rowData.set( index,
                            cell );
    }

    public int size() {
        return rowData.size();
    }

    boolean add(CellValue< ? extends Comparable< ? >> cell) {
        return rowData.add( cell );
    }

    void add(int index,
             CellValue< ? extends Comparable< ? >> cell) {
        rowData.add( index,
                     cell );
    }

    void clear() {
        rowData.clear();
    }

    CellValue< ? extends Comparable< ? >> remove(int index) {
        return rowData.remove( index );
    }

}
