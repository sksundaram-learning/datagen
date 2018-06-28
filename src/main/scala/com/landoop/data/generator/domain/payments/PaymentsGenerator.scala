package com.landoop.data.generator.domain.payments

import com.landoop.data.generator.config.DataGeneratorConfig
import com.landoop.data.generator.domain.Generator
import com.landoop.data.generator.json.{JacksonJson, JacksonXml}
import com.landoop.data.generator.kafka.Producers
import com.sksamuel.avro4s.{RecordFormat, ScaleAndPrecision}
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}

import scala.math.BigDecimal.RoundingMode
import scala.util.Random

object PaymentsGenerator extends Generator with StrictLogging {
  private val MerchantIds = (1 to 100).map(_.toLong).toVector
  private val DateFormatter = ISODateTimeFormat.dateTime()

  private def generate[V](topic: String, delay: Long)(thunk: Payment => V)(implicit producer: KafkaProducer[String, V]): Unit = {
    Iterator.continually {
      val index = Random.nextInt(CreditCard.Cards.size)
      val cc = CreditCard.Cards(index)

      val dt = new DateTime().toDateTime(DateTimeZone.UTC)
      val date = DateFormatter.print(dt)

      val left = 10 + Random.nextInt(5000)
      val right = Random.nextInt(100)
      val decimal = BigDecimal(s"$left.$right").setScale(18, RoundingMode.HALF_UP)
      Payment(s"txn${System.currentTimeMillis()}", date, decimal, cc.currency, cc.number, MerchantIds(Random.nextInt(MerchantIds.size)))
    }.foreach { r =>
      val record = new ProducerRecord(topic, r.id, thunk(r))
      producer.send(record)
      Thread.sleep(delay)
    }
  }

  override def avro(topic: String)(implicit config: DataGeneratorConfig): Unit = {
    val props = Producers.getAvroValueProducerProps(classOf[StringSerializer])
    implicit val producer: KafkaProducer[String, GenericRecord] = new KafkaProducer[String, GenericRecord](props)
    implicit val sp = ScaleAndPrecision(18, 38)
    val rf = RecordFormat[Payment]

    logger.info(s"Publishing payments data to '$topic'")
    try {
      generate(topic, config.pauseBetweenRecordsMs)(rf.to)
    }
    catch {
      case t: Throwable =>
        logger.error(s"Failed to publish credit card data to '$topic'", t)
    }
  }

  override def json(topic: String)(implicit config: DataGeneratorConfig): Unit = {
    val props = Producers.getStringValueProducerProps(classOf[StringSerializer])
    implicit val producer: KafkaProducer[String, String] = new KafkaProducer[String, String](props)

    logger.info(s"Publishing payments data to '$topic'")
    try {
      generate(topic, config.pauseBetweenRecordsMs)(JacksonJson.toJson)
    }
    catch {
      case t: Throwable =>
        logger.error(s"Failed to publish credit card data to '$topic'", t)
    }
  }

  override def xml(topic: String)(implicit config: DataGeneratorConfig): Unit = {
    val props = Producers.getStringValueProducerProps(classOf[StringSerializer])
    implicit val producer: KafkaProducer[String, String] = new KafkaProducer[String, String](props)

    logger.info(s"Publishing payments data to '$topic'")
    try {
      generate(topic, config.pauseBetweenRecordsMs)(JacksonXml.toXml)
    }
    catch {
      case t: Throwable =>
        logger.error(s"Failed to publish credit card data to '$topic'", t)
    }
  }
}
