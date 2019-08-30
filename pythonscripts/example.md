# Example Modeling Process

## AWS

R^2:  0.9985342877635649
intercept: -0.5162863981524453
slope: 0.01124417

## Primary Machine

R^2:  0.9987540199298135
intercept: -5.6255171428571344
slope: 40.89928857


## Secondary Machine

## Simulation
memory settings: 256, 2048

AWSmodel.expect(256) = 2.36 GFLOPS
PrimaryModel.expectInverse(2.36) = 0.195 CPU Quota
SecondaryModel.expectInverse(2.36) = xxx

AWSmodel.expect(2048) = 22.511 GFLOPS
LocalModel.expectInverse(22.511) = 0.951 CPU Quota
SecondaryModel.expectInverse(22.511) = xxx

## OLD Simulation
map.put("LOAD_TIME", "30000");
map.put("CPU_FIBONACCI", "45");
map.put("MEMORY_TO", String.valueOf(64 * 1024 * 1024)); // 64 mb