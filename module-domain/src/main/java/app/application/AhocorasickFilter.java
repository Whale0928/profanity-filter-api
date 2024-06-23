package app.application;

import java.util.List;

public interface AhocorasickFilter {
    void synchronizeProfanityTrie();

    List<?> getProfanityTrieList();
}
