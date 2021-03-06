package org.vertexium.accumulo.util;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.hadoop.io.Text;
import org.vertexium.FetchHint;
import org.vertexium.Property;
import org.vertexium.Visibility;
import org.vertexium.accumulo.AccumuloGraph;
import org.vertexium.accumulo.AccumuloNameSubstitutionStrategy;
import org.vertexium.accumulo.LazyMutableProperty;
import org.vertexium.accumulo.LazyPropertyMetadata;
import org.vertexium.accumulo.iterator.model.*;
import org.vertexium.accumulo.iterator.util.DataOutputStreamUtils;
import org.vertexium.id.NameSubstitutionStrategy;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class DataInputStreamUtils {
    public static Text decodeText(DataInputStream in) throws IOException {
        return org.vertexium.accumulo.iterator.util.DataInputStreamUtils.decodeText(in);
    }

    public static String decodeString(DataInputStream in) throws IOException {
        return org.vertexium.accumulo.iterator.util.DataInputStreamUtils.decodeString(in);
    }

    public static List<Text> decodeTextList(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count == -1) {
            return null;
        }
        List<Text> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(decodeText(in));
        }
        return results;
    }

    public static ImmutableSet<String> decodeStringSet(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count == -1) {
            return null;
        }
        ImmutableSet.Builder<String> results = ImmutableSet.builder();
        for (int i = 0; i < count; i++) {
            results.add(decodeString(in));
        }
        return results.build();
    }

    public static Iterable<Property> decodeProperties(
            AccumuloGraph graph,
            DataInputStream in,
            EnumSet<FetchHint> fetchHints
    ) throws IOException {
        List<Property> results = new ArrayList<>();
        while (true) {
            int propId = in.read();
            if (propId == ElementData.PROP_END) {
                break;
            } else if (propId != ElementData.PROP_START) {
                throw new IOException("Unexpected prop id: " + propId);
            }
            String propertyKey = graph.getNameSubstitutionStrategy().inflate(decodeString(in));
            String propertyName = graph.getNameSubstitutionStrategy().inflate(decodeString(in));
            Visibility propertyVisibility = new Visibility(decodeString(in));
            long propertyTimestamp = in.readLong();
            int propertyValueLength = in.readInt();
            byte[] propertyValue = new byte[propertyValueLength];
            int read = in.read(propertyValue);
            if (read != propertyValueLength) {
                throw new IOException("Unexpected data length expected " + propertyValueLength + " found " + read);
            }
            Set<String> propertyHiddenVisibilitiesStringSet = decodeStringSet(in);
            Set<Visibility> propertyHiddenVisibilities = null;
            if (propertyHiddenVisibilitiesStringSet != null) {
                propertyHiddenVisibilities = Sets.newHashSet(Iterables.transform(propertyHiddenVisibilitiesStringSet, new Function<String, Visibility>() {
                    @Nullable
                    @Override
                    public Visibility apply(String input) {
                        return new Visibility(input);
                    }
                }));
            }
            LazyPropertyMetadata metadata = decodePropertyMetadata(in, graph.getNameSubstitutionStrategy());
            results.add(new LazyMutableProperty(
                    graph,
                    graph.getVertexiumSerializer(),
                    propertyKey,
                    propertyName,
                    propertyValue,
                    metadata,
                    propertyHiddenVisibilities,
                    propertyVisibility,
                    propertyTimestamp,
                    fetchHints
            ));
        }
        return results;
    }

    private static LazyPropertyMetadata decodePropertyMetadata(DataInputStream in, AccumuloNameSubstitutionStrategy nameSubstitutionStrategy) throws IOException {
        LazyPropertyMetadata metadata = new LazyPropertyMetadata();
        int entryCount = in.readInt();
        for (int i = 0; i < entryCount; i++) {
            String key = nameSubstitutionStrategy.deflate(decodeString(in));
            Visibility visibility = new Visibility(decodeString(in));
            int valueLength = in.readInt();
            byte[] value = new byte[valueLength];
            int read = in.read(value);
            if (read != valueLength) {
                throw new IOException("Unexpected data length expected " + valueLength + " found " + read);
            }
            metadata.add(key, visibility, value);
        }
        return metadata;
    }

    public static Edges decodeEdges(DataInputStream in, NameSubstitutionStrategy nameSubstitutionStrategy) throws IOException {
        int edgeLabelMarker = in.readByte();
        if (edgeLabelMarker == DataOutputStreamUtils.EDGE_LABEL_WITH_REFS_MARKER) {
            return decodeEdgesWithRefs(in, nameSubstitutionStrategy);
        } else if (edgeLabelMarker == DataOutputStreamUtils.EDGE_LABEL_ONLY_MARKER) {
            return decodeEdgesLabelsOnly(in, nameSubstitutionStrategy);
        } else {
            throw new IOException("Unexpected edge label marker: " + edgeLabelMarker);
        }
    }

    private static Edges decodeEdgesLabelsOnly(DataInputStream in, NameSubstitutionStrategy nameSubstitutionStrategy) throws IOException {
        EdgesWithCount edges = new EdgesWithCount();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            String label = nameSubstitutionStrategy.inflate(decodeString(in));
            int edgeByLabelCount = in.readInt();
            edges.add(label, edgeByLabelCount);
        }
        return edges;
    }

    private static Edges decodeEdgesWithRefs(DataInputStream in, NameSubstitutionStrategy nameSubstitutionStrategy) throws IOException {
        EdgesWithEdgeInfo edges = new EdgesWithEdgeInfo();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            String label = decodeString(in);
            int edgeByLabelCount = in.readInt();
            for (int edgeByLabelIndex = 0; edgeByLabelIndex < edgeByLabelCount; edgeByLabelIndex++) {
                Text edgeId = decodeText(in);
                long timestamp = in.readLong();
                String vertexId = decodeString(in);
                EdgeInfo edgeInfo = new EdgeInfo(nameSubstitutionStrategy.inflate(label), vertexId, timestamp);
                edges.add(edgeId, edgeInfo);
            }
        }
        return edges;
    }

    public static byte[] decodeByteArray(DataInputStream in) throws IOException {
        return org.vertexium.accumulo.iterator.util.DataInputStreamUtils.decodeByteArray(in);
    }

    public static void decodeHeader(DataInputStream in, byte expectedTypeId) throws IOException {
        byte[] header = new byte[ElementData.HEADER.length];
        int read = in.read(header);
        if (read != header.length) {
            throw new IOException("Unexpected header length. Expected " + ElementData.HEADER.length + " found " + read);
        }
        if (!Arrays.equals(header, ElementData.HEADER)) {
            throw new IOException("Unexpected header");
        }
        int typeId = in.read();
        if (typeId != expectedTypeId) {
            throw new IOException("Unexpected type id. Expected " + expectedTypeId + " found " + typeId);
        }
    }
}
