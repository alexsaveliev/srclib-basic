
package com.sourcegraph.toolchain.php.composer.schema;

import java.net.URI;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Generated("org.jsonschema2pojo")
public class Author {

    /**
     * Full name of the author.
     * (Required)
     * 
     */
    @SerializedName("name")
    @Expose
    private String name;
    /**
     * Email address of the author.
     * 
     */
    @SerializedName("email")
    @Expose
    private String email;
    /**
     * Homepage URL for the author.
     * 
     */
    @SerializedName("homepage")
    @Expose
    private URI homepage;
    /**
     * Author's role in the project.
     * 
     */
    @SerializedName("role")
    @Expose
    private String role;

    /**
     * Full name of the author.
     * (Required)
     * 
     * @return
     *     The name
     */
    public String getName() {
        return name;
    }

    /**
     * Full name of the author.
     * (Required)
     * 
     * @param name
     *     The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Email address of the author.
     * 
     * @return
     *     The email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Email address of the author.
     * 
     * @param email
     *     The email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Homepage URL for the author.
     * 
     * @return
     *     The homepage
     */
    public URI getHomepage() {
        return homepage;
    }

    /**
     * Homepage URL for the author.
     * 
     * @param homepage
     *     The homepage
     */
    public void setHomepage(URI homepage) {
        this.homepage = homepage;
    }

    /**
     * Author's role in the project.
     * 
     * @return
     *     The role
     */
    public String getRole() {
        return role;
    }

    /**
     * Author's role in the project.
     * 
     * @param role
     *     The role
     */
    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(email).append(homepage).append(role).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Author) == false) {
            return false;
        }
        Author rhs = ((Author) other);
        return new EqualsBuilder().append(name, rhs.name).append(email, rhs.email).append(homepage, rhs.homepage).append(role, rhs.role).isEquals();
    }

}
