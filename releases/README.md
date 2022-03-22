# Releases

### Version Number Scheme
Release version notation is made of three integers `x`, `y` and `z` delimited by `.` (i.e. `x.y.z`). Incrementation of version numbers follows the following rules:
- `z` - incremented on **minor** updates such as bug-fixes, optimizations, refactor, ect.
- `y` - incremented on **major** updates such as feature additions.
- `x` - incremented on **breaking** updates such that some feature is not fully backwards compatible.

### Versions

|Version|JAR|Release Notes|
|---|---|---|
|4.0.1|Not Released|<ul><li>Update 551 response property names to match Expectation object property names.</li></ul>|
|[4.0.0](https://github.com/dzegel/DynamockServer/tree/Version_4.0.0)|[Dynamock Server 4.0.0 JAR](DynamockServer-4.0.0.jar)|<ul><li>Register Expectation without Response.</li><li>Deterministic Expectation-Id.</li><li>Updated stored expectation suite serialization.</li><li>Simplified `POST /expectations-suite/load` response.</li><li>Enforce all expectation paths start with `/`.</li><li>Change Expectation `query_parameters` parameter to list instead of map.</li></ul>|
|[3.0.0](https://github.com/dzegel/DynamockServer/tree/Version_3.0.0)|[Dynamock Server 3.0.0 JAR](DynamockServer-3.0.0.jar)|<ul><li>Rename file root from`Dynamock` to `DynamockServer`.</li><li>Handle admin port properly.</li><li>Remove deprecated `did_overwrite_response` property from `POST /expectations-suite/load` response.</li><li>Update default value of `dynamock.path.base` JAR argument to `dynamock`.</li></ul>|
|[2.1.0](https://github.com/dzegel/DynamockServer/tree/Version_2.1.0)|[Dynamock Server 2.1.0 JAR](DynamockServer-2.1.0.jar)|<ul><li>Update `POST /expectation-suite/load` response to include overwritten expectation ids.</li><li>Add `POST /hit-counts/get` and `POST /hit-counts/reset` endpoints.</li></ul> |
|[2.0.1](https://github.com/dzegel/DynamockServer/tree/Version_2.0.1)|[Dynamock Server 2.0.1 JAR](DynamockServer-2.0.1.jar)|<ul><li>Minor updates.</li><li>Memory leak fix.</li></ul>|
|[2.0.0](https://github.com/dzegel/DynamockServer/tree/Version_2.0.0)|[Dynamock Server 2.0.0 JAR](DynamockServer-2.0.0.jar)|<ul><li>Rename `/expectations/store` and `/expectations/load` to `/expectation-suite/store` and `/expectation-suite/load` respectively.</li><li>Verbose 551 error message.</li><li>Rename input argument `expectations.path.base` => `dynamock.path.base`.</li><li>Update `PUT /expectations` and `GET /expectations` endpoints output with expectation ids.</li><li>Targeted expectation deletion.</li></ul>|
|[1.0.0](https://github.com/dzegel/DynamockServer/tree/Version_1.0.0)|[Dynamock Server 1.0.0 JAR](DynamockServer-1.0.0.jar)|Initial Release|
