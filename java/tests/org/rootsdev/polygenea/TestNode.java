package org.rootsdev.polygenea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;
import org.rootsdev.polygenea.nodes.Citation;
import org.rootsdev.polygenea.nodes.ExternalSource;
import org.rootsdev.polygenea.nodes.Thing;

public class TestNode {

	@Test
	public void testCitationLiteral() {
		Citation c = new Citation(Citation.Kind.TRANSIENT, "type","imagination","when","2014-07-06 04:24:20+00:00");
		StringBuilder sb = new StringBuilder();
		boolean valid = c.validate(sb);
		assertTrue(sb.toString(), valid);
		assertEquals("canonical string to hash", 
				"{\"!class\":\"Citation\",\"details\":{\"type\":\"imagination\",\"when\":\"2014-07-06 04:24:20+00:00\"},\"kind\":\"TRANSIENT\"}",
				c.hashableJSON());
	}
	@Test
	public void testFromJSONLite() {
		Citation c = (Citation)Node.fromJSON("{\"!class\":\"Citation\",\"details\":{\"type\":\"imagination\",\"when\":\"2014-07-06 04:24:20+00:00\"},\"kind\":\"TRANSIENT\"}", null);
		StringBuilder sb = new StringBuilder();
		boolean valid = c.validate(sb);
		assertTrue(sb.toString(), valid);
		assertEquals("UUID", UUID.fromString("8a6b11fd-49af-52f0-8673-7056f0c77287"), c.getUUID());
	}
	@Test
	public void testFromJSONHeavy() {
		Citation c = (Citation)Node.fromJSON("{\"!class\":\"Citation\",\"!uuid\":\"8a6b11fd-49af-52f0-8673-7056f0c77287\",\"details\":{\"type\":\"imagination\",\"when\":\"2014-07-06 04:24:20+00:00\"},\"kind\":\"TRANSIENT\"}", null);
		StringBuilder sb = new StringBuilder();
		boolean valid = c.validate(sb);
		assertTrue(sb.toString(), valid);
	}
	@Test
	public void testFromJSONValidate() {
		Citation c = (Citation)Node.fromJSON("{\"!class\":\"Citation\",\"!uuid\":\"1a6b11fd-49af-52f0-8673-7056f0c77287\",\"details\":{\"type\":\"imagination\",\"when\":\"2014-07-06 04:24:20+00:00\"},\"kind\":\"TRANSIENT\"}", null);
		StringBuilder sb = new StringBuilder();
		boolean valid = c.validate(sb);
		assertFalse("Validate should have found UUID inconsistency", valid);
	}
	@Test
	public void testFromJSONIdentityCheck() {
		try {
			Node.fromJSON("{\"!class\":\"Citation\",\"!uuid\":\"8a6b11fd-49af-42f0-8673-7056f0c77287\",\"details\":{\"type\":\"imagination\",\"when\":\"2014-07-06 04:24:20+00:00\"},\"kind\":\"TRANSIENT\"}", null);
			fail("Constructor should have thrown an Exception when given a type-4 UUID with a class lacking identity");
		} catch (IllegalArgumentException ex) {
			assertTrue("Message should mention uuid version", ex.getMessage().contains("version"));
			assertTrue("Message should mention uuid version", ex.getMessage().toLowerCase().contains("uuid"));
		}
	}

	@Test
	public void testESLiteral() {
		Citation c = new Citation(Citation.Kind.TRANSIENT, "type","imagination","when","2014-07-06 04:24:20+00:00");
		ExternalSource es = new ExternalSource(c, "My sister Jane is also my legal guardian");
		StringBuilder sb = new StringBuilder();
		boolean valid = es.validate(sb);
		assertTrue(sb.toString(), valid);
		assertEquals("canonical string to hash", 
				"{\"!class\":\"ExternalSource\",\"citation\":\"8a6b11fd-49af-52f0-8673-7056f0c77287\",\"content\":\"My sister Jane is also my legal guardian\",\"contentType\":\"text/plain\"}",
				es.hashableJSON());
		// System.out.println(es.getUUID());
	}
	@Test
	public void testDBLoadLiteral() {
		Citation c = new Citation(Citation.Kind.TRANSIENT, "type","imagination","when","2014-07-06 04:24:20+00:00");
		Database db = new Database();
		db.add(c);
		db.addJSON("{\"!class\":\"ExternalSource\",\"citation\":\"8a6b11fd-49af-52f0-8673-7056f0c77287\",\"content\":\"My sister Jane is also my legal guardian\",\"contentType\":\"text/plain\"}");
		ExternalSource es = (ExternalSource)db.lookup("f67a64b3-53bf-5bf7-bd69-f39afd24e252");
		assertEquals("loaded source", "My sister Jane is also my legal guardian", es.content);
	}
	@Test
	public void testDBLoadList() {
		Database db = new Database();
		db.addJSON("[{\"!class\":\"Citation\",\"details\":{\"type\":\"imagination\",\"when\":\"2014-07-06 04:24:20+00:00\"},\"kind\":\"TRANSIENT\"},{\"!class\":\"ExternalSource\",\"citation\":0,\"content\":\"My sister Jane is also my legal guardian\",\"contentType\":\"text/plain\"}]");
		assertEquals("databsae size", 2, db.size());
		assertEquals("first element", UUID.fromString("8a6b11fd-49af-52f0-8673-7056f0c77287"), db.asSerializableCollection().get(0).getUUID());
		assertEquals("second element", UUID.fromString("f67a64b3-53bf-5bf7-bd69-f39afd24e252"), db.asSerializableCollection().get(1).getUUID());
	}
	@Test
	public void testThingLiteral() {
		Citation c = new Citation(Citation.Kind.TRANSIENT, "type","imagination","when","2014-07-06 04:24:20+00:00");
		ExternalSource es = new ExternalSource(c, "My sister Jane is also my legal guardian");
		Thing t = new Thing(es);
		StringBuilder sb = new StringBuilder();
		boolean valid = t.validate(sb);
		assertTrue(t.toString(), valid);
		assertTrue("Thing class @HasIdentity", t.hasIdentity());
		assertEquals("Thing should have version-4 UUID", 4, t.getUUID().version());
		assertFalse("compressed strings for ExternalSource should not have a !uuid", Node.compressedJSON(es).contains("\"!uuid\":"));
		assertTrue("compressed strings for Thing should have a !uuid", Node.compressedJSON(t).contains("\"!uuid\":"));
	}
	@Test
	public void testThingLoading() {
		Citation c = new Citation(Citation.Kind.TRANSIENT, "type","imagination","when","2014-07-06 04:24:20+00:00");
		ExternalSource es = new ExternalSource(c, "My sister Jane is also my legal guardian");
		Thing t = new Thing(es);
		Database db = new Database(); db.add(c); db.add(es); db.add(t);
		db.addJSON(t.hashableJSON());
		assertEquals("If given without UUID, should make a new one", 4, db.size());
		db.addJSON(t.toString());
		assertEquals("If given without UUID, should not make a new one", 4, db.size());
	}
}
