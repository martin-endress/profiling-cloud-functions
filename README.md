BachelorArbeit von Martin Endreß

docker stats are retrieved from information about control groups.

these are available in /sys/fs/cgroup/docker/<long_id>/

For example, 'cpuacct.usage' stores the current cpu usage. This is also available in stats (every 1s).

in 'cpuacct.stat',
user time is the amount of time a process has direct control of the CPU, executing process code.
system time is the time the kernel is executing system calls on behalf of the process.
