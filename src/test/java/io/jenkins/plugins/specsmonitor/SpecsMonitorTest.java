package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.ExtensionList;
import hudson.node_monitors.NodeMonitor;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class SpecsMonitorTest {

    @Test
    void descriptorIsRegistered(JenkinsRule j) {
        // Throws IllegalStateException if the @Extension was not discovered.
        SpecsMonitor.DescriptorImpl descriptor = ExtensionList.lookupSingleton(SpecsMonitor.DescriptorImpl.class);
        assertNotNull(descriptor);
    }

    @Test
    void monitorAppearsInNodeMonitorList(JenkinsRule j) {
        assertTrue(j.jenkins.getDescriptorList(NodeMonitor.class).stream()
                .anyMatch(d -> d instanceof SpecsMonitor.DescriptorImpl));
    }

    @Test
    void descriptorHasDisplayName(JenkinsRule j) {
        SpecsMonitor.DescriptorImpl descriptor = ExtensionList.lookupSingleton(SpecsMonitor.DescriptorImpl.class);
        assertNotNull(descriptor.getDisplayName());
        assertFalse(descriptor.getDisplayName().isBlank());
    }

    /**
     * The callable must never throw on a non-Windows machine (e.g. Linux CI); it
     * should
     * fall back gracefully and still report the core count.
     */
    @Test
    void callableReturnsResultOnAnyOs(JenkinsRule j) throws Exception {
        SpecsMonitor.DescriptorImpl descriptor = ExtensionList.lookupSingleton(SpecsMonitor.DescriptorImpl.class);

        CpuInfo info = descriptor.createCallable(j.jenkins.toComputer()).call();

        assertNotNull(info);
        assertNotNull(info.getName());
        assertFalse(info.getName().isBlank());
        assertTrue(info.getThreads() >= 1);
    }
}
