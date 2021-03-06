package com.landoop.data.generator.config

case class DataGeneratorConfig(brokers: String,
                               schemaRegistry: String,
                               pauseBetweenRecordsMs: Long,
                               defaultSchemaRegistry: Boolean)
