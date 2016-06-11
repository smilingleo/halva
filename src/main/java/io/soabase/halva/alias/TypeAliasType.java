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
package io.soabase.halva.alias;

import io.soabase.halva.any.AnyType;
import java.util.function.Function;

public final class TypeAliasType<T, A extends T>
{
    private final AnyType<T> realType;
    private final AnyType<A> aliasType;
    private final Function<T, A> wrapper;

    public TypeAliasType(AnyType<T> realType, AnyType<A> aliasType, Function<T, A> wrapper)
    {
        this.realType = realType;
        this.aliasType = aliasType;
        this.wrapper = wrapper;
    }

    public AnyType<T> getRealType()
    {
        return realType;
    }

    public AnyType<A> getAliasType()
    {
        return aliasType;
    }

    public A wrap(T instance)
    {
        return wrapper.apply(instance);
    }
}
