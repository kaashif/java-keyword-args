package org.example;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
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

            if (annotatedElements.isEmpty()) {
                continue;
            }

            TypeElement annotatedClass = (TypeElement) annotatedElements.stream().findFirst().get();
            String className = String.valueOf(annotatedClass.getQualifiedName());
            String builderClassName = className + "Builder";

            Map<String, String> memberToType = new HashMap<>();

            annotatedClass
                    .getEnclosedElements()
                    .forEach(element -> {
                        if (element.getKind().isField()) {
                            memberToType.put(String.valueOf(element.getSimpleName()), element.asType().toString());
                        }
                    });

            try {
                writeBuilderFile(builderClassName, memberToType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    private void writeBuilderFile(String builderClassName, Map<String, String> memberToType) throws IOException {
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(builderClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("aa");
        }
    }
}
