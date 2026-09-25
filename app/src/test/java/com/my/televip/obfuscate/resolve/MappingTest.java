package com.my.televip.obfuscate.resolve;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** The cache format: a mapping read back must be the mapping that was written. */
public class MappingTest {

    @Test
    public void roundTripsEveryKind() {
        Mapping m = new Mapping();
        m.classes.put("org.telegram.ui.Components.UItem", "n8b");
        m.fields.put(Mapping.memberKey("UItem", "id"), "d");
        m.methods.put(Mapping.memberKey("SettingsActivity", "fillItems"), "b0");
        m.putParameters("fillItems", new String[]{"java.util.ArrayList"});
        m.putParameters("noArgs", new String[0]);

        Mapping back = Mapping.deserialize(m.serialize());

        assertEquals("n8b", back.resolveClass("org.telegram.ui.Components.UItem"));
        assertEquals("d", back.resolveField("UItem", "id"));
        assertEquals("b0", back.resolveMethod("SettingsActivity", "fillItems"));
        assertArrayEquals(new String[]{"java.util.ArrayList"}, back.resolveParameters("fillItems"));
        assertArrayEquals(new String[0], back.resolveParameters("noArgs"));
    }

    @Test
    public void absentMeansAbsent() {
        Mapping back = Mapping.deserialize(new Mapping().serialize());
        assertNull(back.resolveClass("anything"));
        assertNull(back.resolveMethod("Owner", "key"));
        assertNull(back.resolveParameters("name"));
    }

    @Test
    public void garbageLinesAreIgnoredNotFatal() {
        Mapping back = Mapping.deserialize("nonsense\nC\tonly-two-parts\nX\ta\tb\nC\torg.x.Y\tq\n");
        assertEquals("q", back.resolveClass("org.x.Y"));
        assertEquals(1, back.size());
    }

    @Test
    public void firstParameterListWinsOnANameCollision() {
        Mapping m = new Mapping();
        m.putParameters("set", new String[]{"I"});
        m.putParameters("set", new String[]{"J"});
        assertArrayEquals(new String[]{"I"}, m.resolveParameters("set"));
    }
}
