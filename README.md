![FancyJCL](logo/logo.png)

[![Release](https://jitpack.io/v/HPC-ULL/FancyJCL.svg)](https://jitpack.io/#HPC-ULL/FancyJCL)

# Description
This android module uses the [Fancier](https://github.com/HPC-ULL/Fancier) High-Performance library to allow execution of OpenCL kernels on the GPU from Java or Kotlin. It is not just an OpenCL wrapper, since it takes care of all the glue code that is needed to initialize, transfer memory, create programs and kernels and manage the OpenCL execution queue.

# Getting Started

## Installation 
To add this module in your android project:

1. Add this in your root build.gradle at the end of repositories:
```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
2. Add the dependency.
```
dependencies {
        implementation 'com.github.DoMondo:Fancier:Tag'
}
```

## Usage

Say we want to add a constant to an existing Java byte array (`byte []`):

```java
float kConstant = 95;
for (int i = 0; i < input.length; i++) {
    output[i] = input[i] + kConstant;
}
```
In order to use FancyJCL to execute this in GPU, we would rewrite it like so:
```java
Stage stage = new Stage();
stage.setKernelSource("output[d0] = input[d0] * kConstant;");
stage.setInputs(Map.of("input", input, "kConstant", kConstant));
stage.setOutputs(Map.of("output", output));
stage.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{4}));
stage.runSync();
```

For more complex scenarios, check the [tutorials](tutorials.md)

## Example application
An application that tests and benchmarks many image filters is located in `examples/example_app`.

# Documentation
Check the [Javadocs](https://hpc-ull.github.io/FancyJCL/).
