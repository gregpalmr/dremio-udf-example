package com.dremio.udf;

import com.dremio.exec.expr.AggrFunction;
import com.dremio.exec.expr.annotations.FunctionTemplate;
import com.dremio.exec.expr.annotations.Output;
import com.dremio.exec.expr.annotations.Param;
import com.dremio.exec.expr.annotations.Workspace;

import org.apache.arrow.vector.holders.BigIntHolder;
import org.apache.arrow.vector.holders.NullableIntHolder;
import org.apache.arrow.vector.holders.NullableVarCharHolder;
import org.apache.arrow.vector.holders.ObjectHolder;

//import io.netty.buffer.ArrowBuf;
import org.apache.arrow.memory.ArrowBuf;
import javax.inject.Inject;

@FunctionTemplate(
	name = "string_agg",
	scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
public class StringAgg implements AggrFunction {
	@Inject ArrowBuf buffer;

	@Param
	NullableVarCharHolder in;

	@Param
	NullableVarCharHolder separator;

	@Output
	NullableVarCharHolder out;

	@Workspace
    ObjectHolder value;

	@Workspace
	NullableIntHolder init;

	@Workspace
	BigIntHolder nonNullCount;

	public void setup() {
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
		}
	}

	@Override
	public void output() {
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
