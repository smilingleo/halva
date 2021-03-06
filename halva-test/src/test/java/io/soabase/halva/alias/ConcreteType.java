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

public class ConcreteType<A, B>
{
    private A a;
    private B b;

    public ConcreteType()
    {
        a = null;
        b = null;
    }

    public ConcreteType(A a, B b)
    {
        this.a = a;
        this.b = b;
    }

    public ConcreteType<A, B> copy()
    {
        return new ConcreteType<>(a, b);
    }

    public A getA()
    {
        return a;
    }

    public B getB()
    {
        return b;
    }

    public void setA(A a)
    {
        this.a = a;
    }

    public void setB(B b)
    {
        this.b = b;
    }
}
