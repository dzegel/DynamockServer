# Dynamock Server

## Overview
A mock-server designed to mimic any api. 
Simply setup an API expectation and response and then when an API call matching the registered expectation is made, Dynamock Server responds with the registered response.

###### Use Cases

- Testing: Integration test, an application that relies on web APIs, with a classic unit-test mocking experience.
- Development: Develop an application that relies on web APIs that are themselves in development or currently inaccessible. 

###### Basic Usage
When designing automated tests for a service with external web dependencies simply:
1. [Spin-up](#deployment) a Dynamock Server instance.
1. Configure the hosts and ports for the dependent services on the service under test, to point to the Dynamock Server.
1. Setup the expected API calls with desired responses. (see [PUT /expectations](#put-dynamock-path-baseexpectations) or [POST /expectations-suite/load](#post-dynamock-path-baseexpectations-suiteload))
1. Run your tests, i.e. make http requests to Dynamock Server as if it were the dependent service of interest. When a request matches an expectation that is setup, Dynamock Server will respond with the registered response. 

## Deployment
- Ensure Java 8 or higher is installed.
- Download the JAR file of the latest [release](releases/README.md).
- Run `java -jar DynamockServer-x.y.z.jar [-http.port=:<port-number>] [-dynamock.path.base=<dynamock-path-base>]`, where `x.y.z` is the version number.
The arguments are as follows:
    - **http.port**: An integer in the range [2, 65534], prefixed with `:`, specifying the http port the server runs on.
    For example, providing `-http.port=:1234` deploys a Dynamock instance listening on port `1234`.
    If not provided this value defaults to `:8888`. This feature can be used to deploy multiple Dynamock Server instances for different consumers, to avoid collisions. 
    - **dynamock.path.base**: This value prefixes Dynamock API url-paths.
    For example, `-dynamock.path.base=dynamock/test` or `-dynamock.path.base=/dynamock/test` both result in a net url path `/dynamock/test/expectations` for the Dynamock API url-path `<dynamock-path-base>/expectations`.
    If not provided the net url-path would be `/expectations`.
    This feature can be used to avoid collisions on mocked http requests and the dynamock API.  

## Mocked API
Any API call made to Dynamock Server is included in the Mocked API, except for API calls that would collide with the [Dynamock API](#dynamock-api).
To avoid collisions between the Mocked API and the [Dynamock API](#dynamock-api), set the `dynamock.path.base` argument to a unique value that will not collide on any APIs being mocked.

Given an API request that positively matches a registered expectation, the Mocked API will respond with the response registered with the expectation.
See [PUT /expectations](#put-dynamock-path-baseexpectations) or [POST /expectations-suite/load](#post-dynamock-path-baseexpectations-suiteload) for registering expectations.

Given an API request that does not positively match a registered expectation, the Mocked API responds with a `551` status code along with details of the specific request made to the Mocked API.

The Mocked API responds with a `550` error code for internal server errors, in order not to collide with more common `500` errors.

## Dynamock API

### PUT `<dynamock-path-base>/expectations`
Setup a mocked response by registering an expectation and the response to return when the expectation is positively matched.
An expectation name must also be provided and can be used by the client to associate the returned expectation ids to their respective registered expectations.  

**Content-Type:** application/json

**Request Body Parameters:**
- expectation_responses: 
    - type: Array of [NamedExpectationResponse](#namedexpectationresponse-object) Objects
    - required: true

**Response Body Parameters:**
- expectations_info:
    - type: Array of [ExpectationInfo](#expectationinfo-object) Objects
    - required: true

###### Example Request Body:

    {
        "expectation_responses": [{
            "expectation_name": "Some value that is meaningful to the client",
            "expectation": {
                "method": "POST",
                "path": "/some/url/path",
                "query_parameters": {
                    "some_query_parm": "SomeValue"
                },
                "included_header_parameters": {
                    "some_included_header_param": "SomeValue"
                },
                "excluded_header_parameters": {
                    "some_excluded_header_param": "SomeValue"
                },
                "content": "Some Content (Possibly Json wrapped in a string)"
            },
            "response": {
                "status": 200,
                "content": "Some Content",
                "header_map": {
                    "some_header_param": "SomeValue"
                }
            }
        }]
    }

### DELETE `<dynamock-path-base>/expectations`
Clear registered expectations.

**Content-Type:** application/json

**Request Body Parameters:**
- expectation_ids:
    - type: Array of String
    - required: false
    - description: Ids of the expectations to de-register.
    When `null` or not provided clears all expectations.  

### GET `<dynamock-path-base>/expectations`
List all registered mock setups.

**Response Body Parameters:**
- expectation_responses:
    - type: Array of [ExpectationResponse](#expectationresponse-object) Objects
    - required: true

### POST `<dynamock-path-base>/expectations-suite/store`
Save the state of registered expectations into an expectations-suite that can be restored at a later point in time.

**Query Parameters:**
- suite_name:
    - type: String
    - required: true
    - description: Name of the expectations-suite.

### POST `<dynamock-path-base>/expectations-suite/load`
Restore the state of registered expectations to a stored expectations-suite.

**Query Parameters:**
- suite_name:
    - type: String
    - required: true
    - description: Name of the expectations-suite.

----------------------------------------------

### Definitions
##### NamedExpectationResponse Object:
- properties:
    - expectation_name:
        - type: String
        - required: true
        - description: A value for the client to associate the resulting expectation id with the provided expectation.  
    - expectation:
        - type: [Expectation](#expectation-object) Object
        - required: true
    - response:
        - type: [Response](#response-object) Object
        - required: true
        
##### ExpectationInfo Object:
- properties:
    - expectation_name:
        - type: String
        - required: true
        - description: The value provided in the request with the associated expectation.
    - expectation_id:
        - type: String
        - required: true
        - description: The unique id assigned to the expectation provided in the request.
    - did_overwrite_response:
        - type: boolean
        - required: true
        - description: Indicates if the response provided overwrites a response previously registered with the expectation provided.

##### ExpectationResponse Object:
- properties:  
    - expectation_id:
        - type: String
        - required: true
        - description: The id associated with the expectation. 
    - expectation:
        - type: [Expectation](#expectation-object) Object
        - required: true
    - response:
        - type: [Response](#response-object) Object
        - required: true

##### Expectation Object:
- properties:
    - method:
        - type: string
        - description: The HTTP method of the expected request (i.e. GET, PUT, POST, ect.).
        - required: true
        - matching rule: Exact case-insensitive match with the incoming request. 
    - path:
        - type: string
        - description: The url path of the expected request.
        - required: true
        - matching rule: Exact case-sensitive match.
    - queryParameters:
        - type: map of string to string
        - required: false
        - description: The url/query parameters of the expected request.
        When `null` or not specified it is treated as if an empty map is provided.
        - matching rule: Exact case-sensitive match on all key-value pairs.
    - includedHeaderParameters:
        - type: map of string to string
        - required: false
        - description: Header parameters expected to be included in the request.
        When `null` or not specified it is treated as if an empty map is provided.
        - matching rule: Exact case-sensitive match on all key-value pairs.
        A positive match occurs when all of the specified key-value pairs are found in the request's header map.
    - excludedHeaderParameters:
        - type: map of string to string
        - required: false
        - description: Header parameters expected to be excluded from the request.
        When `null` or not specified it is treated as if an empty map is provided.
        - matching rule: Exact case-sensitive match on all key-value pairs.
        A positive match occurs when none of the specified key-value pairs are found in the request's header map.
    - content:
        - type: string
        - required: false
        - description: The string content expected to be included in the request.
        When `null` or not specified it is treated as if an empty string is provided.
        - matching rule: If the string is valid Json then a positive match occurs on a request with equivalent Json, Json property names are matched case-sensitive.
        When the specified content is not valid Json then a positive match occurs on exact case-sensitive match.
        
##### Response Object:
- properties:
    - status:
        - type: integer
        - description: The Http status code of the response. 
        - required: true
    - content:
        - type: string
        - required: false
        - description: The body of the response. When `null` or not specified it is treated as if an empty string is provided.
    - headerMap:
        - type: map of string to string
        - required: false
        - description: Header parameters to be included in the response's header map. When `null` or not specified it is treated as if an empty map is provided.

## Planned work
- `/expectation-suite/list` endpoint
- `/expectation-suite` DELETE endpoint
- Expectation hit-count support, for validating the number of times an expectation is matched.
- Regex matching on expectation matching.

## Bug Reports / Feature Requests
To report a bug, feature request or any other constructive comment, please create a detailed GitHub issue with a mention of **@dzegel**. 
