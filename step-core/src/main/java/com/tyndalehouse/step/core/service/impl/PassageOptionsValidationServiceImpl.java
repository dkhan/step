package com.tyndalehouse.step.core.service.impl;

import com.tyndalehouse.step.core.data.EntityManager;
import com.tyndalehouse.step.core.models.AvailableFeatures;
import com.tyndalehouse.step.core.models.ClientSession;
import com.tyndalehouse.step.core.models.InterlinearMode;
import com.tyndalehouse.step.core.models.LookupOption;
import com.tyndalehouse.step.core.models.TrimmedLookupOption;
import com.tyndalehouse.step.core.service.BibleInformationService;
import com.tyndalehouse.step.core.service.PassageOptionsValidationService;
import com.tyndalehouse.step.core.service.helpers.VersionResolver;
import com.tyndalehouse.step.core.service.jsword.JSwordMetadataService;
import com.tyndalehouse.step.core.service.jsword.JSwordModuleService;
import com.tyndalehouse.step.core.service.jsword.JSwordPassageService;
import com.tyndalehouse.step.core.service.jsword.JSwordSearchService;
import com.tyndalehouse.step.core.service.jsword.JSwordVersificationService;
import com.tyndalehouse.step.core.service.search.SubjectSearchService;
import com.tyndalehouse.step.core.utils.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import static com.tyndalehouse.step.core.models.InterlinearMode.INTERLINEAR;
import static com.tyndalehouse.step.core.models.InterlinearMode.NONE;
import static com.tyndalehouse.step.core.models.LookupOption.ENGLISH_VOCAB;
import static com.tyndalehouse.step.core.models.LookupOption.GREEK_VOCAB;
import static com.tyndalehouse.step.core.models.LookupOption.HEADINGS;
import static com.tyndalehouse.step.core.models.LookupOption.MORPHOLOGY;
import static com.tyndalehouse.step.core.models.LookupOption.NOTES;
import static com.tyndalehouse.step.core.models.LookupOption.TRANSLITERATION;
import static com.tyndalehouse.step.core.models.LookupOption.VERSE_NUMBERS;
import static com.tyndalehouse.step.core.utils.StringUtils.isBlank;

/**
 * @author chrisburrell
 */
@Singleton
public class PassageOptionsValidationServiceImpl implements PassageOptionsValidationService {
    private final Provider<ClientSession> clientSessionProvider;
    private final JSwordMetadataService jswordMetadata;

    @Inject
    public PassageOptionsValidationServiceImpl(final JSwordMetadataService jswordMetadata,
                                               final Provider<ClientSession> clientSessionProvider) {
        this.jswordMetadata = jswordMetadata;
        this.clientSessionProvider = clientSessionProvider;
    }

    @Override
    public List<LookupOption> getLookupOptions(final String options) {
        final List<LookupOption> lookupOptions = new ArrayList<LookupOption>();

        if (isBlank(options)) {
            return lookupOptions;
        }

        for (int ii = 0; ii < options.length(); ii++) {
            lookupOptions.add(LookupOption.fromUiOption(options.charAt(ii)));
        }
        return lookupOptions;
    }

    @Override
    public Set<LookupOption> trim(final List<LookupOption> options, final String version, List<String> extraVersions,
                                  final InterlinearMode mode, final List<TrimmedLookupOption> trimmingExplanations) {
        // obtain error messages
        final ResourceBundle errors = ResourceBundle.getBundle("ErrorBundle", this.clientSessionProvider
                .get().getLocale());

        if (options.isEmpty()) {
            return new HashSet<LookupOption>();
        }

        final Set<LookupOption> result = getUserOptionsForVersion(errors, options, version, extraVersions, trimmingExplanations);

        // if we're not explaining why features aren't available, we don't overwrite the display mode
        final InterlinearMode displayMode = determineDisplayMode(options, mode, trimmingExplanations);

        // now trim further depending on modes required:
        switch (displayMode) {
            case COLUMN:
            case COLUMN_COMPARE:
            case INTERLEAVED:
            case INTERLEAVED_COMPARE:
                removeInterleavingOptions(errors, trimmingExplanations, result, !mode.equals(displayMode));
                break;
            case INTERLINEAR:
                explainRemove(errors, NOTES, result, trimmingExplanations, !mode.equals(displayMode),
                        errors.getString("option_not_available_interlinear"));
                result.add(LookupOption.VERSE_NEW_LINE);
                break;
            case NONE:
                break;
            default:
                break;
        }

        return result;
    }

    /**
     * Determine display mode, if there are no explanations, display mode is NONE and there are interlinear
     * options, then mode gets override to INTERLINEAR
     *
     * @param options              the options
     * @param mode                 the mode
     * @param trimmingExplanations the trimming explanations
     * @return the interlinear mode
     * @
     */
    private InterlinearMode determineDisplayMode(final List<LookupOption> options,
                                                 final InterlinearMode mode,
                                                 final List<TrimmedLookupOption> trimmingExplanations) {
        if (mode == NONE && trimmingExplanations == null && hasInterlinearOption(options)) {
            return INTERLINEAR;
        }

        return mode;
    }

    /**
     * Removes the interleaving options.
     *
     * @param errors                 the error mesages
     * @param trimmingExplanations   explanations on why something was removed
     * @param result                 result
     * @param originalModeHasChanged true to indicate that the chosen display mode has been forced upon the
     *                               user
     */
    private void removeInterleavingOptions(final ResourceBundle errors,
                                           final List<TrimmedLookupOption> trimmingExplanations,
                                           final Set<LookupOption> result,
                                           final boolean originalModeHasChanged) {
        final String interleavedMessage = errors.getString("option_not_available_interleaved");
        explainRemove(errors, VERSE_NUMBERS, result, trimmingExplanations, originalModeHasChanged,
                interleavedMessage);

        explainRemove(errors, NOTES, result, trimmingExplanations, originalModeHasChanged, interleavedMessage);

        explainRemove(errors, ENGLISH_VOCAB, result, trimmingExplanations, originalModeHasChanged,
                interleavedMessage);

        explainRemove(errors, GREEK_VOCAB, result, trimmingExplanations, originalModeHasChanged,
                interleavedMessage);

        explainRemove(errors, TRANSLITERATION, result, trimmingExplanations, originalModeHasChanged,
                interleavedMessage);

        explainRemove(errors, MORPHOLOGY, result, trimmingExplanations, originalModeHasChanged,
                interleavedMessage);

        explainRemove(errors, HEADINGS, result, trimmingExplanations, originalModeHasChanged,
                interleavedMessage);

    }

    /**
     * explains why an option has been removed.
     *
     * @param errors              the errors
     * @param option              the option we want to remove
     * @param result              the resulting options
     * @param trimmingOptions     the list of options
     * @param originalModeChanged tru if the original mode has changed
     * @param explanation         the explanation
     */
    private void explainRemove(final ResourceBundle errors, final LookupOption option,
                               final Set<LookupOption> result, final List<TrimmedLookupOption> trimmingOptions,
                               final boolean originalModeChanged, final String explanation) {
        if (result.remove(option) && trimmingOptions != null) {

            final TrimmedLookupOption trimmedOption;
            if (originalModeChanged) {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(explanation);
                stringBuilder.append(" ");
                stringBuilder.append(errors.getString("option_not_available_other"));
                trimmedOption = new TrimmedLookupOption(stringBuilder.toString(), option);
            } else {
                trimmedOption = new TrimmedLookupOption(explanation, option);
            }
            trimmingOptions.add(trimmedOption);
        }
    }

    /**
     * @param options the options that have been selected
     * @return true if one of the options requires an interlinear
     */
    private boolean hasInterlinearOption(final List<LookupOption> options) {
        return options.contains(LookupOption.GREEK_VOCAB) || options.contains(LookupOption.MORPHOLOGY)
                || options.contains(LookupOption.ENGLISH_VOCAB)
                || options.contains(LookupOption.TRANSLITERATION);
    }

    /**
     * Given a set of options selected by the user and a verson, retrieves the options that are actually
     * available
     *
     * @param errors               the error messages
     * @param options              the options given by the user
     * @param version              the version of interest
     * @param extraVersions        the secondary versions that affect feature resolution
     * @param trimmingExplanations the explanations of why options are being removed
     * @return a potentially smaller set of options that are actually possible
     */
    private Set<LookupOption> getUserOptionsForVersion(final ResourceBundle errors,
                                                       final List<LookupOption> options, final String version,
                                                       final List<String> extraVersions,
                                                       final List<TrimmedLookupOption> trimmingExplanations) {
        final Set<LookupOption> available = this.jswordMetadata.getFeatures(version, extraVersions);
        final Set<LookupOption> result = new HashSet<LookupOption>(options.size());
        // do a crazy bubble intersect, but it's tiny so that's fine
        for (final LookupOption loOption : options) {
            boolean added = false;
            for (final LookupOption avOption : available) {
                if (loOption.equals(avOption)) {
                    result.add(loOption);
                    added = true;
                    break;
                }
            }

            // option not available in that particular version
            if (trimmingExplanations != null && !added) {
                trimmingExplanations.add(new TrimmedLookupOption(errors
                        .getString("option_not_supported_by_version"), loOption));
            }
        }
        return result;
    }

    
    @Override
    public AvailableFeatures getAvailableFeaturesForVersion(final String version, final List<String> extraVersions, final String displayMode) {
        final List<LookupOption> allLookupOptions = Arrays.asList(LookupOption.values());
        final List<TrimmedLookupOption> trimmed = new ArrayList<TrimmedLookupOption>();
        final Set<LookupOption> outcome = trim(allLookupOptions, version,
                extraVersions, getDisplayMode(displayMode, version, extraVersions),
                trimmed);

        return new AvailableFeatures(new ArrayList<LookupOption>(outcome), trimmed);
    }

    @Override
    public InterlinearMode getDisplayMode(final String interlinearMode, final String mainBook, final List<String> extraVersions) {
        InterlinearMode userDesiredMode = isBlank(interlinearMode) ? NONE : InterlinearMode.valueOf(interlinearMode);
        return this.jswordMetadata.getBestInterlinearMode(mainBook, extraVersions, userDesiredMode);
    }
    

    @Override
    public String optionsToString(final Collection<LookupOption> options) {
        StringBuilder codedOptions = new StringBuilder();
        for (LookupOption o : options) {
            if (o.getUiName() != BibleInformationService.UNAVAILABLE_TO_UI) {
                codedOptions.append(o.getUiName());
            }
        }
        return codedOptions.toString();
    }
}