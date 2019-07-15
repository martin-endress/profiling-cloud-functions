# BachelorArbeit von Martin Endreß

## Overview

```
├── cloudFunctions       | set of test cloud functions
│   ├── build            | built cloud functions 
│   ├── build.gradle     | 
│   └── src              | 
├── executor             | cloud function executor
│   ├── build.gradle     | 
│   ├── Dockerfile       | definition of the docker container
│   └── src              | 
├── plots                | examplary plots (will be replaced by automatically generated ones)
├── profiles             | result profiles and container logs
│   ├── Profile 1        |
│   └── Profile 2        | 
├── profiling            | retrieves docker statistics and cgroup information from running containers
│   ├── build.gradle     | 
│   └── src              | 
├── pythonscripts        | python scripts for plotting
│   ├── Dockerfile       | automatically generate plots (work in progress)
│   └── plotProfile.py   | plot all available `metrics.csv` files in profiles
├── README.md            | this file
├── serviceMock          | service which generates response of certain size after a delay /api/getResponse?size=[size]&delay=[delay]
│   ├── Dockerfile       | definition of the docker container
│   └── src              | 
└── settings.gradle      | project settings
```

## Getting Started

Start the stats retriever.  
From there, both the service mock image and profiling image is built. After that, they are executed and a new profile is created.  
No parameters are needed. Currently (ugly), the IO/CPU load pattern is defined inside `cloudFunctions/../Mixed`.

Check with `docker info` what resource limitations are enabled in the host kernel.

## cgroup information

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


## TODOs

ressourcen einschränken, RAM und CPU dazu skalieren
vergleich cgroups vs docker api detailliert..

## Open Issues

get total usage of a container execution. currently, the last recorded metric is used.
