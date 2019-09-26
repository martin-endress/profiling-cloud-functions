# Profiling a Cloud Function - Bachelor Thesis

## Overview


```
├── calibration             calibration files (local and provider)
├── executor                resource simulating container image
├── experiments             definition of experiment files
├── gradle
├── linpack                 Linpack benchmark definitions
│   ├── aws
│   ├── local
│   └── src
├── profiles                Profiles
├── profiling               Prototype of profiling approach
├── pythonscripts           Model visualisation
├── serviceMock             part of executor
└── sysbench                (see 6.2 in thesis)
```


## Notes
- Check with `docker info` what resource limitations are enabled in the host kernel. (Windows is restricted)
- Calibrations and Profiles already created are not overwritten and skipped.


## Profiling steps

- Make sure the calibration cloud function is deployed under the specified endpoint(s).
  - deploy script in `linpack/aws`
- Define an experiment in the `experiments` folder. (see template)
- Run `gradle profiling:run` in the project root. (IntelliJ run may not work)
- Profiles are saved under `profiles`
- Visualising the model is done using python (folder `pythonscripts`)
  - `plotProfile.py` - Plot a single profile. (specify folder)
  - `plotProfiles.py` - Plots resource usage as histogram (specify folder).
  - `plotXBenchmark.py` - Plot benchmark results and linear regression model.
  - `plotNonLinear.py` - see section 6.3