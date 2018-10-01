package com.dzegel.DynamockServer

import org.scalamock.handlers.CallHandler

import scala.language.implicitConversions

package object testUtil {
  implicit def optionExceptionToEitherConversion(optionException: Option[Exception]): Either[Exception, Unit] = optionException match {
    case Some(exception) => Left(exception)
    case None => Right(Unit)
  }

  implicit class RichCallHandler[R](callHandler: CallHandler[R]) {
    def respondsWith(exceptionOrReturnValue: Either[Exception, R]): Unit = exceptionOrReturnValue match {
      case Right(returnValue) => callHandler.returning(returnValue)
      case Left(ex) => callHandler.throwing(ex)
    }
  }

}
