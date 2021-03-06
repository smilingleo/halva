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
package io.soabase.halva.tuple;

import io.soabase.halva.tuple.details.Tuple2;

/**
 * An alias for Tuple2<A, B>
 */
public class Pair<A, B> extends Tuple2<A, B>
{
    public Pair(A _1, B _2)
    {
        super(_1, _2);
    }

    @SuppressWarnings("MethodNameSameAsClassName")
    public static <A, B> Pair<A, B> Pair(A _1, B _2)
    {
        return new Pair<>(_1, _2);
    }
}
