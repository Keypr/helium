package com.stanfy.helium.handler.codegen.objectivec.entity;

import com.stanfy.helium.handler.Handler
import com.stanfy.helium.handler.codegen.BaseGenerator
import com.stanfy.helium.handler.codegen.objectivec.entity.builder.ObjCDefaultFileStructureBuilder
import com.stanfy.helium.handler.codegen.objectivec.entity.builder.ObjCDefaultProjectBuilder
import com.stanfy.helium.handler.codegen.objectivec.entity.builder.ObjCPropertyNameTransformer
import com.stanfy.helium.handler.codegen.objectivec.entity.builder.ObjCTypeTransformer
import com.stanfy.helium.handler.codegen.objectivec.entity.httpclient.urlsession.ObjCHTTPClientGenerator
import com.stanfy.helium.handler.codegen.objectivec.entity.mapper.mantle.ObjCMantleMappingsGenerator
import com.stanfy.helium.handler.codegen.objectivec.entity.mapper.sfobjectmapping.ObjCSFObjectMappingsGenerator
import com.stanfy.helium.model.Project
import java.io.File

/**
 * Created by ptaykalo on 8/25/14.
 */
class ObjCEntitiesGenerator(outputDirectory: File?, options: ObjCEntitiesOptions?) : BaseGenerator<ObjCEntitiesOptions>(outputDirectory, options), Handler {

  private val objCTypeTransformer = ObjCTypeTransformer()
  private val objCPropertyNameTransformer = ObjCPropertyNameTransformer()

  private val projectBuilder = ObjCDefaultProjectBuilder(objCTypeTransformer, objCPropertyNameTransformer)
  private val client = ObjCHTTPClientGenerator(objCTypeTransformer, objCPropertyNameTransformer)

  override fun handle(project: Project?) {

    val objCProject = projectBuilder.build(project!!, options)
    client.generate(objCProject, project, options)

    // Generate mappings
    val mapper = mapperFromOptions(options)
    mapper?.generate(objCProject, project, options)

    val fileStructureBuilder = ObjCDefaultFileStructureBuilder()
    val filesStructure = fileStructureBuilder.build(objCProject.classesTree)
    ObjCProjectGenerator(outputDirectory, filesStructure).generate()
  }

  private fun mapperFromOptions(options: ObjCEntitiesOptions?): ObjCProjectStructureGenerator? {
    return when (options?.mappingsType) {
      ObjCMappingOption.MANTLE -> ObjCMantleMappingsGenerator()
      ObjCMappingOption.SFMAPPING -> ObjCSFObjectMappingsGenerator()
      else -> null
    }
  }

}
