package org.rootsdev.polygenea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.LinkedList;
import java.util.TreeMap;

import org.junit.Test;
import org.rootsdev.polygenea.JSONParser.MalformedJSONException;


public class TestJSONParser {
	
	/////////////////// NUMBERS ///////////////////////
	@Test
	public void t1() {
		assertEquals(JSONParser.parse("1"), Long.valueOf(1));
	}
	@Test
	public void t0() {
		assertEquals(JSONParser.parse("0"), Long.valueOf(0));
	}
	@Test
	public void t1234567890123456789() {
		assertEquals(JSONParser.parse("1234567890123456789"), Long.valueOf(1234567890123456789L));
	}
	@Test
	public void tneg() {
		assertEquals(JSONParser.parse("-0"), Long.valueOf(0));
	}
	@Test
	public void tneg4() {
		assertEquals(JSONParser.parse("-4"), Long.valueOf(-4));
	}
	@Test
	public void t00() {
		assertEquals(JSONParser.parse("0.0"), Double.valueOf(0));
	}
	@Test
	public void t0e0() {
		assertEquals(JSONParser.parse("0e0"), Double.valueOf(0));
	}
	@Test
	public void t_1e_4() {
		assertEquals(JSONParser.parse("-1e+4"), Double.valueOf(-1e+4));
	}
	@Test(expected=MalformedJSONException.class)
	public void tbignum() {
		JSONParser.parse("1234567890123457890123457890");
	}
	@Test(expected=MalformedJSONException.class)
	public void t0dot() {
		JSONParser.parse("0.");
	}
	@Test(expected=MalformedJSONException.class)
	public void tdot0() {
		JSONParser.parse(".0");
	}
	@Test(expected=MalformedJSONException.class)
	public void t_dot0() {
		JSONParser.parse("-.0");
	}
	@Test(expected=MalformedJSONException.class)
	public void tee() {
		JSONParser.parse("1e23e3");
	}



	/////////////////// LITERALS ///////////////////////
	public void tlit() {
		assertEquals(JSONParser.parse("true"), Boolean.valueOf(true));
		assertEquals(JSONParser.parse("false"), Boolean.valueOf(false));
		assertNull(JSONParser.parse("null"));
	}
	@Test(expected=MalformedJSONException.class)
	public void tnulla() { JSONParser.parse("nulla"); }
	@Test(expected=MalformedJSONException.class)
	public void tnul() { JSONParser.parse("nul"); }
	@Test(expected=MalformedJSONException.class)
	public void ttruea() { JSONParser.parse("truea"); }
	@Test(expected=MalformedJSONException.class)
	public void ttrul() { JSONParser.parse("tru"); }
	@Test(expected=MalformedJSONException.class)
	public void tfalsea() { JSONParser.parse("falsea"); }
	@Test(expected=MalformedJSONException.class)
	public void tfals() { JSONParser.parse("fals"); }
	
	
	
	/////////////////// STRINGS ///////////////////////
	@Test
	public void tEpsilon() {
		assertEquals(JSONParser.parse("\"\""),"");
	}
	@Test
	public void tplain() {
		assertEquals(JSONParser.parse("\"abcedf\ngh\rs\tt\""),"abcedf\ngh\rs\tt");
	}
	@Test
	public void tescapes() {
		assertEquals(JSONParser.parse("\"\\f\\n\\r\\t\\\"\\\\\""),"\f\n\r\t\"\\");
	}
	@Test
	public void tunicode() {
		assertEquals(JSONParser.parse("\"\\u0123\\u00a0\""),"\u0123\u00a0");
	}
	@Test(expected=MalformedJSONException.class)
	public void tonequote() {
		JSONParser.parse("\"hi there");
	}
	@Test(expected=MalformedJSONException.class)
	public void tbadescape() {
		JSONParser.parse("\"\\a\"");
	}
	@Test(expected=MalformedJSONException.class)
	public void tshortu() {
		JSONParser.parse("\"\\u012 hi\"");
	}


	/////////////////// LISTS ///////////////////////
	@Test
	public void tEmptyList() {
		assertEquals(JSONParser.parse("[]"), new LinkedList<Object>());
	}
	@Test
	public void tEmptyList2() {
		assertEquals(JSONParser.parse("[  \n ]"), new LinkedList<Object>());
	}
	@Test
	public void tl123() {
		LinkedList<Object> ans = new LinkedList<Object>();
		ans.add(1L); ans.add(2L); ans.add(3L);
		assertEquals(JSONParser.parse("[1,2,3]"), ans);
	}
	@Test
	public void tl12l3() {
		LinkedList<Object> ans = new LinkedList<Object>();
		ans.add(1L); ans.add(2L); ans.add(new LinkedList<Object>()); ans.add(3L);
		assertEquals(JSONParser.parse("[1,2,[],3]"), ans);
	}
	@SuppressWarnings("unchecked")
	@Test
	public void tl12lxy3() {
		LinkedList<Object> ans = new LinkedList<Object>();
		ans.add(1L); ans.add(2L); ans.add(new LinkedList<Object>()); 
		((LinkedList<Object>)ans.get(2)).add("x");
		((LinkedList<Object>)ans.get(2)).add("y");
		ans.add(3L);
		assertEquals(JSONParser.parse("[1,2,[\"x\", \"y\"],3]"), ans);
	}
	@Test(expected=MalformedJSONException.class)
	public void tunbal() {
		JSONParser.parse("[[[]]");
	}
	@Test(expected=MalformedJSONException.class)
	public void ttrail() {
		JSONParser.parse("[2,]");
	}
	@Test(expected=MalformedJSONException.class)
	public void tlead() {
		JSONParser.parse("[,2]");
	}


	/////////////////// OBJECTS ///////////////////////
	@Test
	public void tEmptyObj() {
		assertEquals(JSONParser.parse("{}"), new TreeMap<String,Object>());
	}
	@Test
	public void toa1b2() {
		TreeMap<String,Object> ans = new TreeMap<String,Object>();
		ans.put("a",1L); ans.put("b",2L);
		assertEquals(JSONParser.parse("{\"a\":1 , \"b\"  : 2  }"), ans);
	}
	@Test
	public void toa1blc2() {
		TreeMap<String,Object> ans = new TreeMap<String,Object>();
		ans.put("a",1L); ans.put("c",2L); ans.put("b",new LinkedList<Object>());
		assertEquals(JSONParser.parse("{\"a\":1 , \"b\"  : [], \"c\":2  }"), ans);
	}
	@SuppressWarnings("unchecked")
	@Test
	public void toa1bmc2() {
		TreeMap<String,Object> ans = new TreeMap<String,Object>();
		ans.put("a",1L); ans.put("c",2L); ans.put("b",new TreeMap<String,Object>());
		((TreeMap<String,Object>)ans.get("b")).put("fire", "water");
		assertEquals(JSONParser.parse("{ \"b\"  : {\"fire\":\"water\"}, \"c\":2, \"a\":1  }"), ans);
	}
	@Test(expected=MalformedJSONException.class)
	public void tnest() {
		JSONParser.parse("{\"a\":{}");
	}
	@Test(expected=MalformedJSONException.class)
	public void ttrailo() {
		JSONParser.parse("{\"a\":1,}");
	}
	@Test(expected=MalformedJSONException.class)
	public void tleado() {
		JSONParser.parse("{,\"a\":1}");
	}
}
