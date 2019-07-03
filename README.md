BachelorArbeit von Martin Endreß

docker stats are retrieved from information about control groups.

these are available in /sys/fs/cgroup/<type>/docker/<long_id>/

type can be 'memory', 'cpu'

For example, 'cpuacct.usage' stores the current cpu usage. This is also available in stats (every 1s).

in 'cpuacct.stat',
user time is the amount of time a process has direct control of the CPU, executing process code.
system time is the time the kernel is executing system calls on behalf of the process.




|   | property | description |
|---|---|---|
|MEM| cache | memory usage |
|   | swap | memory in swap |
|   | active_anon |   |
|   | inactive_file |   |
|   |   |   |
|CPU| totalCpuUsage | cpu usage in nanoseconds |
|   | cpu_i | cpu usage of each code in nanoseconds |
|   | user | cpu usage in USER_HZ / seconds (usually 100/s) |
|   | system | time kernel is executing system calls (again 100/s) |
|   |  |  |
|I/O| blkio.sectors |  |


ressourcen einschränken, RAM und CPU dazu skalieren
io - cpu container
vergleich cgroups vs docker api detailliert..

