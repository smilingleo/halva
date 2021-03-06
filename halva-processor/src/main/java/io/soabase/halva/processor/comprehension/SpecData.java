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
package io.soabase.halva.processor.comprehension;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import io.soabase.halva.any.AnyVal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class SpecData
{
    final List<TypeVariableName> typeVariableNames;
    final ParameterizedTypeName parameterizedMonadicName;
    final ParameterizedTypeName anyName;
    final TypeVariableName monadicTypeName;

    SpecData(MonadicSpec spec)
    {
        typeVariableNames = IntStream.range(0, spec.getMonadElement().getTypeParameters().size())
            .mapToObj(i -> TypeVariableName.get(Character.toString((char)('A' + i))))
            .collect(Collectors.toList());

        int monadicParameterPosition = spec.getAnnotationReader().getInt("monadicParameterPosition");
        if ( (monadicParameterPosition < 0) || (monadicParameterPosition >= typeVariableNames.size()) )
        {
            monadicParameterPosition = typeVariableNames.size() - 1;
        }
        monadicTypeName = typeVariableNames.get(monadicParameterPosition);
        parameterizedMonadicName = ParameterizedTypeName.get(ClassName.get(spec.getMonadElement()), typeVariableNames.toArray(new TypeName[typeVariableNames.size()]));
        anyName = ParameterizedTypeName.get(ClassName.get(AnyVal.class), monadicTypeName);
    }
}
