package com.stanfy.helium.handler.codegen.swift.entity.entities

data class SwiftProperty(val name: String, val type: SwiftEntity, val originalName: String) {
  constructor(name: String, type: SwiftEntity) : this(name, type, name)
}