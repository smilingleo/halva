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
package io.soabase.halva.implicit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as containing implicit source fields and/or methods. Public/static
 * methods and fields in the class annotated with {@link Implicit} will be candidates
 * for implicit injection
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ImplicitContext
{
    /**
     * Suffix for the Implicit Context generic map. i.e. if the template is "Foo" the class is
     * named "FooCase" (or whatever the suffix is)
     *
     * @return suffix
     */
    String suffix() default "Map";

    /**
     * If a non-empty string, is used instead of {@link #suffix()}. The
     * Implicit Context generic map name is the name of the template <em>minus</em> the value
     * of this attribute.
     *
     * @return string suffix to remove to produce the class name
     */
    String unsuffix() default "_";

    /**
     * If non-empty, the context only applies to the listed implicit classes
     *
     * @return list of classes or []
     */
    Class[] limits() default {};

    /**
     * If non-empty, the context applies to all <em>but</em> the listed implicit classes
     *
     * @return list of classes or []
     */
    Class[] excludes() default {};
}
