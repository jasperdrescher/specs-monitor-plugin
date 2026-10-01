package io.jenkins.plugins.specsmonitor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class SpecsMonitor extends NodeMonitor {

    @DataBoundConstructor
    public SpecsMonitor() {}

    @Extension
    @Symbol("cpuInfo")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<CpuInfo> {

        @Override
        protected MasterToSlaveCallable<CpuInfo, IOException> createCallable(Computer c) {
            return new GetCpuInfo();
        }

        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }
    }

    /** Runs on the node itself, so it reports the node's hardware. Windows only. */
private static class GetCpuInfo extends MasterToSlaveCallable<CpuInfo, IOException> {
    private static final long serialVersionUID = 1L;

    @Override
    public CpuInfo call() throws IOException {
        int cores = Runtime.getRuntime().availableProcessors();
        return new CpuInfo(detectName(), cores);
    }

    private static String detectName() {
        try {
            String out = run("powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                    "(Get-CimInstance Win32_Processor | Select-Object -First 1).Name");
            if (!out.isEmpty()) {
                return out.replaceAll("\\s+", " ");
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "PowerShell CPU lookup failed, trying registry", e);
        }
        try {
            String out = run("reg", "query",
                    "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0",
                    "/v", "ProcessorNameString");
            int i = out.indexOf("REG_SZ");
            if (i >= 0) {
                return out.substring(i + 6).trim().replaceAll("\\s+", " ");
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Registry CPU lookup failed", e);
        }
        return System.getenv().getOrDefault("PROCESSOR_IDENTIFIER", "unknown");
    }

    private static String run(String... cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        try {
            p.getOutputStream().close(); // PowerShell can wait on stdin otherwise
            String out;
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
                out = r.lines().collect(Collectors.joining("\n")).trim();
            }
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                throw new IOException("Timed out running " + cmd[0]);
            }
            return out;
        } finally {
            p.destroyForcibly();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(GetCpuInfo.class.getName());
    }
}
