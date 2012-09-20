package com.tyndalehouse.step.core.data.entities.lexicon;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import com.avaje.ebean.annotation.CacheStrategy;

/**
 * A particular representation of a word, tied back to the strong number in the lexicon
 * 
 * @author chrisburrell
 * 
 */
@CacheStrategy(readOnly = true)
@Entity
public class LexicalForm {
    @Id
    private int id;

    private String rawForm;

    @Column
    private String unaccentedForm;

    @OneToMany
    @JoinColumn(name = "rawStrongNumber", referencedColumnName = "strongNumber")
    private Definition strongNumber;

    private String rawStrongNumber;

    /**
     * @return the rawStrongNumber
     */
    public String getRawStrongNumber() {
        return this.rawStrongNumber;
    }

    /**
     * @param rawStrongNumber the rawStrongNumber to set
     */
    public void setRawStrongNumber(final String rawStrongNumber) {
        this.rawStrongNumber = rawStrongNumber;
    }

    /**
     * @return the rawForm
     */
    public String getRawForm() {
        return this.rawForm;
    }

    /**
     * @param rawForm the rawForm to set
     */
    public void setRawForm(final String rawForm) {
        this.rawForm = rawForm;
    }

    /**
     * @return the unaccentedForm
     */
    public String getUnaccentedForm() {
        return this.unaccentedForm;
    }

    /**
     * @param unaccentedForm the unaccentedForm to set
     */
    public void setUnaccentedForm(final String unaccentedForm) {
        this.unaccentedForm = unaccentedForm;
    }

    /**
     * @return the strongNumber
     */
    public Definition getStrongNumber() {
        return this.strongNumber;
    }

    /**
     * @param strongNumber the strongNumber to set
     */
    public void setStrongNumber(final Definition strongNumber) {
        this.strongNumber = strongNumber;
    }
}