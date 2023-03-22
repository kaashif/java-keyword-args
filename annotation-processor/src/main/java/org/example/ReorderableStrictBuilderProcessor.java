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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            String builderClassName = annotatedClass.getSimpleName() + "Builder";

            Map<String, String> memberToType = extractMemberToTypeMap(annotatedClass);

            try {
                writeBuilderFile(builderClassName, packageName, memberToType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    private static Map<String, String> extractMemberToTypeMap(TypeElement annotatedClass) {
        Map<String, String> memberToType = new HashMap<>();

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

    private void writeBuilderFile(String builderSimpleName, String packageName, Map<String, String> memberToType) throws IOException {
        List<TypeVariableName> typeVariables = memberToType.keySet().stream().map(
                memberName -> TypeVariableName.get("Has" + capitalize(memberName))
        ).toList();

        ClassName builderClassName = ClassName.get(packageName, builderSimpleName);

        TypeSpec.Builder builderClassBuilder = TypeSpec.classBuilder(builderSimpleName)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariables(typeVariables);

        builderClassBuilder.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build()
        );

        TypeSpec trueType = TypeSpec.classBuilder("True").build();
        builderClassBuilder.addType(trueType);

        TypeSpec falseType = TypeSpec.classBuilder("False").build();
        builderClassBuilder.addType(falseType);

        builderClassBuilder.addMethod(
                MethodSpec.methodBuilder("create")
                        .returns(ParameterizedTypeName.get(null, builderClassName, typeVariables.stream().map(v -> )))
                        .build()
        );

        for (var entry : memberToType.entrySet()) {
            var memberName = entry.getKey();
            var typeName = entry.getValue();
            builderClassBuilder.addField(ClassName.bestGuess(typeName), memberName, Modifier.PRIVATE);
        }

        TypeSpec builderClass = builderClassBuilder.build();

        JavaFile javaFile = JavaFile.builder(packageName, builderClass)
                .build();

        JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(builderSimpleName);

        try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
            javaFile.writeTo(out);
        }
    }
}
