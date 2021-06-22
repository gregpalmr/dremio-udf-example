package com.dremio.example_udfs;

import com.dremio.exec.expr.AggrFunction;
import com.dremio.exec.expr.annotations.FunctionTemplate;
import com.dremio.exec.expr.annotations.Output;
import com.dremio.exec.expr.annotations.Param;
import com.dremio.exec.expr.annotations.Workspace;

import org.apache.arrow.vector.holders.BigIntHolder;
import org.apache.arrow.vector.holders.NullableIntHolder;
import org.apache.arrow.vector.holders.NullableVarCharHolder;
import org.apache.arrow.vector.holders.ObjectHolder;

import org.apache.arrow.memory.ArrowBuf;
import javax.inject.Inject;

@FunctionTemplate(
	name = "string_agg_udf",
	scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)

public class StringAggUDF implements AggrFunction {

	@Param
	private NullableVarCharHolder in;

	@Param
	private NullableVarCharHolder separator;

	@Output
	private NullableVarCharHolder out;

	@Inject 
	private ArrowBuf buffer;

	@Workspace
        ObjectHolder value;

	@Workspace
	NullableIntHolder init;

	@Workspace
	BigIntHolder nonNullCount;

	@Workspace
	private NullableVarCharHolder exampleProperty;

	public void setup() {

	    // Show an example of how to output to STDOUT/STDERR in Dremio server.out log file
	    // These will be logged to the /var/log/dremio/server.out log file
	    System.out.println("STDOUT: Calling setup() in string_agg_udf "); 
	    System.err.println("STDERR: Calling setup() in string_agg_udf "); 

	    // Show an example of reading a Java Property passed from the /opt/dremio/conf/dremio-env config file
	    // For this to work, Modify the DREMIO_JAVA_SERVER_EXTRA_OPTS variable like this:
	    //     DREMIO_JAVA_SERVER_EXTRA_OPTS='-Dcom.mycompany.mypackage.myProperty=PROPERTY_VALUE'
	    //    
	    
	    exampleProperty = new NullableVarCharHolder();
	    exampleProperty.isSet = 0;

	    String tmpPropStr = System.getProperty("com.mycompany.mypackage.myProperty");

	    if (tmpPropStr != null) {

	      exampleProperty.isSet = 1;
	      final byte [] byteb = tmpPropStr.getBytes();
	      int finalLength = byteb.length;
	      exampleProperty.buffer = buffer = buffer.reallocIfNeeded((long)finalLength);
       	      exampleProperty.start  = 0;
   	      exampleProperty.end = finalLength;
    	      exampleProperty.buffer.setBytes(0, byteb, 0, finalLength);

	      String tmpStr = com.dremio.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(
			      					exampleProperty.start, 
			      					exampleProperty.end, 
								exampleProperty.buffer);
	      System.out.println("STDOUT: string_agg_udf - Read property: com.mycompany.mypackage.myProperty = '" + tmpStr + "'"); 
	    } else {
	      System.err.println("STDERR: string_agg_udf - Error: com.mycompany.mypackage.myProperty not found"
			      + "\n     Did you set DREMIO_JAVA_SERVER_EXTRA_OPTS='-Dcom.mycompany.mypackage.myProperty=PROPERTY_VALUE'"
			      + "\n     in the /opt/dremio/conf/dremio-env file?"
			      ); 
	    }

	    // Initialize the working variables
	    init = new NullableIntHolder();
	    init.value = 0;
	    nonNullCount = new BigIntHolder();
	    nonNullCount.value = 0;
	    value = new ObjectHolder();
	    com.dremio.exec.expr.fn.impl.ByteArrayWrapper tmp = new com.dremio.exec.expr.fn.impl.ByteArrayWrapper();
	    value.obj = tmp;
	}

	@Override
	public void add() {

		if (in.isSet != 0) {
			nonNullCount.value = 1;
			com.dremio.exec.expr.fn.impl.ByteArrayWrapper tmp = (com.dremio.exec.expr.fn.impl.ByteArrayWrapper) value.obj;

			if (init.value == 0) {
			  init.value = 1;
			  byte[] tempArray = new byte[in.end - in.start];
		          in.buffer.getBytes(in.start, tempArray, 0, in.end - in.start);
		          tmp.setBytes(tempArray);
			} else {
				java.lang.String strVal = new java.lang.String(tmp.getBytes());
				strVal = strVal +
							((separator.isSet != 0)?
								com.dremio.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(separator.start, separator.end, separator.buffer):"") +
								com.dremio.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(in.start, in.end, in.buffer);
				final byte [] bytea = strVal.getBytes(java.nio.charset.Charset.forName("UTF-8"));
				tmp.setBytes(bytea);
			}

			System.out.println("STDOUT: Calling add() in string_agg_udf ");
		}
	}

	@Override
	public void output() {

		System.out.println("STDOUT: Calling output() in string_agg_udf "); 

		if (nonNullCount.value > 0) {
			out.isSet = 1;
			com.dremio.exec.expr.fn.impl.ByteArrayWrapper tmp = (com.dremio.exec.expr.fn.impl.ByteArrayWrapper) value.obj;
			final byte [] bytea = tmp.getBytes();
			int finalLength = bytea.length;
			out.buffer = buffer = buffer.reallocIfNeeded((long)finalLength);
            out.start  = 0;
		    out.end = finalLength;
		    out.buffer.setBytes(0, bytea, 0, finalLength);
		} else {
			out.isSet = 0;
		}
	}

	@Override
	public void reset() {
		value = new ObjectHolder();
	    value.obj = new com.dremio.exec.expr.fn.impl.ByteArrayWrapper();
		init.value = 0;
	    nonNullCount.value = 0;
	}
}
