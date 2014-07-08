package org.rootsdev.polygenea;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Test;


public class TestUUID5 {

	@Test
	public void testNull() {
		assertEquals(UUID.fromString("da39a3ee-5e6b-5b0d-b255-bfef95601890"), UUID5.fromUTF8(""));
	}
	@Test
	public void testNullNamespace() {
		assertEquals(UUID.fromString("e129f27c-5103-5c5c-844b-cdf0a15e160d"), UUID5.fromUTF8(null, ""));
	}
	@Test
	public void testNullNamespace2() {
		assertEquals(UUID.fromString("e129f27c-5103-5c5c-844b-cdf0a15e160d"), UUID5.fromUTF8(new UUID(0,0), ""));
	}
	@Test
	public void testPolygenea() {
		assertEquals(UUID.fromString("a0ed6102-497e-562a-86db-762638f9fc59"), UUID5.fromUTF8("polygenea"));
	}
	@Test
	public void testPolygenea2() {
		assertEquals(UUID.fromString("954aac7d-47b2-5975-9a80-37eeed186527"), UUID5.fromUTF8(null, "polygenea"));
	}
	@Test
	public void testPolygenea3() {
		assertEquals(UUID.fromString("954aac7d-47b2-5975-9a80-37eeed186527"), UUID5.fromUTF8(new UUID(0,0), "polygenea"));
	}
	@Test
	public void testPolygenea4() {
		assertEquals(UUID.fromString("736071f3-edeb-5c7a-afcc-b73417e0852f"), UUID5.fromUTF8(UUID5.fromUTF8("polygenea"), "polygenea"));
	}
	@Test
	public void testPolygenea5() {
		assertEquals(UUID.fromString("a282126e-598a-557b-adb9-7efa7dc5ac49"), UUID5.fromUTF8(UUID5.fromUTF8(null, "polygenea"), "polygenea"));
	}
}
