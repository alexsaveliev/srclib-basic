
package com.sourcegraph.toolchain.php.composer.schema;

import java.net.URI;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Generated("org.jsonschema2pojo")
public class Support {

    /**
     * Email address for support.
     * 
     */
    @SerializedName("email")
    @Expose
    private String email;
    /**
     * URL to the issue tracker.
     * 
     */
    @SerializedName("issues")
    @Expose
    private URI issues;
    /**
     * URL to the forum.
     * 
     */
    @SerializedName("forum")
    @Expose
    private URI forum;
    /**
     * URL to the wiki.
     * 
     */
    @SerializedName("wiki")
    @Expose
    private URI wiki;
    /**
     * IRC channel for support, as irc://server/channel.
     * 
     */
    @SerializedName("irc")
    @Expose
    private URI irc;
    /**
     * URL to browse or download the sources.
     * 
     */
    @SerializedName("source")
    @Expose
    private URI source;
    /**
     * URL to the documentation.
     * 
     */
    @SerializedName("docs")
    @Expose
    private URI docs;

    /**
     * Email address for support.
     * 
     * @return
     *     The email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Email address for support.
     * 
     * @param email
     *     The email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * URL to the issue tracker.
     * 
     * @return
     *     The issues
     */
    public URI getIssues() {
        return issues;
    }

    /**
     * URL to the issue tracker.
     * 
     * @param issues
     *     The issues
     */
    public void setIssues(URI issues) {
        this.issues = issues;
    }

    /**
     * URL to the forum.
     * 
     * @return
     *     The forum
     */
    public URI getForum() {
        return forum;
    }

    /**
     * URL to the forum.
     * 
     * @param forum
     *     The forum
     */
    public void setForum(URI forum) {
        this.forum = forum;
    }

    /**
     * URL to the wiki.
     * 
     * @return
     *     The wiki
     */
    public URI getWiki() {
        return wiki;
    }

    /**
     * URL to the wiki.
     * 
     * @param wiki
     *     The wiki
     */
    public void setWiki(URI wiki) {
        this.wiki = wiki;
    }

    /**
     * IRC channel for support, as irc://server/channel.
     * 
     * @return
     *     The irc
     */
    public URI getIrc() {
        return irc;
    }

    /**
     * IRC channel for support, as irc://server/channel.
     * 
     * @param irc
     *     The irc
     */
    public void setIrc(URI irc) {
        this.irc = irc;
    }

    /**
     * URL to browse or download the sources.
     * 
     * @return
     *     The source
     */
    public URI getSource() {
        return source;
    }

    /**
     * URL to browse or download the sources.
     * 
     * @param source
     *     The source
     */
    public void setSource(URI source) {
        this.source = source;
    }

    /**
     * URL to the documentation.
     * 
     * @return
     *     The docs
     */
    public URI getDocs() {
        return docs;
    }

    /**
     * URL to the documentation.
     * 
     * @param docs
     *     The docs
     */
    public void setDocs(URI docs) {
        this.docs = docs;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(email).append(issues).append(forum).append(wiki).append(irc).append(source).append(docs).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Support) == false) {
            return false;
        }
        Support rhs = ((Support) other);
        return new EqualsBuilder().append(email, rhs.email).append(issues, rhs.issues).append(forum, rhs.forum).append(wiki, rhs.wiki).append(irc, rhs.irc).append(source, rhs.source).append(docs, rhs.docs).isEquals();
    }

}
