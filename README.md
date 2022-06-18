# Containerized Profiling of Cloud Function

This repository contains the research tool implemented as part of my bachelor thesis.

## Thesis Abstract

Serverless Computing has attracted a lot of attention in recent years.
In the Function as a Service realization of the serverless paradigm, the developer is released from operational concerns and only has to provide for the business logic of a service.
However, in most cases, developers still face the challenge of choosing the amount of CPU and memory allocated for their cloud functions.
Since it is not an easy, yet an important task, we provide a decision aid based on an offline, containerized profiling approach.
With the goal of better understanding the resource usage of a cloud function, we run it locally inside a container and measure its resource demands.
For this, we propose to map the execution environment of a cloud provider to the local machine.
The aim is to get closer to the local simulation of the cloud environment and thereby provide meaningful information about a cloud function prior to its deployment.
From there, we develop a model which describes the cloud function in a mostly hardware independent way.

## Thesis Details

The thesis can be accessed [here](ContainerizedProfilingOfCloudFunctions.pdf).

Thesis Supervisor: [Guido Wirtz](https://www.uni-bamberg.de/pi/team/wirtz/)  
Thesis Advisor: [Johannes Manner](https://www.uni-bamberg.de/pi/team/manner-johannes/)

An extended version of this thesis was published on the IEEE International Conference on Cloud Computing in 2021: [Optimizing Cloud Function Configuration via Local Simulations](https://ieeexplore.ieee.org/abstract/document/9582177)
