/**
 * Copyright 2016 Jordan Zimmerman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.soabase.halva.processor.caseclass;

import com.squareup.javapoet.*;
import io.soabase.halva.processor.Constants;
import io.soabase.halva.processor.Environment;
import io.soabase.halva.tuple.ClassTuple;
import io.soabase.halva.tuple.Tuple;
import io.soabase.halva.tuple.details.Tuple0;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Templates
{
    private final Initializers initializers;

    Templates(Environment environment)
    {
        initializers = new Initializers(environment);
    }

    void addField(CaseClassItem item, TypeSpec.Builder builder, TypeName type, boolean makeFinal, boolean makeVolatile, boolean json)
    {
        TypeName localType;
        if ( makeFinal )
        {
            localType = type;
        }
        else
        {
            localType = item.hasDefaultValue() ? type.box() : type;
        }
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(localType, item.getName(), Modifier.PRIVATE);
        if ( makeFinal )
        {
            fieldBuilder.addModifiers(Modifier.FINAL);
        }
        if ( makeVolatile )
        {
            fieldBuilder.addModifiers(Modifier.VOLATILE);
        }
        if ( json && !checkParentJsonAnnotations(item.getElement(), fieldBuilder) )
        {
            AnnotationSpec annotationSpec = AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.annotation", "JsonProperty")).build();
            fieldBuilder.addAnnotation(annotationSpec);
        }
        builder.addField(fieldBuilder.build());
    }

    void addGetter(CaseClassItem item, TypeSpec.Builder builder, TypeName type, Modifier... modifiers)
    {
        MethodSpec methodSpec = MethodSpec
            .methodBuilder(item.getName())
            .returns(type)
            .addModifiers(modifiers)
            .addAnnotation(Override.class)
            .addStatement("return $L", item.getName())
            .build();

        builder.addMethod(methodSpec);
    }

    void addSetter(CaseClassItem item, TypeSpec.Builder builder, TypeName type, Modifier... modifiers)
    {
        ParameterSpec parameterSpec = ParameterSpec.builder(type, item.getName()).build();

        MethodSpec methodSpec = MethodSpec
            .methodBuilder(item.getName())
            .returns(TypeName.VOID)
            .addModifiers(modifiers)
            .addParameter(parameterSpec)
            .addStatement("this.$L = $L", item.getName(), item.getName())
            .build();

        builder.addMethod(methodSpec);
    }

    void addBuilderSetter(CaseClassItem item, TypeSpec.Builder builder, TypeName type, TypeName builderClassName, Modifier... modifiers)
    {
        ParameterSpec parameterSpec = ParameterSpec.builder(type, item.getName()).build();

        MethodSpec methodSpec = MethodSpec
            .methodBuilder(item.getName())
            .returns(builderClassName)
            .addModifiers(modifiers)
            .addParameter(parameterSpec)
            .addStatement("this.$L = $L", item.getName(), item.getName())
            .addStatement("return this")
            .build();

        builder.addMethod(methodSpec);
    }

    void addGetterItem(CaseClassItem item, TypeSpec.Builder builder, boolean json)
    {
        TypeName type = ClassName.get(item.getType());
        addField(item, builder, type, true, false, json);
        addGetter(item, builder, type, Modifier.PUBLIC);
    }

    void addSetterItem(CaseClassItem item, TypeSpec.Builder builder, boolean json)
    {
        TypeName type = TypeName.get(item.getType());
        addField(item, builder, type, false, true, json);
        addGetter(item, builder, type, Modifier.PUBLIC);
        addSetter(item, builder, type, Modifier.PUBLIC);
    }

    void addItem(CaseClassItem item, TypeSpec.Builder builder, boolean json)
    {
        if ( item.isMutable() )
        {
            addSetterItem(item, builder, json);
        }
        else
        {
            addGetterItem(item, builder, json);
        }
    }

    void addBuilderSetterItem(CaseClassItem item, TypeSpec.Builder builder, TypeName builderClassName, boolean json)
    {
        TypeName type = TypeName.get(item.getType());
        addField(item, builder, type, false, false, json);
        addBuilderSetter(item, builder, type, builderClassName, Modifier.PUBLIC);
    }

    void addConstructor(CaseClassSpec spec, TypeSpec.Builder builder, boolean makePrivate)
    {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
            .addCode(buildFieldValidation(spec))
            .addModifiers(makePrivate ? Modifier.PRIVATE : Modifier.PROTECTED);
        spec.getItems().stream()
            .forEach(item -> {
                TypeName type = TypeName.get(item.getType());
                constructor.addParameter(item.hasDefaultValue() ? type.box() : type, item.getName());
                constructor.addStatement("this.$L = $L", item.getName(), item.getName());
            });
        builder.addMethod(constructor.build());
    }

    void addTuple(CaseClassSpec spec, TypeSpec.Builder builder)
    {
        Optional<Class<? extends Tuple>> optionalTupleClass = Tuple.getTupleClass(spec.getItems().size());
        boolean hasThisTuple = optionalTupleClass.isPresent();
        Class<? extends Tuple> tupleClass = optionalTupleClass.orElse(Tuple.class);

        TypeName typeName;
        CodeBlock codeBlock;
        if ( hasThisTuple )
        {
            List<TypeName> typeNameList = spec.getItems().stream()
                .map(item -> ClassName.get(item.getType()).box())
                .collect(Collectors.toList());
            typeName = getTupleType(tupleClass, typeNameList);

            String args = spec.getItems().stream()
                .map(item -> item.getName() + "()")
                .collect(Collectors.joining(", "));

            codeBlock = CodeBlock.builder()
                .addStatement("return $T.$L($L)", Tuple.class, Constants.TUPLE_METHOD, args)
                .build();
        }
        else
        {
            typeName = ClassName.get(tupleClass);

            codeBlock = CodeBlock.builder()
                .addStatement("throw new $T($S)", UnsupportedOperationException.class, "Too many arguments for a Tuple")
                .build();
        }

        MethodSpec methodSpec = MethodSpec.methodBuilder("tuple")
            .returns(typeName)
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addCode(codeBlock)
            .build();
        builder.addMethod(methodSpec);
    }

    void addHashCode(CaseClassSpec spec, TypeSpec.Builder builder)
    {
        MethodSpec.Builder hashCodeBuilder = MethodSpec
            .methodBuilder("hashCode")
            .returns(TypeName.INT)
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC);
        boolean firstTime = true;
        for ( CaseClassItem item : spec.getItems() )
        {
            String field;
            if ( item.getType().getKind() == TypeKind.BOOLEAN )
            {
                field = "(" + item.getName() + " ? 1 : 0)";
            }
            else if ( item.getType().getKind() == TypeKind.FLOAT )
            {
                field = "Float.hashCode(" + item.getName() + ")";
            }
            else if ( item.getType().getKind() == TypeKind.DOUBLE )
            {
                field = "Double.hashCode(" + item.getName() + ")";
            }
            else if ( item.getType().getKind() == TypeKind.LONG )
            {
                field = "Long.hashCode(" + item.getName() + ")";
            }
            else if ( item.getType().getKind().isPrimitive() )
            {
                field = item.getName();
            }
            else
            {
                field = item.getName() + ".hashCode()";
            }
            String format = firstTime ? "int result = $L" : "result = 31 * result + $L";
            hashCodeBuilder.addStatement(format, field);
            firstTime = false;
        }
        if ( spec.getItems().size() > 0 )
        {
            hashCodeBuilder.addStatement("return result");
        }
        else
        {
            hashCodeBuilder.addStatement("return super.hashCode()");
        }
        builder.addMethod(hashCodeBuilder.build());
    }

    void addEquals(CaseClassSpec spec, TypeSpec.Builder builder, ClassName className)
    {
        MethodSpec.Builder equalsBuilder = MethodSpec
            .methodBuilder("equals")
            .returns(TypeName.BOOLEAN)
            .addParameter(ParameterSpec.builder(Object.class, "rhsObj").build())
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC);

        equalsBuilder.beginControlFlow("if ( this == rhsObj )")
            .addStatement("return true")
            .endControlFlow();

        equalsBuilder.beginControlFlow("if ( rhsObj == null || getClass() != rhsObj.getClass() )")
            .addStatement("return false")
            .endControlFlow();

        if ( spec.getItems().size() > 0 )
        {
            equalsBuilder.addStatement("$L rhs = ($L)rhsObj", className.simpleName(), className.simpleName());
        }

        spec.getItems().forEach(item -> {
            if ( item.getType().getKind().isPrimitive() )
            {
                equalsBuilder.beginControlFlow("if ( $L != rhs.$L )", item.getName(), item.getName())
                    .addStatement("return false")
                    .endControlFlow();
            }
            else
            {
                equalsBuilder.beginControlFlow("if ( !$L.equals(rhs.$L) )", item.getName(), item.getName())
                    .addStatement("return false")
                    .endControlFlow();
            }
        });
        equalsBuilder.addStatement("return true");
        builder.addMethod(equalsBuilder.build());
    }

    void addDebugString(CaseClassSpec spec, TypeSpec.Builder builder, ClassName className)
    {
        MethodSpec.Builder toStringBuilder = MethodSpec
            .methodBuilder("debugString")
            .returns(String.class)
            .addModifiers(Modifier.PUBLIC);

        toStringBuilder.addCode("return \"$L { \" +\n", className.simpleName());
        spec.getItems().forEach(item -> toDebugStringItem(toStringBuilder, item));
        toStringBuilder.addStatement("'}'");
        builder.addMethod(toStringBuilder.build());
    }

    void addToString(CaseClassSpec spec, TypeSpec.Builder builder, ClassName className)
    {
        MethodSpec.Builder toStringBuilder = MethodSpec
            .methodBuilder("toString")
            .returns(String.class)
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC);

        toStringBuilder.addCode("return \"$L(\" +\n", className.simpleName());
        boolean isFirst = true;
        for ( CaseClassItem item : spec.getItems() )
        {
            toStringItem(toStringBuilder, item, isFirst);
            isFirst = false;
        }
        toStringBuilder.addStatement("')'");
        builder.addMethod(toStringBuilder.build());
    }

    void addObjectInstance(TypeSpec.Builder builder, ClassName className, Optional<List<TypeVariableName>> typeVariableNames)
    {
        TypeName localCaseClassName = getLocalCaseClassName(className, typeVariableNames);
        FieldSpec.Builder fieldSpecBuilder = FieldSpec.builder(className, className.simpleName(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("new $T()", localCaseClassName);
        builder.addField(fieldSpecBuilder.build());
    }

    void addApplyBuilder(CaseClassSpec spec, TypeSpec.Builder builder, ClassName className, Optional<List<TypeVariableName>> typeVariableNames)
    {
        TypeName localCaseClassName = getLocalCaseClassName(className, typeVariableNames);

        List<ParameterSpec> parameters = spec.getItems().stream()
            .map(item -> ParameterSpec.builder(ClassName.get(item.getType()), item.getName()).build())
            .collect(Collectors.toList());

        String arguments = spec.getItems().stream().map(CaseClassItem::getName).collect(Collectors.joining(", "));
        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder()
            .addStatement("return new $L$L($L)", className.simpleName(), getDuck(typeVariableNames), arguments);

        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder(className.simpleName())
            .returns(localCaseClassName)
            .addParameters(parameters)
            .addCode(codeBlockBuilder.build())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        if ( typeVariableNames.isPresent() )
        {
            methodSpecBuilder.addTypeVariables(typeVariableNames.get());
        }
        builder.addMethod(methodSpecBuilder.build());
    }

    void addBuilder(CaseClassSpec spec, TypeSpec.Builder builder, ClassName className, boolean json, Optional<List<TypeVariableName>> typeVariableNames)
    {
        TypeName builderClassName = getBuilderClassName(className, typeVariableNames);
        TypeSpec typeSpec = buildBuilderClass(spec, className, builderClassName, json, typeVariableNames);
        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("builder")
            .returns(builderClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addStatement("return new Builder$L()", getDuck(typeVariableNames));

        if ( typeVariableNames.isPresent() )
        {
            methodSpecBuilder.addTypeVariables(typeVariableNames.get());
        }

        builder.addMethod(methodSpecBuilder.build());
        builder.addType(typeSpec);
    }

    TypeName getBuilderClassName(ClassName className, Optional<List<TypeVariableName>> typeVariableNames)
    {
        ClassName rawClassname = className.nestedClass("Builder");
        if ( typeVariableNames.isPresent() )
        {
            return ParameterizedTypeName.get(rawClassname, typeVariableNames.get().toArray(new TypeName[typeVariableNames.get().size()]));
        }
        return rawClassname;
    }

    void addCopy(TypeSpec.Builder builder, ClassName className, Optional<List<TypeVariableName>> typeVariableNames)
    {
        TypeName builderClassName = getBuilderClassName(className, typeVariableNames);
        MethodSpec copySpec = MethodSpec
            .methodBuilder("copy")
            .returns(builderClassName)
            .addCode(CodeBlock.builder().addStatement("return new Builder$L(this)", getDuck(typeVariableNames)).build())
            .addModifiers(Modifier.PUBLIC)
            .build();
        builder.addMethod(copySpec);
    }

    void addClassTuple(CaseClassSpec spec, TypeSpec.Builder builder, ClassName className, boolean json)
    {
        String classTupleName = className.simpleName() + Constants.TUPLE_METHOD;
        ClassName tupleClassName = ClassName.get(Tuple.class);
        ClassName classTupleClassName = ClassName.get(ClassTuple.class);

        addClassTupleMethod(spec, builder, classTupleName, tupleClassName, classTupleClassName);
        addClassTuplable(spec, builder, classTupleName, json);
    }

    private void addClassTuplable(CaseClassSpec spec, TypeSpec.Builder builder, String classTupleName, boolean json)
    {
        String arguments = IntStream.range(0, spec.getItems().size())
            .mapToObj(i -> "\"\"")
            .collect(Collectors.joining(", "));
        FieldSpec.Builder fieldSpecBuilder = FieldSpec.builder(Class.class, "classTuplableClass", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$L($L).getClass()", classTupleName, arguments);

        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("getClassTuplableClass")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(Class.class)
            .addCode(CodeBlock.builder().addStatement("return classTuplableClass").build())
            ;
        if ( json )
        {
            methodSpecBuilder.addAnnotation(ClassName.get("com.fasterxml.jackson.annotation", "JsonIgnore"));
        }
        builder.addMethod(methodSpecBuilder.build());
        builder.addField(fieldSpecBuilder.build());
    }

    private void addClassTupleMethod(CaseClassSpec spec, TypeSpec.Builder builder, String classTupleName, ClassName tupleClassName, ClassName classTupleClassName)
    {
        String arguments = IntStream.rangeClosed(1, spec.getItems().size())
            .mapToObj(i -> "_" + i)
            .collect(Collectors.joining(", "));
        CodeBlock codeBlock = CodeBlock.builder()
            .addStatement("return () -> $T.$L($L)", tupleClassName, Constants.TUPLE_METHOD, arguments)
            .build();

        List<ParameterSpec> parameters = IntStream.rangeClosed(1, spec.getItems().size())
            .mapToObj(i -> ParameterSpec.builder(Object.class, "_" + i).build())
            .collect(Collectors.toList());

        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder(classTupleName)
            .returns(classTupleClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameters(parameters)
            .addCode(codeBlock)
            ;
        builder.addMethod(methodSpecBuilder.build());
    }

    private void toStringItem(MethodSpec.Builder toStringBuilder, CaseClassItem item, boolean isFirst)
    {
        if ( item.getType().toString().equals(String.class.getName()) )
        {
            toStringBuilder.addCode("\"$L\\\"\" + $L + \"\\\"\" + \n", isFirst ? "" : ", ", item.getName());
        }
        else
        {
            toStringBuilder.addCode("$L$L +\n", isFirst ? "" : "\", \" + ", item.getName());
        }
    }

    private void toDebugStringItem(MethodSpec.Builder toStringBuilder, CaseClassItem item)
    {
        if ( item.getType().toString().equals(String.class.getName()) )
        {
            toStringBuilder.addCode("    \"$L=\\\"\" + $L + \"\\\"; \" +\n", item.getName(), item.getName());
        }
        else
        {
            toStringBuilder.addCode("    \"$L=\" + $L + \"; \" +\n", item.getName(), item.getName());
        }
    }

    private TypeName getTupleType(Class<? extends Tuple> tupleClass, List<TypeName> itemTypes)
    {
        TypeName tupleType;
        if ( Tuple0.class.isAssignableFrom(tupleClass) )
        {
            tupleType = ClassName.get(Tuple0.class);
        }
        else
        {
            tupleType = ParameterizedTypeName.get(ClassName.get(tupleClass), itemTypes.toArray(new TypeName[itemTypes.size()]));
        }
        return tupleType;
    }

    private String getDuck(Optional<List<TypeVariableName>> typeVariableNames)
    {
        return typeVariableNames.isPresent() ? "<>" : "";
    }

    private CodeBlock buildFieldValidation(CaseClassSpec spec)
    {
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        spec.getItems().forEach(item -> initializers.addTo(codeBuilder, spec, item));
        return codeBuilder.build();
    }

    private TypeSpec buildBuilderClass(CaseClassSpec spec, ClassName caseClassName, TypeName builderClassName, boolean json, Optional<List<TypeVariableName>> typeVariableNames)
    {
        TypeName localCaseClassName = getLocalCaseClassName(caseClassName, typeVariableNames);
        CodeBlock.Builder newBuilder = CodeBlock.builder().add("return new $L$L(", caseClassName.simpleName(), getDuck(typeVariableNames));
        String comma = "";
        for ( CaseClassItem item : spec.getItems() )
        {
            newBuilder.add("$L\n    $L", comma, item.getName());
            comma = ", ";
        }
        newBuilder.add("\n);\n");

        MethodSpec.Builder newSpecBuilder = MethodSpec.methodBuilder("build")
            .returns(localCaseClassName)
            .addModifiers(Modifier.PUBLIC)
            .addCode(newBuilder.build())
            ;

        MethodSpec.Builder constructorSpecBuilder = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            ;
        if ( json )
        {
            AnnotationSpec annotationSpec = AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.annotation", "JsonCreator")).build();
            constructorSpecBuilder.addAnnotation(annotationSpec);
        }

        MethodSpec.Builder copyConstructorBuilder = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .addParameter(ParameterSpec.builder(localCaseClassName, "rhs").build());
        spec.getItems().forEach(item -> copyConstructorBuilder.addStatement("$L = rhs.$L", item.getName(), item.getName()));

        TypeSpec.Builder builder = TypeSpec.classBuilder("Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
            .addMethod(constructorSpecBuilder.build())
            .addMethod(copyConstructorBuilder.build())
            .addMethod(newSpecBuilder.build());
        if ( json )
        {
            AnnotationSpec annotationSpec = AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.databind.annotation", "JsonPOJOBuilder"))
                .addMember("withPrefix", "\"\"")
                .build();
            builder.addAnnotation(annotationSpec);
        }
        if ( typeVariableNames.isPresent() )
        {
            builder.addTypeVariables(typeVariableNames.get());
        }

        spec.getItems().forEach(item -> addBuilderSetterItem(item, builder, builderClassName, json));

        return builder.build();
    }

    private TypeName getLocalCaseClassName(ClassName caseClassName, Optional<List<TypeVariableName>> typeVariableNames)
    {
        TypeName localCaseClassName;
        if ( typeVariableNames.isPresent() )
        {
            localCaseClassName = ParameterizedTypeName.get(caseClassName, typeVariableNames.get().toArray(new TypeName[typeVariableNames.get().size()]));
        }
        else
        {
            localCaseClassName = caseClassName;
        }
        return localCaseClassName;
    }

    private boolean checkParentJsonAnnotations(Element element, FieldSpec.Builder fieldBuilder)
    {
        return element.getAnnotationMirrors().stream().filter(annotation -> {
            if ( annotation.getAnnotationType().asElement().toString().equals("com.fasterxml.jackson.annotation.JsonProperty")
                || annotation.getAnnotationType().asElement().toString().equals("com.fasterxml.jackson.annotation.JsonIgnore") )
            {
                AnnotationSpec.Builder annotationSpec = AnnotationSpec.builder(ClassName.get((TypeElement)annotation.getAnnotationType().asElement()));
                annotation.getElementValues().entrySet().forEach(entry ->
                    annotationSpec.addMember(entry.getKey().getSimpleName().toString(), "$S", entry.getValue().getValue()));
                fieldBuilder.addAnnotation(annotationSpec.build());
                return true;
            }
            return false;
        }).count() > 0;
    }
}
