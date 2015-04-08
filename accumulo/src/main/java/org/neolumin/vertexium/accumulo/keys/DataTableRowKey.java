package org.neolumin.vertexium.accumulo.keys;

import org.neolumin.vertexium.Property;
import org.neolumin.vertexium.accumulo.AccumuloConstants;

public class DataTableRowKey extends KeyBase {
    private static final int PARTS_INDEX_ELEMENT_ROW_KEY = 0;
    private static final int PARTS_INDEX_PROPERTY_NAME = 1;
    private static final int PARTS_INDEX_PROPERTY_KEY = 2;
    private final String[] parts;

    public DataTableRowKey(String elementRowKey, Property property) {
        this.parts = new String[]{
                elementRowKey,
                property.getName(),
                property.getKey()
        };
    }

    public String getRowKey() {
        return AccumuloConstants.DATA_ROW_KEY_PREFIX
                + getElementRowKey()
                + VALUE_SEPARATOR + getPropertyName()
                + VALUE_SEPARATOR + getPropertyKey();
    }

    public String getElementRowKey() {
        return parts[PARTS_INDEX_ELEMENT_ROW_KEY];
    }

    public String getPropertyName() {
        return parts[PARTS_INDEX_PROPERTY_NAME];
    }

    public String getPropertyKey() {
        return parts[PARTS_INDEX_PROPERTY_KEY];
    }
}
