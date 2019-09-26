# Linpack Documentation

Three types of linpack benchmarks are developed:

## Local Linpack

defined in `local`

## AWS Linpack

defined in `aws`

The build scripts `create.sh` and `deploy.sh` can be used to deploy the benchmark to AWS. AWS Gateway endpoints are not defined in the script.

# Java AWS Linpack

defined in `build.gradle` / `src`

This is the implementation of the experiment described in section 6.1 in the thesis.