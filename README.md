# Dynamock Server
A mock-server designed to replicate classic unit-test mocking experience. Setup an API expectation and response and receive the registered response when an API call matching the registered expectation is made. 

###### Usage
When designing automated tests for a service with external web dependencies simply:
1. Spin-up a Dynamock Server instance.
1. Configure the hosts and ports for the dependent services on the service under test, to point to the Dynamock Server.
1. Setup the expected API calls along with desired responses.
1. Optionally tear-down the setup expectations when done testing.

## Dynamock API

### PUT /expectations
Setup a mocked response by registering an expectation and the response to return when the expectation is positively matched. 

**Content-Type:** application/json

**Request Body Parameters:**
- expectation_responses: Array of ExpectationResponse Objects

###### Example Body:

    {
        "expectation_responses": [{
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

### DELETE /expectations
Clear all registered mock setups.

### GET /expectations
List all registered mock setups.

**Response Body Parameters:**
- expectation_responses: Array of ExpectationResponse Objects

### POST /expectations/store
Save the state of registered expectations into an expectations-suite that can be restored at a later point in time.

**Query Parameters:**
- suite_name: Name of the expectations-suite.

### POST /expectations/load
Restore the state of registered expectations to a stored expectations-suite.

**Query Parameters:**
- suite_name: Name of the expectations-suite.

----------------------------------------------

### Definitions
ExpectationResponse Object:
- properties:
    - expectation:
        - type: Expectation Object
        - required: true
    - response:
        - type: Response Object
        - required: true

Expectation Object:
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
        - description: The url/query parameters of the expected request.
        - required: false, when not specified it is treated as if an empty map is provided.
        - matching rule: Exact case-sensitive match on all key-value pairs.
    - includedHeaderParameters:
        - type: map of string to string
        - description: Header parameters expected to be included in the request.
        - required: false, when not specified it is treated as if an empty map is provided.
        - matching rule: Exact case-sensitive match on all key-value pairs. A positive match occurs when all of the specified key-value pairs are found in the request's header map.
    - excludedHeaderParameters:
        - type: map of string to string
        - description: Header parameters expected to be excluded from the request.
        - required: false, when not specified it is treated as if an empty map is provided.
        - matching rule: Exact case-sensitive match on all key-value pairs. A positive match occurs when none of the specified key-value pairs are found in the request's header map.
    - content:
        - type: string
        - description: The string content expected to be included in the request.
        - required: false, when not specified it is treated as if an empty string is provided.
        - matching rule: If the string is valid Json then a positive match occurs on a request with equivalent Json, Json property names are matched case-sensitive. When the specified content is not valid Json then a positive match occurs on exact case-sensitive match.
        
Response Object:
- properties:
    - status:
        - type: integer
        - description: The Http status code of the response. 
        - required: true
    - content:
        - type: string
        - description: The body of the response.
        - required: false, when not specified it is treated as if an empty string is provided.
    - headerMap:
        - type: map of string to string
        - description: Header parameters to be included in the response's header map.
        - required: false, when not specified it is treated as if an empty map is provided.
