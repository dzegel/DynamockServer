package com.dzegel.DynamockServer.server

import com.dzegel.DynamockServer.types.Content
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import com.twitter.finatra.json.modules.FinatraJacksonModule

private[server] object ContentSerializer extends JsonSerializer[Content] {

  override def serialize(content: Content, gen: JsonGenerator, serializers: SerializerProvider): Unit =
    gen.writeString(content.stringValue)

  override val handledType: Class[Content] = classOf[Content]
}

private[server] object MapStringStringSerializer extends JsonSerializer[Set[(String, String)]] {

  override protected def serialize(setStringString: Set[(String, String)], gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeStartObject()
    setStringString.foreach { case (name, value) =>
      gen.writeStringField(name, value)
    }
    gen.writeEndObject()
  }

  override val handledType: Class[Set[(String, String)]] = classOf[Set[(String, String)]]
}

private[server] object CustomModule extends SimpleModule {
  addSerializer(ContentSerializer)
  addSerializer(MapStringStringSerializer)
}

object JacksonModule extends FinatraJacksonModule {
  override protected val additionalJacksonModules: Seq[Module] = CustomModule :: Nil
}
