# Initialization

Before using the FancyJCL module you need to initialize it. For example, in your MainActivity do
this:

```java
FancyJCLManager.initialize(getApplicationContext().getCacheDir().getAbsolutePath());
```

# Executing a simple kernel

In this example we will:

- Create three direct buffers
- Create a `Stage` that will compute the <b>weighted average</b> of each element of the first two buffers
  and store it in the third
- Show information about the `Stage`
- Execute the stage

## Memory allocation

This library has been devised with performance in mind. Usually memory synchronization between Java,
JNI and OpenCL is a bottleneck. Fancier is taking care of implementing zero-copy between JNI and
OpenCL. Allocating memory in Java will always have a penalty, except in the case of using
DirectBuffers so use them whenever possible.

```java
ByteBuffer input0 = ByteBuffer.allocateDirect(size);
ByteBuffer input1 = ByteBuffer.allocateDirect(size);
ByteBuffer output = ByteBuffer.allocateDirect(size);
```

If this is not possible, you can also use `{byte,short,int,float,double} []`:

```java
int[] input0 = new int[size];
int[] input1 = new int[size];
int[] output = new int[size];
```
We will also declare two constants for the weights of the mean:

```java
float k0 = 0.3f;
float k1 = 0.7f;
```

## Declaring the `Stage`

A `Stage` is an execution unit that has information about an algorithm, its parameters and the run
configuration i.e. how long is the range of execution per variable and how many GPU threads to use.

In order to create a `Stage` do this:

```java
Stage stage = new Stage();
```

## Declaring parameters

To declare inputs and outputs we will use a map, to name each one and pass the corresponding Java
object:

```java
stage.setInputs(Map.of("input0",input0, "input1",input1, "k0", k0, "k1", k1));
stage.setOutputs(Map.of("output", output));
```

## Declaring the kernel

To specify what the OpenCL kernel to execute is going to be we write it like it was OpenCL kernel code, but without the kernel signature. The function `get_global_id(n)`  can be replaced with `dn`. The names used in the `setInputs` and `setOutputs` methods must be used here.

```java
stage.setKernelSource("""

  // Get pixels and multiply by constant
  float p0 = input0[d0] * k0;
  float p1 = input1[d0] * k1;

  // Store pixel
  output[d0] = (int) round(p0 + p1);

                            """);
```

## Declaring the `RunConfiguration`
The last thing to do before executing is to specify the length and the number of threads to use per dimension. In this case we will traverse the size elements as the first dimension and use 4 threads.
```java
stage.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{4}));
```

## Execute it

```java
stage.runSync();
```
This call is blocking and ensures that the outputs are back in CPU, whereas `sync` is not.

## Displaying debugging information

```java
stage.printSummary();
```

Using logcat to get logs will show this:

```java
********************************************************************************
	 - STAGE NAME: kernel_0
	 - PARAMETERS:
		0: [INPUT] "input1" (bytearray)[25]
		1: [INPUT] "input0" (bytearray)[25]
		2: [INPUT] "k0" (float)
		3: [INPUT] "k1" (float)
		4: [OUTPUT] "output" (bytearray)[25]
	 - KERNEL:
--------------------------------------------------------------------------------
kernel void kernel_0(global const char* input1, global const char* input0, const float k0, const float k1, global char* output) {
  // Get pixels and multiply by constant
  float p0 = input0[get_global_id(0)] * k0;
  float p1 = input1[get_global_id(0)] * k1;
  // Store pixel
  output[get_global_id(0)] = (int) round(p0 + p1);
}
--------------------------------------------------------------------------------
	 - RUN CONFIGURATION:
		Dimensions (1):25
		Parallelization (1): 4
********************************************************************************
```

## Finally
To allow for memory deallocation, once the GPU processing is done, clear the FancyJCL state by calling
```java
FancyJCLManager.clear();
```

# In place operations

It is possible to use an input that is also an output. For example:

```java
stage.setInputs(Map.of("data", data, "kConstant", kConstant));
stage.setOutputs(Map.of("data", data));
```

# Multiple stage execution
It is possible to execute several stages and connect an output of one stage with the input of another. For example:

```java
Stage power = new Stage();
power.setKernelSource("aux[d0] = array[d0] * array[d0];");
power.setInputs(Map.of("array", array));
power.setOutputs(Map.of("aux", aux));
power.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{1}));

Stage threshold = new Stage();
threshold.setKernelSource("output[d0] = (aux[d0] > 25.0f)? 1.0f : 0.0f;");
threshold.setInputs(Map.of("aux", aux));
threshold.setOutputs(Map.of("output", output));
threshold.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{1}));

power.syncInputsToGPU();
power.run();
threshold.run();
threshold.syncOutputsToCPU();
```
Note that we are explicitly stating when to synchronize in this example. This is equivalent:
```java
power.syncInputsToGPU();
power.run();
threshold.runSync();
```
And if we are using DirectBuffers, this is the only needed code:
```java
power.run();
threshold.runSync();
```


# Additional notes
- Although the language in this tutorial is Java, Kotlin can also be used.
