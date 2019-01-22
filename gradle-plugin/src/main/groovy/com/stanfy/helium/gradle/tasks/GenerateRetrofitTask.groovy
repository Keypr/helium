package com.stanfy.helium.gradle.tasks

import com.stanfy.helium.handler.codegen.java.retrofit2.Retrofit2GeneratorOptions
import com.stanfy.helium.handler.codegen.java.retrofit2.Retrofit2InterfaceGenerator

/**
 * Task for generating Retrofit interfaces from a specification.
 */
class GenerateRetrofitTask extends BaseHeliumTask<Retrofit2GeneratorOptions> {

  @Override
  protected void doIt() {
    helium.processBy new Retrofit2InterfaceGenerator(output, options)
  }

}
