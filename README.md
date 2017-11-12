# DynamockServer

## Dynamock API

### PUT /expectation
Mock an API response by registering an expectation to match on, along with the response to return when the expectation is positively matched. 

**Content-Type:** application/json

**Body Parameters:**
- expectation: Expectation Object
- response: Response Object

**Example:**

    {
        "expectation": {},
        "response": {}
    }

### Definitions
Expectation Object:
- properties:
    - method:
        - type: string
        - description: The HTTP method of the expected request (i.e. GET, PUT, POST, ect.).
        - required: true
        - matching rule: Exact match with the incoming request. 
    - path:
        - type: string
        - description: The url stub of the expected request.
        - required: true
        - matching rule: Exact case-sensitive string match.
    - queryParameters:
        - type: Map of string to string
        - description: The url query parameters of the expected request.
        - required: false, when not specified it is treated as if an empty map is provided.
        - matching rule: Exact case-sensitive match on all key-value pairs.
    - includedHeaderParameters:
        - type: Map of string to string
        - description: Header parameters expected to be included in the request.
        - required: false, when not specified it is treated as if an empty map is provided.
        - matching rule: Exact case-sensitive match on all included key-value pairs. A positive match occures when all of the specified key-value pairs are found in the requests header map.
    - excludedHeaderParameters:
        - type: Map of string to string
        - description: Header parameters expected to be excluded from the request.
        - required: false, when not specified it is treated as if an empty map is provided.
        - matching rule: Exact case-sensitive match on all included key-value pairs. A positive match occures when none of the specified key-value pairs are found in the requests header map.
    - content:
        - type: string
        - description: The request's string content.
        - required: false, when not specified it is treated as if an empty string is provided.
        - matching rule: If the string is valid Json then a positive match occures with a request with equivilant Json. Json property names are matched case-sensitive. When the specified content is not valid Json then a positive match occures on exact case-sensitive string match.
        
Response Object:
- properties:
    - status:
        - type: integer
        - description: The http status code of the response. 
        - required: true
    - content:
        - type: string
        - description: The body of the response.
        - required: false, when not specified it is treated as if an empty string is provided.
    - headerMap:
        - type: Map of string to string
        - description: header parameters to be included in the response's header map.
        - required: false, when not specified it is treated as if an empty map is provided.
