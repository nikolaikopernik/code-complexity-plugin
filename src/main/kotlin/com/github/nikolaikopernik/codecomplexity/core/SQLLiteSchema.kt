package com.github.nikolaikopernik.codecomplexity.core

import org.ktorm.schema.Table
import org.ktorm.schema.bytes
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object Examples : Table<Nothing>("examples") {
    val id = varchar("id").primaryKey()
    val lang = varchar("lang")
    val complexity = int("complexity")
    val body = bytes("body")
}

