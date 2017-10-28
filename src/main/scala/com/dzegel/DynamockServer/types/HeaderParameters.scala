package com.dzegel.DynamockServer.types

import com.dzegel.DynamockServer.registry.HeaderSet

case class HeaderParameters(included: HeaderSet, excluded: HeaderSet)
