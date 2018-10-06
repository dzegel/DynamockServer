# Dynamock Server

- [Overview](#overview)
- [Deployment](#deployment)
- [Mocked API](#mocked-api)
  - [Matched Expectations and Mocked Responses](#matched-expectations-and-mocked-responses)
  - [NonMocked Responses](#nonmocked-responses)
  - [Internal Errors](#internal-errors)
- [Dynamock API](#dynamock-api)
  - [PUT `/expectations`](#put-dynamock-path-baseexpectations)
  - [DELETE `/expectations`](#delete-dynamock-path-baseexpectations)
  - [GET `/expectations`](#get-dynamock-path-baseexpectations)
  - [POST `/expectations-suite/store`](#post-dynamock-path-baseexpectations-suitestore)
  - [POST `/expectations-suite/load`](#post-dynamock-path-baseexpectations-suiteload)
  - [POST `/hit-counts/get`](#post-dynamock-path-basehit-countsget)
  - [POST `/hit-counts/reset`](#post-dynamock-path-basehit-countsreset)
  - [Definitions](#definitions)
     - [NamedExpectationResponse Object](#namedexpectationresponse-object)
     - [ExpectationInfo Object](#expectationinfo-object)
     - [ExpectationResponse Object](#expectationresponse-object)
     - [LoadInfo Object](#loadinfo-object)
     - [LoadInfoExpectationOverwrite](#loadinfoexpectationoverwrite-object) Object
     - [Expectation Object](#expectation-object)
     - [Response Object](#response-object)
- [Planned work](#planned-work)
- [Bug Reports / Feature Requests](#bug-reports--feature-requests)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>

## Overview
A mock-server designed to mimic any API. 
Simply setup an API expectation and response and then when an API call matching the registered expectation is made, Dynamock Server responds with the registered response.

###### Use Cases

- Testing: Integration test an application that relies on web APIs, with a classic unit-test mocking experience.
- Development: Develop an application that relies on web APIs that are themselves in development or currently inaccessible. 

###### Basic Usage
When designing automated tests for a service with external web dependencies simply:
1. [Spin-up](#deployment) a Dynamock Server instance.
1. Configure the url base (i.e. host and port) of the dependent services on the service under test to point to the Dynamock Server.
1. Setup the expected API calls with desired responses. (see [PUT /expectations](#put-dynamock-path-baseexpectations) or [POST /expectations-suite/load](#post-dynamock-path-baseexpectations-suiteload))
1. Run your tests making http requests to Dynamock Server as if it were the dependent service of interest. When a request matches an expectation that is setup, Dynamock Server will respond with the registered response. 

## Deployment
- Ensure Java 8 (or higher) is installed.
- Download the JAR file of the latest [release](releases/README.md).
- Run `java -jar DynamockServer-x.y.z.jar [-http.port=:<port-number>] [-dynamock.path.base=<dynamock-path-base>]`, where `x.y.z` is the release version number.
The arguments are as follows:
    - **http.port**: An integer in the range [2, 65534], prefixed with `:`, specifying the http port the server runs on.
    For example, providing `-http.port=:1234` deploys a Dynamock instance listening on port `1234`.
    If not provided this value defaults to `:8888`.
    This feature can be used to deploy multiple Dynamock Server instances on the same computer for different consumers, to avoid collisions. 
    - **dynamock.path.base**: This value prefixes Dynamock API url-paths.
    For example, `-dynamock.path.base=my/test` or `-dynamock.path.base=/my/test` both result in a net url path `/my/test/expectations` for the Dynamock API url-path `<dynamock-path-base>/expectations`.
    If not provided `dynamock.path.base` defaults to the value `dynamock`, resulting in the net url-path being `/dynamock/expectations`.
    You should only need to use this feature to avoid collisions on mocked http requests and the dynamock API, in the unlikely case that the API being mocked has `dynamock` in its url paths.

## Mocked API
Any API call made to Dynamock Server is included in the Mocked API, except for API calls that would collide with the [Dynamock API](#dynamock-api).

### Matched Expectations and Mocked Responses
When an API request is made, all expectations that positively match the request and that are registered with a response are considered (see [PUT /expectations](#put-dynamock-path-baseexpectations) for registering expectations).
Of those considered, the response of the most constrained expectation is selected to be used for the mocked response.
The most constrained expectation is defined as, the expectation with the greatest number of included and excluded header parameters specified.
In the event that there are multiple equally constrained expectations that positively match the API request, one of those expectations is selected arbitrarily but deterministically.
Additionally, all registered expectations (including ones not registered with a response) that positively match the request have their hit-counts incremented (see [POST /hit-counts/get](#post-dynamock-path-basehit-countsget)).

### NonMocked Responses
Given an API request that does not positively match an expectation registered with a response, the Mocked API responds with a `551` status code along with details of the specific request made to the Mocked API.

### Internal Errors
The Mocked API responds with a `550` error code for internal server errors, in order not to collide with more common `5xx` errors that may be intentionally set in a registered mock response.
If you experience a `550` error we would greatly appreciate it if you would submit a thorough bug report, see [below](#bug-reports--feature-requests) for submission instructions.

## Dynamock API

### PUT `<dynamock-path-base>/expectations`
Setup a mocked response by registering an expectation and the response to return when the expectation is positively matched.
An expectation name is provided to be used by the client to associate the returned expectation-ids to their respective registered expectations.
Previously registered expectations which are identical to expectations provided will have their respective responses overridden with the responses provided and will keep the ids the expectations were originally registered with.

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
The saved state includes the expectation-id, expectation and response but not the hit-count.

**Query Parameters:**
- suite_name:
    - type: String
    - required: true
    - description: Name of the expectations-suite.

### POST `<dynamock-path-base>/expectations-suite/load`
Register expectations stored in an expectations-suite with their original expectation-ids.
Registered expectations which are identical to expectations in the loaded suite will have their ids and responses overridden with the respective values in the suite.
In the event that an expectation-id loaded from the suite is identical to a pre-registered expectation id, the hit-count will retain its value and not be reset.

**Query Parameters:**
- suite_name:
    - type: String
    - required: true
    - description: Name of the expectations-suite.
    
**Response Body Parameters:**
- suite_load_info:
    - type: Array of [LoadInfo](#loadinfo-object) Objects
    - required: true

### POST `<dynamock-path-base>/hit-counts/get`
Get the hit-counts of the specified expectation-ids; where an expectation-id's hit-count is the number of times a request was made that matched the expectation associated with the expectation-id.
If a request matches multiple registered expectations, though only one of their responses' is used for the API response, all of their hit-counts are incremented.

**Content-Type:** application/json

**Request Body Parameters:**
- expectation_ids:
    - type: Array of String
    - required: true
    - description: Ids of the expectations for which to get hit-counts.

**ResponseBody**
- expectation_id_to_hit_count:
    - type: Map of String to Integer
    - required: true
    - description: A map specifying the hit count for all requested registered expectation-ids.
    Non-registered requested expectation-ids are not present.

### POST `<dynamock-path-base>/hit-counts/reset`
Reset hit-counts to 0.

**Content-Type:** application/json

**Request Body Parameters:**
- expectation_ids:
    - type: Array of String
    - required: false
    - description: Ids of the expectations for which to reset hit-counts to 0. 
    When `null` or not provided resets the hit-count for all expectations.
----------------------------------------------

### Definitions
##### NamedExpectationResponse Object:
- properties:
    - expectation_name:
        - type: String
        - required: true
        - description: A value for the client to associate the resulting expectation-id with the provided expectation.
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
        
##### LoadInfo Object:
- properties:
    - expectation_id:
        - type: String
        - required: true
        - description: The unique expectation-id loaded from the expectation suite.
    - overwrite_info:
        - type: [LoadInfoExpectationOverwrite](#loadinfoexpectationoverwrite-object) Object
        - required: false
        - description: When the expectation loaded from the suite matches an expectation previously registered, this object indicates exactly how that expectation was overwritten.

##### LoadInfoExpectationOverwrite Object:
- properties:
    - old_expectation_id
        - type: string
        - required: true
        - description: The expectation-id previously assigned to the expectation loaded from the suite.
    - did_overwrite_response:
        - type: boolean
        - required: true
        - description: Indicates if the response, previously registered with the expectation, is identical to the response loaded from the suite or if it was overwritten.

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
- Regex matching on expectation matching.
- Remove the requirement that an exclusion must have a response.

## Bug Reports / Feature Requests
To report a bug, feature request or any other constructive comment, please create a detailed GitHub issue [here](https://github.com/dzegel/DynamockServer/issues/new) with a mention of **@dzegel**. 
