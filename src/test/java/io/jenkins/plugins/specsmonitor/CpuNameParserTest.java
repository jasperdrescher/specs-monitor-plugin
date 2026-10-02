package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CpuNameParserTest {

    private static final String LSCPU_PPC64LE = """
            Architecture:                ppc64le
              Byte Order:                Little Endian
            CPU(s):                      8
              On-line CPU(s) list:       0-7
            Model name:                  POWER10 (architected), altivec supported
              Model:                     2.0 (pvr 0080 0200)
              Thread(s) per core:        8
            """;

    private static final String LSCPU_AARCH64 = """
            Architecture:                aarch64
              CPU op-mode(s):            64-bit
              Byte Order:                Little Endian
            CPU(s):                      16
            Vendor ID:                   ARM
              Model name:                Neoverse-V2
                Model:                   1
                Thread(s) per core:      1
            """;

    private static final String LSCPU_X86 = """
            Architecture:                    x86_64
            Vendor ID:                       GenuineIntel
            Model name:                      13th Gen Intel(R) Core(TM) i9-13900H
            BIOS Model name:                 Some BIOS String
            """;

    @Test
    void lscpuPower() {
        assertEquals("POWER10 (architected), altivec supported", CpuNameParser.fromLscpu(LSCPU_PPC64LE));
    }

    @Test
    void lscpuAarch64() {
        assertEquals("Neoverse-V2", CpuNameParser.fromLscpu(LSCPU_AARCH64));
    }

    @Test
    void lscpuX86IgnoresBiosModelName() {
        assertEquals("13th Gen Intel(R) Core(TM) i9-13900H", CpuNameParser.fromLscpu(LSCPU_X86));
    }

    @Test
    void lscpuWithoutModelNameIsEmpty() {
        assertEquals("", CpuNameParser.fromLscpu("Architecture: s390x\nCPU(s): 4\n"));
        assertEquals("", CpuNameParser.fromLscpu(""));
        assertEquals("", CpuNameParser.fromLscpu(null));
    }

    @Test
    void procCpuinfoX86() {
        List<String> lines = List.of(
                "processor\t: 0",
                "vendor_id\t: GenuineIntel",
                "cpu family\t: 6",
                "model\t\t: 158",
                "model name\t: Intel(R) Core(TM) i7-8700 CPU @ 3.20GHz");
        assertEquals("Intel(R) Core(TM) i7-8700 CPU @ 3.20GHz", CpuNameParser.fromProcCpuinfo(lines));
    }

    @Test
    void procCpuinfoPower() {
        List<String> lines = List.of(
                "processor       : 0",
                "cpu             : POWER10 (architected), altivec supported",
                "clock           : 2950.000000MHz",
                "revision        : 2.0 (pvr 0080 0200)");
        assertEquals("POWER10 (architected), altivec supported", CpuNameParser.fromProcCpuinfo(lines));
    }

    @Test
    void procCpuinfoAarch64HasNoReadableName() {
        List<String> lines = List.of(
                "processor       : 0",
                "BogoMIPS        : 2000.00",
                "Features        : fp asimd evtstrm aes",
                "CPU implementer : 0x41",
                "CPU architecture: 8",
                "CPU part        : 0xd4f");
        assertEquals("", CpuNameParser.fromProcCpuinfo(lines));
    }
}
