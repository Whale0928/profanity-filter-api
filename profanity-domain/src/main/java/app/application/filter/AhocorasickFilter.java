package app.application.filter;

import java.util.List;

public interface AhocorasickFilter {
    void synchronizeProfanityTrie();

    List<?> getProfanityTrieList();
}
