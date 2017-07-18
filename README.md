## How it works

There are main requirements to the service:

1. Parallel requesting to multiple services;
2. Return the first ready result as soon as possible;
3. Return the known result when all services are not accessible
within some timeout;
4. Immediately return known fallback value when all services were failed.

The picture below shows a custom graph which fit the requirements above.

Each input of the graph is passing via `Flow` that will recover upstream stage if it will fail. Recovered value it is fallback value.

First ready response from the services is passed to the `Merge` stage. Next stage is a `Filter`, that prevent passing fallback values if there is a right result.

`OrElse` stage is need when all services were failed. In that case the Filter stage will not passing any fallback values and therefore an elements will be taken from the secondary flow from the OrElse stage, it will be fallback value.

In order to return fallback value after timeout is used Merge with the parameter `eagerComplete = true`. To one of the inputs the `Future` is connected that will be completed after some timeout.
