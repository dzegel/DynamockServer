package com.dzegel.DynamockServer.contract

import com.dzegel.DynamockServer.registry.{Expectation, Response}

case class SetupExpectationPostRequest(expectation: Expectation, response: Response)
