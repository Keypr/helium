package com.stanfy.helium.handler.codegen.java.retrofit2;

import com.stanfy.helium.handler.codegen.java.JavaGeneratorOptions;
import com.stanfy.helium.model.ServiceMethod;
import com.stanfy.helium.internal.utils.Names;

/**
 * Options for Retrofit generator.
 */
public class Retrofit2GeneratorOptions extends JavaGeneratorOptions {

  /** Entities package. */
  private String entitiesPackage;

  /** Whether to use service method names to generate java method names. */
  private boolean useMethodNames;

  /** Whether to wrap response types in rx.Observable generic. */
  private boolean useRxObservables;

  public static Retrofit2GeneratorOptions defaultOptions(final String packageName) {
    Retrofit2GeneratorOptions options = new Retrofit2GeneratorOptions();
    options.setPackageName(packageName);
    options.setPrettifyNames(true);
    options.setUseMethodNames(true);
    options.setUseRxObservables(false);
    return options;
  }

  public void setEntitiesPackage(final String entitiesPackage) {
    this.entitiesPackage = entitiesPackage;
  }

  public String getEntitiesPackage() {
    return entitiesPackage;
  }

  public boolean isUseMethodNames() {
    return useMethodNames;
  }

  public void setUseMethodNames(final boolean useMethodNames) {
    this.useMethodNames = useMethodNames;
  }

  public boolean isUseRxObservables() {
    return useRxObservables;
  }

  public void setUseRxObservables(final boolean useRxObservables) {
    this.useRxObservables = useRxObservables;
  }

  String getMethodName(final ServiceMethod method) {
    if (!useMethodNames || method.getName() == null) {
      return getName(method);
    }
    String name = Names.canonicalName(method.getName());
    if (isPrettifyNames()) {
      String res = Names.prettifiedName(name);
      name = String.valueOf(Character.toLowerCase(res.charAt(0)));
      if (res.length() > 1) {
        name = name.concat(res.substring(1));
      }
    }
    return name;
  }
}
