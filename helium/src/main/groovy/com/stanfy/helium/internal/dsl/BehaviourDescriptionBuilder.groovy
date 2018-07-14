package com.stanfy.helium.internal.dsl

import com.stanfy.helium.internal.model.tests.BehaviorDescriptionContainer
import com.stanfy.helium.internal.model.tests.BehaviourDescription
import groovy.transform.CompileStatic

/**
 * Builds a new behaviour description adding it to a service or project.
 */
@CompileStatic
class BehaviourDescriptionBuilder {

  private final String name
  private final BehaviorDescriptionContainer target
  private final ProjectDsl project
  private final boolean specificTarget

  BehaviourDescriptionBuilder(String name, BehaviorDescriptionContainer target, ProjectDsl project,
                              boolean specificTarget) {
    this.name = name
    this.target = target
    this.project = project
    this.specificTarget = specificTarget
  }

  void spec(Closure<Void> action) {
    def check = new BehaviourDescription(name: name, action: action, project: project)
    target.addBehaviourDescription(check)
    if (specificTarget) {
      project.addSpecificCheckTarget(check)
    }
  }

}
