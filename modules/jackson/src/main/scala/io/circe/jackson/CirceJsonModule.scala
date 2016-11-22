package io.circe.jackson

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.{
  BeanDescription,
  DeserializationConfig,
  JavaType,
  JsonSerializer,
  SerializationConfig
}
import com.fasterxml.jackson.databind.Module.SetupContext
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.Serializers
import io.circe.ast.Json

final object CirceJsonModule extends SimpleModule("CirceJson", Version.unknownVersion()) {
  override final def setupModule(context: SetupContext): Unit = {
    context.addDeserializers(
      new Deserializers.Base {
        override final def findBeanDeserializer(
          javaType: JavaType,
          config: DeserializationConfig,
          beanDesc: BeanDescription
        ) = {
          val klass = javaType.getRawClass
          if (classOf[Json].isAssignableFrom(klass) || klass == Json.JNull.getClass) {
            new CirceJsonDeserializer(config.getTypeFactory, klass)
          } else null
        }
      }
    )

    context.addSerializers(
      new Serializers.Base {
        override final def findSerializer(
          config: SerializationConfig,
          javaType: JavaType,
          beanDesc: BeanDescription
        ) = {
          val ser: Object = if (classOf[Json].isAssignableFrom(beanDesc.getBeanClass)) {
            CirceJsonSerializer
          } else null

          ser.asInstanceOf[JsonSerializer[Object]]
        }
      }
    )
  }
}
