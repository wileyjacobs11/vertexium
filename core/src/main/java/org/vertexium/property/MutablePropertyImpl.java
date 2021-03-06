package org.vertexium.property;

import org.vertexium.*;
import org.vertexium.util.IncreasingTime;

import java.util.*;

public class MutablePropertyImpl extends MutableProperty {
    private final String key;
    private final String name;
    private final EnumSet<FetchHint> fetchHints;
    private Set<Visibility> hiddenVisibilities;
    private Object value;
    private Visibility visibility;
    private long timestamp;
    private final Metadata metadata;

    public MutablePropertyImpl(
            String key,
            String name,
            Object value,
            Metadata metadata,
            Long timestamp,
            Set<Visibility> hiddenVisibilities,
            Visibility visibility,
            EnumSet<FetchHint> fetchHints
    ) {
        if (metadata == null && fetchHints.contains(FetchHint.PROPERTY_METADATA)) {
            metadata = new Metadata();
        }

        this.key = key;
        this.name = name;
        this.value = value;
        this.metadata = metadata;
        this.timestamp = timestamp == null ? IncreasingTime.currentTimeMillis() : timestamp;
        this.visibility = visibility;
        this.hiddenVisibilities = hiddenVisibilities;
        this.fetchHints = fetchHints;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public Metadata getMetadata() {
        FetchHint.checkFetchHints(fetchHints, FetchHint.PROPERTY_METADATA);
        return metadata;
    }

    @Override
    public Iterable<Visibility> getHiddenVisibilities() {
        if (this.hiddenVisibilities == null) {
            return new ArrayList<>();
        }
        return this.hiddenVisibilities;
    }

    @Override
    public boolean isHidden(Authorizations authorizations) {
        if (hiddenVisibilities != null) {
            for (Visibility v : getHiddenVisibilities()) {
                if (authorizations.canRead(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    @Override
    public void addHiddenVisibility(Visibility visibility) {
        if (this.hiddenVisibilities == null) {
            this.hiddenVisibilities = new HashSet<>();
        }
        this.hiddenVisibilities.add(visibility);
    }

    @Override
    public void removeHiddenVisibility(Visibility visibility) {
        if (this.hiddenVisibilities == null) {
            this.hiddenVisibilities = new HashSet<>();
        }
        this.hiddenVisibilities.remove(visibility);
    }

    @Override
    protected void updateMetadata(Property property) {
        Collection<Metadata.Entry> entries = new ArrayList<>(property.getMetadata().entrySet());
        this.metadata.clear();
        for (Metadata.Entry metadataEntry : entries) {
            this.metadata.add(metadataEntry.getKey(), metadataEntry.getValue(), metadataEntry.getVisibility());
        }
    }
}
