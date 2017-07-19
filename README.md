## Description

This project shows how to use some nifty libraries together.

In the project one service is defined, which determines code of country
based on the client ip-address.

The process of determining code of country is request to external web-services
that are providing such service. In particular, the request is sending
to `freegeoip.net` and `ip-api.com` sites. This services provides a REST API.

Communicating with external web services is done with `akka-http` library.

To decrease the response time, the service is requesting all services
simultaneously. The first response will be a result. If an error occurred
or timeout has occurs, then will be returned fallback code. To achieve such
behaviour the `akka-stream` is used.

The implementation provides command-line interface that is provided
by `scopt` library.

To parse JSON response is using `json4s` library.

## How it works

There are main requirements to the service:

1. Parallel requesting to multiple services;
2. Return the first ready result as soon as possible;
3. Return the known result when all services are not accessible
within some timeout;
4. Immediately return known fallback value when all services were failed.

The picture below shows a custom graph which fit the requirements above.

![MergerWithFallbackAndTimeout](https://raw.githubusercontent.com/Chicker/references/master/infobyipextractor/MergerWithFallbackAndTimeout.png)

Each input of the graph is passing via `Flow` that will recover upstream stage
if it will fail. Recovered value it is fallback value.

First ready response from the services is passed to the `Merge` stage.
Next stage is a `Filter`, that prevent passing fallback values if there is a right result.

`OrElse` stage is need when all services were failed. In that case
the previous `Filter` stage will not passing any fallback values and therefore
an elements will be taken from the secondary flow from the OrElse stage,
it will be fallback value.

In order to return fallback value after timeout is used Merge with the parameter
 `eagerComplete = true`. To one of the inputs the `Future` is connected
 that will be completed after some timeout.

## Run

To produce a standalone jar-file use following command: `sbt assembly`.
Run this file as normal jar-file.
