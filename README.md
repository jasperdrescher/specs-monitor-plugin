# Specs Monitor

## Introduction

Adds a node monitor that shows the CPU model and the number of hardware threads (logical processors) of every
node (built-in node and agents) in the Jenkins node list. This makes it easy to see which
agents are fast or slow without opening each machine.

The monitor runs on the node itself, so it reports the hardware of that node. Long vendor
strings such as `13th Gen Intel(R) Core(TM) i9-13900H` are shortened to `i9-13900H`.

Supported operating systems:

- Windows (PowerShell, with a registry fallback)
- Linux (`lscpu`, with `/proc/cpuinfo` as a fallback), including x86, ARM (aarch64) and POWER
- macOS (`sysctl`)

On other systems (for example AIX or Solaris), or if the CPU name cannot be determined, the
monitor shows `N/A`.

## Getting started

1. Install the plugin.
2. Go to **Manage Jenkins → Nodes → Configure Monitors** (gear icon).
3. Enable **Specs Monitor**.
4. A CPU column appears in the node list, for example `i7-13700K (16 threads)`.

### Configuration as Code

```yaml
jenkins:
  nodeMonitors:
    - specsMonitor
```

## Issues

Report issues and enhancements on the
[GitHub issue tracker](https://github.com/jenkinsci/specs-monitor-plugin/issues).

## Contributing

Refer to the Jenkins
[contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md).

To try the plugin locally:

```
mvn hpi:run
```

## License

Licensed under MIT, see [LICENSE.md](LICENSE.md).
