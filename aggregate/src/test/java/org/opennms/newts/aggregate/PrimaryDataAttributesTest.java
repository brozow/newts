package org.opennms.newts.aggregate;

import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.opennms.newts.api.Timestamp.fromEpochSeconds;

import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import org.opennms.newts.aggregate.Utils.SampleRowsBuilder;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;

import com.google.common.collect.Maps;

public class PrimaryDataAttributesTest {

    @Test
    public void testOverlappingAttributes() {

        // This set has samples with attributes that fall strictly within sample intervals.
        Iterator<Row<Sample>> testData = new SampleRowsBuilder("localhost", MetricType.GAUGE)
                // Part of interval 900000300 (Row 1)
                .row(900000297).element("m0", 1, mapFor("a", "1"))
                .row(900000298).element("m0", 1, mapFor("a", "2", "c", "5"))
                // Part of intervals 900000300 and 900000600
                .row(900000301).element("m0", 1, mapFor("a", "3"))
                // Part of interval 900000600 (Row 2)
                .row(900000597).element("m0", 1, mapFor("b", "1"))
                .row(900000598).element("m0", 1, mapFor("b", "2"))
                .row(900000599).element("m0", 1, mapFor("b", "3"))
                // Part of interval 900000900 (Row 3)
                .row(900000899).element("m0", 2)
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(600), null);

        PrimaryData primaryData = new PrimaryData(
                "localhost",
                Timestamp.fromEpochSeconds(900000300),
                Timestamp.fromEpochSeconds(900000900),
                rDescriptor,
                testData);

        // Row 1
        Row<Measurement> row = primaryData.next();
        assertThat(row.getTimestamp(), equalTo(fromEpochSeconds(900000300)));
        assertAttributes(row.getElement("m0"), mapFor("a", "3", "c", "5"));

        // Row 2
        row = primaryData.next();
        assertThat(row.getTimestamp(), equalTo(fromEpochSeconds(900000600)));
        assertAttributes(row.getElement("m0"), mapFor("a", "3", "b", "3"));

        // Row 3
        row = primaryData.next();
        assertThat(row.getTimestamp(), equalTo(fromEpochSeconds(900000900)));
        assertAttributes(row.getElement("m0"), mapFor());

    }

    @Test
    public void testAttributesWithinInterval() {

        // This set has samples with attributes that fall strictly within sample intervals.
        Iterator<Row<Sample>> testData = new SampleRowsBuilder("localhost", MetricType.GAUGE)
                // Part of interval 900000300
                .row(900000297).element("m0", 1, mapFor("a", "1"))
                .row(900000298).element("m0", 1, mapFor("a", "2", "c", "5"))
                .row(900000299).element("m0", 1, mapFor("a", "3"))
                .row(900000305).element("m0", 1)
                // Part of interval 900000600
                .row(900000597).element("m0", 1, mapFor("b", "1"))
                .row(900000598).element("m0", 1, mapFor("b", "2"))
                .row(900000599).element("m0", 1, mapFor("b", "3"))
                // Part of interval 900000900
                .row(900000899).element("m0", 2)
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(600), null);

        PrimaryData primaryData = new PrimaryData(
                "localhost",
                Timestamp.fromEpochSeconds(900000300),
                Timestamp.fromEpochSeconds(900000900),
                rDescriptor,
                testData);

        // Row 1
        Row<Measurement> row = primaryData.next();
        assertThat(row.getTimestamp(), equalTo(fromEpochSeconds(900000300)));
        assertAttributes(row.getElement("m0"), mapFor("a", "3", "c", "5"));

        // Row 2
        row = primaryData.next();
        assertThat(row.getTimestamp(), equalTo(fromEpochSeconds(900000600)));
        assertAttributes(row.getElement("m0"), mapFor("b", "3"));

        // Row 3
        row = primaryData.next();
        assertThat(row.getTimestamp(), equalTo(fromEpochSeconds(900000900)));
        assertAttributes(row.getElement("m0"), mapFor());

    }

    @Test
    public void testMultipleSamples() {

        // This set has samples with attributes that fall strictly within sample intervals.
        Iterator<Row<Sample>> testData = new SampleRowsBuilder("localhost", MetricType.GAUGE)
                // Part of interval 900000300 (Row 1)
                .row(900000297).element("m0", 1, mapFor("a", "1")).element("m1", 2, mapFor("aa", "11"))
                .row(900000298).element("m0", 1, mapFor("a", "2", "c", "5"))
                // Part of intervals 900000300 and 900000600
                .row(900000301).element("m0", 1, mapFor("a", "3")).element("m1", 2, mapFor("aa", "33"))
                // Part of interval 900000600 (Row 2)
                .row(900000597).element("m0", 1, mapFor("b", "1")).element("m1", 2, mapFor("bb", "11"))
                .row(900000598).element("m0", 1, mapFor("b", "2")).element("m1", 2, mapFor("bb", "22"))
                .row(900000599).element("m0", 1, mapFor("b", "3")).element("m1", 2, mapFor("bb", "33"))
                // Part of interval 900000900 (Row 3)
                .row(900000899).element("m0", 2).element("m1", 3)
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(600), null)
                .datasource("m1", "m1", Duration.seconds(600), null);

        PrimaryData primaryData = new PrimaryData(
                "localhost",
                Timestamp.fromEpochSeconds(900000300),
                Timestamp.fromEpochSeconds(900000900),
                rDescriptor,
                testData);

        // Row 1
        Row<Measurement> row = primaryData.next();
        assertThat(row.getTimestamp(), equalTo(fromEpochSeconds(900000300)));
        assertAttributes(row.getElement("m0"), mapFor("a", "3", "c", "5"));
        assertAttributes(row.getElement("m1"), mapFor("aa", "33"));

        // Row 2
        row = primaryData.next();
        assertThat(row.getTimestamp(), equalTo(fromEpochSeconds(900000600)));
        assertAttributes(row.getElement("m0"), mapFor("a", "3", "b", "3"));
        assertAttributes(row.getElement("m1"), mapFor("aa", "33", "bb", "33"));

        // Row 3
        row = primaryData.next();
        assertThat(row.getTimestamp(), equalTo(fromEpochSeconds(900000900)));
        assertAttributes(row.getElement("m0"), mapFor());
        assertAttributes(row.getElement("m1"), mapFor());
        
    }

    static Map<String, String> mapFor(String... attributes) {
        checkArgument((attributes.length % 2) == 0, "not an even sequence of k/v pairs");

        Map<String, String> r = Maps.newHashMap();

        for (int i = 0; i < attributes.length; i += 2) {
            r.put(attributes[i], attributes[i + 1]);
        }

        return r;
    }

    static void assertAttributes(Measurement measurement, Map<String, String> expected) {
        assertThat("Missing measurement", measurement, notNullValue());
        assertThat("Missing measurement attributes", measurement.getAttributes(), notNullValue());
        assertThat(measurement.getAttributes(), equalTo(expected));
    }

}
