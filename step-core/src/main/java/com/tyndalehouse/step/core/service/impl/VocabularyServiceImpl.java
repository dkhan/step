package com.tyndalehouse.step.core.service.impl;

import static com.tyndalehouse.step.core.utils.ValidateUtils.notBlank;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.avaje.ebean.EbeanServer;
import com.tyndalehouse.step.core.data.entities.LexiconDefinition;
import com.tyndalehouse.step.core.exceptions.UserExceptionType;
import com.tyndalehouse.step.core.service.VocabularyService;

/**
 * defines all vocab related queries
 * 
 * @author chrisburrell
 * 
 */
@Singleton
public class VocabularyServiceImpl implements VocabularyService {
    private static final String HIGHER_STRONG = "STRONG:";
    private static final String LOWER_STRONG = "strong:";
    private static final int START_STRONG_KEY = HIGHER_STRONG.length();
    private final EbeanServer ebean;

    /**
     * @param ebean the database server
     */
    @Inject
    public VocabularyServiceImpl(final EbeanServer ebean) {
        this.ebean = ebean;
    }

    @Override
    public List<LexiconDefinition> getDefinitions(final String vocabIdentifiers) {
        notBlank(vocabIdentifiers, "Vocab identifiers was null", UserExceptionType.SERVICE_VALIDATION_ERROR);

        final List<String> idList = getKeys(vocabIdentifiers);
        return this.ebean.find(LexiconDefinition.class).where().idIn(idList).findList();

    }

    /**
     * Extracts a compound key into several keys
     * 
     * @param vocabIdentifiers the vocabulary identifiers
     * @return the list of all keys to lookup
     */
    List<String> getKeys(final String vocabIdentifiers) {
        final List<String> idList = new ArrayList<String>();
        final String[] ids = vocabIdentifiers.split(",");

        for (final String i : ids) {

            if (i.length() > START_STRONG_KEY + 1
                    && (i.startsWith(LOWER_STRONG) || i.startsWith(HIGHER_STRONG))) {
                idList.add(String.format("%c%04d", i.charAt(START_STRONG_KEY),
                        Integer.parseInt(i.substring(START_STRONG_KEY + 1))));
            }
        }
        return idList;
    }
}