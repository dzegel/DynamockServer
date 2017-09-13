package com.dzegel.DynamockServer.contract

import com.dzegel.DynamockServer.Registry.{Expectation, Response}

case class SetupExpectationPostRequest(expectation: Expectation, response: Response)
