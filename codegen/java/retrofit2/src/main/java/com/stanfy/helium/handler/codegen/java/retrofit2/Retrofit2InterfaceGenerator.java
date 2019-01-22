package com.stanfy.helium.handler.codegen.java.retrofit2;

import com.squareup.javawriter.JavaWriter;
import com.stanfy.helium.handler.Handler;
import com.stanfy.helium.handler.codegen.java.BaseJavaGenerator;
import com.stanfy.helium.handler.codegen.java.JavaPrimitiveTypes;
import com.stanfy.helium.internal.utils.Names;
import com.stanfy.helium.model.DataType;
import com.stanfy.helium.model.Dictionary;
import com.stanfy.helium.model.Field;
import com.stanfy.helium.model.FileType;
import com.stanfy.helium.model.FormType;
import com.stanfy.helium.model.HttpHeader;
import com.stanfy.helium.model.Message;
import com.stanfy.helium.model.MultipartType;
import com.stanfy.helium.model.Project;
import com.stanfy.helium.model.Sequence;
import com.stanfy.helium.model.Service;
import com.stanfy.helium.model.ServiceMethod;
import com.stanfy.helium.model.Type;
import org.apache.commons.io.IOUtils;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.squareup.javawriter.JavaWriter.stringLiteral;
import static com.stanfy.helium.internal.utils.Names.canonicalName;
import static com.stanfy.helium.internal.utils.Names.prettifiedName;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Generates a Retrofit interface for a web service.
 */
public class Retrofit2InterfaceGenerator extends BaseJavaGenerator<Retrofit2GeneratorOptions> implements Handler {

  public Retrofit2InterfaceGenerator(final File outputDir, final Retrofit2GeneratorOptions options) {
    super(outputDir, options);
  }

  @Override
  public void handle(final Project project) {
    File dest = getPackageDirectory();
    for (Service service : project.getServices()) {
      ensureServiceNamePresent(service);
      String name = Names.capitalize(getOptions().getName(service));
      File serviceFile = new File(dest, name.concat(EXT_JAVA));
      try {
        write(service, serviceFile, name);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

  }

  private String resolveJavaTypeName(final Type type, final JavaWriter writer, final boolean seq) {
    Type imported = type;
    boolean sequence = seq;
    if (imported instanceof FileType || imported instanceof DataType) {
      return "okhttp3.RequestBody";
    }
    if (imported instanceof MultipartType) {
      return "java.util.Map";
    }

    if (imported instanceof Sequence) {
      sequence = true;
      imported = ((Sequence) imported).getItemsType();
    }
    if (imported instanceof FormType) {
      imported = ((FormType) type).getBase();
    }
    String javaType = getOptions().getJavaTypeName(imported, sequence, false, writer);
    String entitiesPackage = getOptions().getEntitiesPackage();
    if (!type.isPrimitive() && entitiesPackage != null && !entitiesPackage.equals(getOptions().getPackageName())) {
      javaType = entitiesPackage + "." + javaType;
    }
    return writer.compressType(javaType);
  }

  private void addImport(final Set<String> imports, final Type type, final JavaWriter writer) {
    Type importedType = type;
    if (importedType instanceof Sequence) {
      if (getOptions().getSequenceCollectionName() != null) {
        imports.add(getOptions().getSequenceCollectionName());
      }
      importedType = ((Sequence) importedType).getItemsType();
    }
    if (importedType instanceof Dictionary) {
      imports.add("java.util.Map");
      addImport(imports, ((Dictionary) importedType).getKey(), writer);
      addImport(imports, ((Dictionary) importedType).getValue(), writer);
    } else {
      String name = resolveJavaTypeName(importedType, writer, false);
      if (name.contains(".")) {
        imports.add(name);
      }
    }
  }

  private static String getTransformedPath(final ServiceMethod m) {
    return m.getPath().replaceAll("@(\\w+)", "{$1}");
  }

  private void write(final Service service, final File dest, final String serviceClassName) throws IOException {
    JavaWriter writer = new JavaWriter(new OutputStreamWriter(new FileOutputStream(dest), "UTF-8"));
    Retrofit2GeneratorOptions options = getOptions();

    try {
      writer.emitPackage(options.getPackageName());

      HashSet<String> imports = new HashSet<String>();
      imports.add("retrofit2.*");
      imports.add("retrofit2.http.*");
      imports.add("okhttp3.RequestBody");
      imports.add("okhttp3.ResponseBody");
      imports.add("okhttp3.MultipartBody.Part");

      for (ServiceMethod m : service.getMethods()) {
        if (m.getResponse() != null) {
          addImport(imports, m.getResponse(), writer);
        }

        if (m.getBody() != null) {
          processImportsForBody(imports, writer, m);
        }
      }
      if (options.isUseRxObservables()) {
        imports.add("io.reactivex.Observable");
      }

      writer.emitImports(imports);
      writer.emitEmptyLine();

      writer.beginType(serviceClassName, "interface", EnumSet.of(PUBLIC));
      writer.emitEmptyLine();

      if (service.getLocation() != null) {
        writer.emitField("String", "DEFAULT_URL", EnumSet.noneOf(Modifier.class), stringLiteral(service.getLocation()));
        writer.emitEmptyLine();
      }

      for (ServiceMethod m : service.getMethods()) {
        if (m.getBody() != null && m.getBody().isAnonymous()) {
          continue;
        }

        writeJavaDoc(writer, m);

        List<String> constantHeaders = new ArrayList<String>();
        for (HttpHeader header : m.getHttpHeaders()) {
          if (header.isConstant()) {
            constantHeaders.add(stringLiteral(header.getName() + ": " + header.getValue()));
          }
        }
        if (!constantHeaders.isEmpty()) {
          writer.emitAnnotation("Headers", constantHeaders.toArray(new String[constantHeaders.size()]));
        }

        writer.emitAnnotation(m.getType().toString(), stringLiteral(getTransformedPath(m)));

        if (m.hasFormBody()) {
          writer.emitAnnotation("FormUrlEncoded");
        }
        if (m.hasMultipartBody()) {
          writer.emitAnnotation("Multipart");
        }

        String responseType;
        if (m.getResponse() == null
                || m.getResponse() instanceof FileType
                || m.getResponse() instanceof DataType) {
          responseType = "ResponseBody";
        } else {
          responseType = resolveJavaTypeName(m.getResponse(), writer, false);
        }

        if (responseType.equals("ResponseBody") || responseType.equals("okhttp3.ResponseBody")) {
          writer.emitAnnotation("Streaming");
        }

        if (options.isUseRxObservables()) {
          responseType = writer.compressType("Observable<" + responseType + ">");
        } else {
          responseType = writer.compressType("Call<" + responseType + ">");
        }

        writer.beginMethod(responseType, options.getMethodName(m), EnumSet.noneOf(Modifier.class),
            resolveParameters(m, writer), Collections.<String>emptyList());
        writer.endMethod();
        writer.emitEmptyLine();
      }

      writer.endType();
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  private void processImportsForBody(final Set<String> imports, final JavaWriter writer, final ServiceMethod m) {
    if (m.getBody() instanceof MultipartType) {
      final MultipartType multipartType = (MultipartType) m.getBody();
      if (multipartType.isGeneric()) {
        imports.add("java.util.Map");
      } else {
        for (Type type : multipartType.getParts().values()) {
          addImport(imports, type, writer);
        }
      }
    }
    // Form type is an anonymous wrapper - don't import it.
    if ((m.getBody() instanceof FormType)) {
      return;
    }

    addImport(imports, m.getBody(), writer);
  }

  private void writeJavaDoc(final JavaWriter writer, final ServiceMethod m) throws IOException {
    StringBuilder javadoc = new StringBuilder();
    if (m.getName() != null) {
      javadoc.append(m.getName());
      if (javadoc.length() > 0 && javadoc.charAt(javadoc.length() - 1) != '.') {
        javadoc.append('.');
      }
    }
    if (m.getDescription() != null) {
      if (javadoc.length() > 0) {
        javadoc.append("\n");
      }
      javadoc.append(m.getDescription());
    }
    if (javadoc.length() > 0) {
      writer.emitJavadoc(javadoc.toString());
    }
  }

  private String getJavaType(final Type type, final JavaWriter writer, final boolean sequence) {
    String name = resolveJavaTypeName(type, writer, sequence);
    if (type instanceof DataType) {
      return writer.compressType(name);
    }
    return name;
  }

  private List<String> resolveParameters(final ServiceMethod m, final JavaWriter writer) {
    ArrayList<String> res = new ArrayList<String>();
    if (m.hasRequiredParametersInPath()) {
      for (String pp : m.getPathParameters()) {
        res.add("@Path(" + stringLiteral(pp) + ") String");
        res.add(getOptions().getSafeParameterName(pp));
      }
    }

    if (m.hasRequiredHeaders()) {
      for (HttpHeader header : m.getHttpHeaders()) {
        if (!header.isConstant()) {
          res.add("@Header(" + stringLiteral(header.getName()) + ") String");
          res.add("header".concat(prettifiedName(canonicalName(header.getName()))));
        }
      }
    }

    if (m.getParameters() != null) {
      for (Field f : m.getParameters().getActiveFields()) {
        res.add("@Query(\"" + f.getName() + "\") " + parameterTypeName(f, writer));
        res.add(getOptions().getSafeParameterName(f.getCanonicalName()));
      }
    }

    if (m.getBody() != null) {
      if (m.getBody() instanceof FormType) {
        final Message message = ((FormType) m.getBody()).getBase();
        // We should think on including parent message fields.
        // MB message.getAllFields() ?
        for (Field f : message.getActiveFields()) {
          res.add("@Field(\"" + f.getName() + "\") " + getJavaType(f.getType(), writer, f.getSequence()));
          res.add(getOptions().getSafeParameterName(f.getCanonicalName()));
        }
      } else if (m.getBody() instanceof MultipartType) {
        addMultipartBody(writer, res, (MultipartType) m.getBody());
      } else {
        res.add("@Body " + getJavaType(m.getBody(), writer, false));
        res.add("body");
      }
    }

    return res;
  }

  private String parameterTypeName(Field field, JavaWriter writer) {
    if (!field.isRequired() && !field.isSequence() && field.getType().isPrimitive()
        && !getOptions().getCustomPrimitivesMapping().containsKey(field.getType().getName())) {
      // Box the primitive.
      Class<?> primitive = JavaPrimitiveTypes.javaClass(field.getType());
      if (primitive != null) {
        return writer.compressType(JavaPrimitiveTypes.box(primitive).getCanonicalName());
      }
    }
    return getJavaType(field.getType(), writer, field.getSequence());
  }

  private void addMultipartBody(final JavaWriter writer, final ArrayList<String> res, final MultipartType body) {
    if (body.isGeneric()) {
      res.add("@PartMap Map<String, Object>");
      res.add("parts");
    } else {
      for (String name : body.getParts().keySet()) {
        res.add(String.format("@Part(\"%s\") %s", name, getJavaType(body.getParts().get(name), writer, false)));
        res.add(Names.canonicalName(name));
      }
    }
  }

}
