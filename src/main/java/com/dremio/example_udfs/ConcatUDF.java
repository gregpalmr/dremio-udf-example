/*
 * Copyright (C) 2017-2021 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.example_udfs;

import javax.inject.Inject;

import org.apache.arrow.memory.ArrowBuf;
import org.apache.arrow.vector.holders.VarCharHolder;

import com.dremio.exec.expr.SimpleFunction;
import com.dremio.exec.expr.annotations.FunctionTemplate;
import com.dremio.exec.expr.annotations.FunctionTemplate.NullHandling;
import com.dremio.exec.expr.annotations.Output;
import com.dremio.exec.expr.annotations.Param;

public class ConcatUDF {

  @FunctionTemplate(
    name = "concat_udf",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = NullHandling.NULL_IF_NULL)

  public static class ExampleConcatUDF implements SimpleFunction {

    @Param private VarCharHolder left;

    @Param private VarCharHolder right;

    @Output private VarCharHolder out;

    @Inject private ArrowBuf buffer;

    @Override
      public void setup() {
      // Show an example of how to output to STDOUT/STDERR in Dremio server.out log file
      // These will be logged to the /var/log/dremio/server.out log file
      System.out.println("STDOUT: Calling setup() in concat_udf ");
      System.err.println("STDERR: Calling setup() in concat_udf ");

    }

    @Override
    public void eval() {
      System.out.println("STDOUT: Calling eval() in concat_udf ");

      final int bytesLeftArg = left.end - left.start;
      final int bytesRightArg = right.end - right.start;
      final int finalLength = bytesLeftArg + bytesRightArg;
  
      out.buffer = buffer = buffer.reallocIfNeeded(finalLength);
      out.start = 0;
      out.end = finalLength;
  
      left.buffer.getBytes(left.start, out.buffer, 0, bytesLeftArg);
      right.buffer.getBytes(right.start, out.buffer, bytesLeftArg, bytesRightArg);
    }
  }
}
