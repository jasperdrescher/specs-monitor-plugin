package io.jenkins.plugins.specsmonitor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/** Node monitor that reports the CPU model and thread count of each node. */
public class SpecsMonitor extends NodeMonitor {

    @DataBoundConstructor
    public SpecsMonitor() {}

    @Extension
    @Symbol("specsMonitor")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<CpuInfo> {

        @Override
        protected MasterToSlaveCallable<CpuInfo, IOException> createCallable(Computer c) {
            return new GetCpuInfo();
        }

        @Override
        public String getDisplayName() {
            return Messages.displayName();
        }
    }

    /**
     * Runs on the node itself, so it reports the node's hardware. Supports Windows,
     * Linux and macOS;
     * other systems report N/A.
     */
    private static final class GetCpuInfo extends MasterToSlaveCallable<CpuInfo, IOException> {
        private static final long serialVersionUID = 1L;

        private static final Logger LOGGER = Logger.getLogger(GetCpuInfo.class.getName());
        private static final long COMMAND_TIMEOUT_SECONDS = 10;
        private static final long READ_TIMEOUT_SECONDS = 2;

        @Override
        public CpuInfo call() throws IOException {
            int threads = Runtime.getRuntime().availableProcessors();
            return new CpuInfo(detectName(), threads);
        }

        private static String detectName() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String name = "";
            if (os.startsWith("windows")) {
                name = detectWindows();
            } else if (os.startsWith("mac")) {
                name = tryRun("sysctl", "-n", "machdep.cpu.brand_string");
            } else if (os.startsWith("linux")) {
                name = detectLinux();
            }
            if (!name.isBlank()) {
                return name.replaceAll("\\s+", " ").trim();
            }
            // Other systems (AIX, Solaris, BSD, ...) and detection failures. The
            // architecture is already
            // shown by the built-in architecture monitor, so there is nothing more useful
            // to report.
            String identifier = System.getenv("PROCESSOR_IDENTIFIER");
            if (identifier != null && !identifier.isBlank()) {
                return identifier;
            }
            return "N/A";
        }

        private static String detectWindows() {
            String out = tryRun(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-Command",
                    "(Get-CimInstance Win32_Processor | Select-Object -First 1).Name");
            if (!out.isBlank()) {
                return out;
            }
            out = tryRun(
                    "reg",
                    "query",
                    "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0",
                    "/v",
                    "ProcessorNameString");
            int i = out.indexOf("REG_SZ");
            return i >= 0 ? out.substring(i + "REG_SZ".length()).trim() : "";
        }

        private static String detectLinux() {
            // lscpu knows how to name CPUs that /proc/cpuinfo only describes with numeric
            // IDs (e.g. aarch64).
            String name = CpuNameParser.fromLscpu(tryRun("lscpu"));
            if (!name.isEmpty()) {
                return name;
            }
            try {
                return CpuNameParser.fromProcCpuinfo(
                        Files.readAllLines(Path.of("/proc/cpuinfo"), StandardCharsets.UTF_8));
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Could not read /proc/cpuinfo", e);
                return "";
            }
        }

        /**
         * Runs a command and returns its trimmed output, or an empty string if it
         * fails.
         */
        private static String tryRun(String... cmd) {
            try {
                return run(cmd);
            } catch (IOException e) {
                LOGGER.log(Level.FINE, e, () -> "Command failed: " + cmd[0]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.FINE, e, () -> "Interrupted running: " + cmd[0]);
            }
            return "";
        }

        private static String run(String... cmd) throws IOException, InterruptedException {
            ProcessBuilder builder = new ProcessBuilder(cmd).redirectErrorStream(true);
            // Make tool output independent of the node's locale (e.g. "Model name:" in
            // lscpu).
            builder.environment().put("LC_ALL", "C");
            builder.environment().put("LANG", "C");
            Process p = builder.start();
            try {
                p.getOutputStream().close(); // PowerShell can wait on stdin otherwise
                CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readAll(p));
                if (!p.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new IOException("Timed out running " + cmd[0]);
                }
                return output.get(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                throw new IOException("Failed to read output of " + cmd[0], e);
            } finally {
                p.destroyForcibly();
            }
        }

        private static String readAll(Process p) {
            try (BufferedReader r =
                    new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                return r.lines().collect(Collectors.joining("\n")).trim();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
