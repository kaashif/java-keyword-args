package org.example;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.sound.sampled.CompoundControl;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@AutoService(Processor.class)
@SupportedAnnotationTypes("org.example.ReorderableStrictBuilder")
@SupportedSourceVersion(SourceVersion.RELEASE_18)
public class ReorderableStrictBuilderProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (var annotation : annotations) {
            var annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

            TypeElement annotatedClass = (TypeElement) annotatedElements.stream().findFirst().get();

            String packageName = processingEnv.getElementUtils().getPackageOf(annotatedClass).toString();

            Map<String, String> memberToType = extractMemberToTypeMap(annotatedClass);

            try {
                writeBuilderFile(annotatedClass.getSimpleName().toString(), packageName, memberToType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    private static Map<String, String> extractMemberToTypeMap(TypeElement annotatedClass) {
        Map<String, String> memberToType = new TreeMap<>();

        annotatedClass
                .getEnclosedElements()
                .forEach(element -> {
                    if (element.getKind().isField()) {
                        memberToType.put(String.valueOf(element.getSimpleName()), element.asType().toString());
                    }
                });
        return memberToType;
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static String makeParameterizedType(String typeName, List<String> parameters) {
        return String.format("%s<%s>", typeName, String.join(", ", parameters));
    }

    private void writeBuilderFile(String simpleName, String packageName, Map<String, String> memberToType) throws IOException {
        String builderSimpleName = simpleName + "Builder";
        JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(builderSimpleName);
        try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
            out.printf("package %s;\n", packageName);

            List<String> typeVariables = memberToType.keySet().stream().map(
                    memberName -> "Has" + capitalize(memberName)
            ).toList();

            out.printf("public class %s {\n", makeParameterizedType(builderSimpleName, typeVariables));
            out.printf("private %s() {}\n", builderSimpleName);
            out.print("private static class True {}\n");
            out.print("private static class False {}\n");

            String allFalseBuilderType = makeParameterizedType(builderSimpleName, typeVariables.stream().map(v -> "False").toList());

            out.printf("""
            public static %s create() {
              return new %s();
            }
            """, allFalseBuilderType, allFalseBuilderType);

            var memberToTypeEntries = memberToType.entrySet().stream().toList();

            for (int i = 0; i < memberToTypeEntries.size(); i++) {
                var entry = memberToTypeEntries.get(i);
                var memberName = entry.getKey();
                var typeName = entry.getValue();
                out.printf("private %s %s;\n", typeName, memberName);

                var returnTypeArgs = new ArrayList<>(typeVariables);
                returnTypeArgs.set(i, "True");

                String returnType = makeParameterizedType(builderSimpleName, returnTypeArgs);

                out.printf("""
                public %s set%s(%s arg) {
                    this.%s = arg;
                    return (%s) this;
                }
                """, returnType, capitalize(memberName), typeName, memberName, returnType);
            }


            String allTrueBuilderType = makeParameterizedType(builderSimpleName, typeVariables.stream().map(v -> "True").toList());

            out.printf("""
            public static %s build(%s builder) {
              return new %s(%s);
            }
            """,
                    simpleName,
                    allTrueBuilderType,
                    simpleName,
                    String.join(", ", memberToType.keySet().stream().map(member -> "builder."+member).toList()));

            out.print("}\n");
        }
    }
}
