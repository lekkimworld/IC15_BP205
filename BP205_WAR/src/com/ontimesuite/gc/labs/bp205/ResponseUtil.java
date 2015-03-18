package com.ontimesuite.gc.labs.bp205;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;

import dk.intravision.json.JsonBuilder;

public class ResponseUtil {

	public static void sendError(HttpServletResponse resp, int httpCode, String errorCode, String errorTxt) throws IOException {
		sendError(resp, httpCode, errorCode, errorTxt, null);
	}
	
	public static void sendError(HttpServletResponse resp, int httpCode, String errorCode, String errorTxt, Throwable t) throws IOException {
		sendError(resp, httpCode, errorCode, errorTxt, t, null);
	}
	
	public static void sendError(HttpServletResponse resp, int httpCode, String errorCode, String errorTxt, Throwable t, String key, String... additionalInfo) throws IOException {
		JsonBuilder b = new JsonBuilder()
			.add("Status", "ERROR")
			.add("ErrorCode", errorCode)
			.add("ErrorText", errorTxt);
		if (null != t) {
			StringWriter sw = new StringWriter();
			t.printStackTrace(new PrintWriter(sw));
			String stacktrace = sw.toString();
			b.add("Stacktrace", stacktrace);
		}
		if (!StringUtil.isEmpty(key) && !ArrayUtil.isEmpty(additionalInfo) && additionalInfo.length % 2 == 0) {
			b.beginObject(key);
			for (int i=0; i<additionalInfo.length; ) {
				b.add(additionalInfo[i++], additionalInfo[i++]);
			}
			b.endObject();
		}
		resp.setStatus(httpCode);
		resp.setContentType("application/json");
		PrintWriter pw = resp.getWriter();
		pw.write(b.toString());
		pw.flush();
		pw.close();
	}

}
