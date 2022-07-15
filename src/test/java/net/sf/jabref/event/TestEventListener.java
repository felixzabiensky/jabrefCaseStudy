package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.event.EntryAddedEvent;
import net.sf.jabref.model.event.EntryChangedEvent;
import net.sf.jabref.model.event.EntryRemovedEvent;

import com.google.common.eventbus.Subscribe;

public class TestEventListener {

    private BibEntry bibEntry;


    @Subscribe
    public void listen(EntryAddedEvent event) {
        this.bibEntry = event.getBibEntry();
    }

    @Subscribe
    public void listen(EntryRemovedEvent event) {
        this.bibEntry = event.getBibEntry();
    }

    @Subscribe
    public void listen(EntryChangedEvent event) {
        this.bibEntry = event.getBibEntry();
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }

}
