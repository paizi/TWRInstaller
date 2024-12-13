package com.khjxiaogu.tssap.util;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class JsonUTCDateAdapter extends TypeAdapter<Date> {
	public SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public JsonUTCDateAdapter() {
		super();
		sdf.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
	}

	@Override
	public void write(JsonWriter out, Date value) throws IOException {
		out.value(sdf.format(value));
		
	}

	@Override
	public Date read(JsonReader in) throws IOException {
		try {
			return sdf.parse(in.nextString());
		} catch (ParseException e) {
			throw new JsonSyntaxException(e);
		}
	}

}
