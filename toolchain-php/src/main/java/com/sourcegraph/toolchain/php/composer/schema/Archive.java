
package com.sourcegraph.toolchain.php.composer.schema;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * Options for creating package archives for distribution.
 * 
 */
@Generated("org.jsonschema2pojo")
public class Archive {

    /**
     * A list of patterns for paths to exclude or include if prefixed with an exclamation mark.
     * 
     */
    @SerializedName("exclude")
    @Expose
    private List<Object> exclude = new ArrayList<Object>();

    /**
     * A list of patterns for paths to exclude or include if prefixed with an exclamation mark.
     * 
     * @return
     *     The exclude
     */
    public List<Object> getExclude() {
        return exclude;
    }

    /**
     * A list of patterns for paths to exclude or include if prefixed with an exclamation mark.
     * 
     * @param exclude
     *     The exclude
     */
    public void setExclude(List<Object> exclude) {
        this.exclude = exclude;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(exclude).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Archive) == false) {
            return false;
        }
        Archive rhs = ((Archive) other);
        return new EqualsBuilder().append(exclude, rhs.exclude).isEquals();
    }

}
