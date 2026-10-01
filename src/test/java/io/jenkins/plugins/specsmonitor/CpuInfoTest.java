package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CpuInfoTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(
            delimiter = '|',
            value = {
                "Intel(R) Core(TM) i7-13700K|i7-13700K",
                "13th Gen Intel(R) Core(TM) i9-13900H|i9-13900H",
                "Intel(R) Core(TM) i7-8700 CPU @ 3.20GHz|i7-8700",
                "AMD Ryzen 7 5800X 8-Core Processor|Ryzen 7 5800X",
                "AMD Ryzen 9 7950X 16-Core Processor|Ryzen 9 7950X",
                "AMD EPYC 7763 64-Core Processor|EPYC 7763",
                "AMD   Ryzen   5   5600X|Ryzen 5 5600X",
                "intel(r) core(tm) i5-12400|i5-12400"
            })
    void shortNameStripsNoise(String raw, String expected) {
        assertEquals(expected, new CpuInfo(raw, 8).getShortName());
    }

    @Test
    void shortNameFallsBackToRawNameWhenEverythingIsNoise() {
        assertEquals("Intel CPU", new CpuInfo("Intel CPU", 4).getShortName());
    }

    @Test
    void shortNameFallsBackToRawNameWhenEmpty() {
        assertEquals("", new CpuInfo("", 4).getShortName());
    }

    @Test
    void nullNameDoesNotThrow() {
        CpuInfo info = new CpuInfo(null, 2);
        assertNull(info.getName());
        assertNull(info.getShortName());
    }

    @Test
    void getNameReturnsRawName() {
        assertEquals("Intel(R) Core(TM) i7-13700K", new CpuInfo("Intel(R) Core(TM) i7-13700K", 8).getName());
    }

    @Test
    void getCoresReturnsCoreCount() {
        assertEquals(16, new CpuInfo("AMD Ryzen 9 7950X", 16).getThreads());
    }

    @Test
    void toStringUsesShortNameAndCores() {
        assertEquals("i7-13700K (8)", new CpuInfo("Intel(R) Core(TM) i7-13700K", 8).toString());
    }

    /** CpuInfo is sent from agent to controller over remoting, so it must survive serialization. */
    @Test
    void survivesSerializationRoundTrip() throws Exception {
        CpuInfo original = new CpuInfo("AMD Ryzen 7 5800X 8-Core Processor", 16);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }

        CpuInfo copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (CpuInfo) in.readObject();
        }

        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getThreads(), copy.getThreads());
        assertEquals(original.getShortName(), copy.getShortName());
    }
}
