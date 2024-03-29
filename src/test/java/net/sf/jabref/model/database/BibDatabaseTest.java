package net.sf.jabref.model.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.event.TestEventListener;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.IdGenerator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class BibDatabaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private BibDatabase database;

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance(); // set preferences for this test

        database = new BibDatabase();
    }

    @Test
    public void resolveStrings() throws IOException {
        try (FileInputStream stream = new FileInputStream("src/test/resources/net/sf/jabref/util/twente.bib");
                InputStreamReader fr = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            ParserResult result = BibtexParser.parse(fr);

            BibDatabase db = result.getDatabase();

            assertEquals("Arvind", db.resolveForStrings("#Arvind#"));
            assertEquals("Patterson, David", db.resolveForStrings("#Patterson#"));
            assertEquals("Arvind and Patterson, David", db.resolveForStrings("#Arvind# and #Patterson#"));

            // Strings that are not found return just the given string.
            assertEquals("#unknown#", db.resolveForStrings("#unknown#"));
        }
    }

    @Test
    public void insertEntryAddsEntryToEntriesList() {
        BibEntry entry = new BibEntry();
        database.insertEntry(entry);
        assertEquals(database.getEntries().size(), 1);
        assertEquals(database.getEntryCount(), 1);
        assertEquals(entry, database.getEntries().get(0));
    }

    @Test
    public void containsEntryIdFindsEntry() {
        BibEntry entry = new BibEntry();
        assertFalse(database.containsEntryWithId(entry.getId()));
        database.insertEntry(entry);
        assertTrue(database.containsEntryWithId(entry.getId()));
    }

    @Test(expected = KeyCollisionException.class)
    public void insertEntryWithSameIdThrowsException() {
        BibEntry entry0 = new BibEntry();
        database.insertEntry(entry0);

        BibEntry entry1 = new BibEntry(entry0.getId());
        database.insertEntry(entry1);
        fail();
    }

    @Test
    public void removeEntryRemovesEntryFromEntriesList() {
        BibEntry entry = new BibEntry();
        database.insertEntry(entry);

        database.removeEntry(entry);
        assertEquals(Collections.emptyList(), database.getEntries());
        assertFalse(database.containsEntryWithId(entry.getId()));
    }

    @Test(expected = NullPointerException.class)
    public void insertNullEntryThrowsException() {
        database.insertEntry(null);
        fail();
    }

    @Test(expected = NullPointerException.class)
    public void removeNullEntryThrowsException() {
        database.removeEntry(null);
        fail();
    }

    @Test
    public void emptyDatabaseHasNoStrings() {
        assertEquals(Collections.emptySet(), database.getStringKeySet());
        assertTrue(database.hasNoStrings());
    }

    @Test
    public void insertStringUpdatesStringList() {
        BibtexString string = new BibtexString(IdGenerator.next(), "DSP", "Digital Signal Processing");
        database.addString(string);
        assertFalse(database.hasNoStrings());
        assertEquals(database.getStringKeySet().size(), 1);
        assertEquals(database.getStringCount(), 1);
        assertTrue(database.getStringValues().contains(string));
        assertTrue(database.getStringKeySet().contains(string.getId()));
        assertEquals(string, database.getString(string.getId()));
    }

    @Test
    public void removeStringUpdatesStringList() {
        BibtexString string = new BibtexString(IdGenerator.next(), "DSP", "Digital Signal Processing");
        database.addString(string);
        database.removeString(string.getId());
        assertTrue(database.hasNoStrings());
        assertEquals(database.getStringKeySet().size(), 0);
        assertEquals(database.getStringCount(), 0);
        assertFalse(database.getStringValues().contains(string));
        assertFalse(database.getStringKeySet().contains(string.getId()));
        assertNull(database.getString(string.getId()));
    }

    @Test
    public void hasStringLabelFindsString() {
        BibtexString string = new BibtexString(IdGenerator.next(), "DSP", "Digital Signal Processing");
        database.addString(string);
        assertTrue(database.hasStringLabel("DSP"));
        assertFalse(database.hasStringLabel("VLSI"));
    }

    @Test(expected = KeyCollisionException.class)
    public void addSameStringLabelTwiceThrowsKeyCollisionException() {
        BibtexString string = new BibtexString(IdGenerator.next(), "DSP", "Digital Signal Processing");
        database.addString(string);
        string = new BibtexString(IdGenerator.next(), "DSP", "Digital Signal Processor");
        database.addString(string);
        fail();
    }

    @Test(expected = KeyCollisionException.class)
    public void addSameStringIdTwiceThrowsKeyCollisionException() {
        String id = IdGenerator.next();
        BibtexString string = new BibtexString(id, "DSP", "Digital Signal Processing");
        database.addString(string);
        string = new BibtexString(id, "VLSI", "Very Large Scale Integration");
        database.addString(string);
        fail();
    }

    @Test
    public void insertEntryPostsAddedEntryEvent() {
        BibEntry expectedEntry = new BibEntry();
        TestEventListener tel = new TestEventListener();
        database.registerListener(tel);
        database.insertEntry(expectedEntry);
        BibEntry actualEntry = tel.getBibEntry();
        assertEquals(expectedEntry, actualEntry);
    }

    @Test
    public void removeEntryPostsRemovedEntryEvent() {
        BibEntry expectedEntry = new BibEntry();
        TestEventListener tel = new TestEventListener();
        database.insertEntry(expectedEntry);
        database.registerListener(tel);
        database.removeEntry(expectedEntry);
        BibEntry actualEntry = tel.getBibEntry();
        assertEquals(expectedEntry, actualEntry);
    }

    @Test
    public void changingEntryPostsChangeEntryEvent() {
        BibEntry entry = new BibEntry();
        TestEventListener tel = new TestEventListener();
        database.insertEntry(entry);
        database.registerListener(tel);

        entry.setField("test", "some value");

        assertEquals(entry, tel.getBibEntry());
    }

    @Test
    public void correctKeyCountOne() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(database.getNumberOfKeyOccurrences("AAA"), 1);
    }

    @Test
    public void correctKeyCountTwo() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(database.getNumberOfKeyOccurrences("AAA"), 2);
    }

    @Test
    public void setCiteKeySameKeySameEntry() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertFalse(database.setCiteKeyForEntry(entry, "AAA"));
        assertEquals(database.getNumberOfKeyOccurrences("AAA"), 1);
    }

    @Test
    public void setCiteKeyRemoveKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertFalse(database.setCiteKeyForEntry(entry, null));
        assertEquals(database.getNumberOfKeyOccurrences("AAA"), 0);
        assertNull(entry.getCiteKey());
    }

    @Test
    public void setCiteKeyDifferentKeySameEntry() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertFalse(database.setCiteKeyForEntry(entry, "BBB"));
        assertEquals(database.getNumberOfKeyOccurrences("AAA"), 0);
        assertEquals(database.getNumberOfKeyOccurrences("BBB"), 1);
    }


    @Test
    public void setCiteKeySameKeyDifferentEntries() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        entry = new BibEntry();
        entry.setCiteKey("BBB");
        database.insertEntry(entry);
        assertTrue(database.setCiteKeyForEntry(entry, "AAA"));
        assertEquals(entry.getCiteKey(), "AAA");
        assertEquals(database.getNumberOfKeyOccurrences("AAA"), 2);
        assertEquals(database.getNumberOfKeyOccurrences("BBB"), 0);
    }

    @Test
    public void correctKeyCountAfterRemoving() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        database.removeEntry(entry);
        assertEquals(database.getNumberOfKeyOccurrences("AAA"), 1);
    }

    @Test
    public void circularStringResolving() {
        BibtexString string = new BibtexString(IdGenerator.next(), "AAA", "#BBB#");
        database.addString(string);
        string = new BibtexString(IdGenerator.next(), "BBB", "#AAA#");
        database.addString(string);
        assertEquals(database.resolveForStrings("#AAA#"), "AAA");
        assertEquals(database.resolveForStrings("#BBB#"), "BBB");
    }

    @Test
    public void circularStringResolvingLongerCycle() {
        BibtexString string = new BibtexString(IdGenerator.next(), "AAA", "#BBB#");
        database.addString(string);
        string = new BibtexString(IdGenerator.next(), "BBB", "#CCC#");
        database.addString(string);
        string = new BibtexString(IdGenerator.next(), "CCC", "#DDD#");
        database.addString(string);
        string = new BibtexString(IdGenerator.next(), "DDD", "#AAA#");
        database.addString(string);
        assertEquals(database.resolveForStrings("#AAA#"), "AAA");
        assertEquals(database.resolveForStrings("#BBB#"), "BBB");
        assertEquals(database.resolveForStrings("#CCC#"), "CCC");
        assertEquals(database.resolveForStrings("#DDD#"), "DDD");
    }

    @Test
    public void resolveForStringsMonth() {
        assertEquals(database.resolveForStrings("#jan#"), "January");
    }

    @Test
    public void resolveForStringsSurroundingContent() {
        BibtexString string = new BibtexString(IdGenerator.next(), "AAA", "aaa");
        database.addString(string);
        assertEquals(database.resolveForStrings("aa#AAA#AAA"), "aaaaaAAA");
    }

    @Test
    public void resolveForStringsOddHashMarkAtTheEnd() {
        BibtexString string = new BibtexString(IdGenerator.next(), "AAA", "aaa");
        database.addString(string);
        assertEquals(database.resolveForStrings("AAA#AAA#AAA#"), "AAAaaaAAA#");
    }
}
