
package com.sourcegraph.toolchain.php.composer.schema;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Description of how the package can be autoloaded.
 * 
 */
@Generated("org.jsonschema2pojo")
public class Autoload {

    /**
     * This is a hash of namespaces (keys) and the directories they can be found into (values, can be arrays of paths) by the autoloader.
     * 
     */
    @SerializedName("psr-0")
    @Expose
    private Map<String, List<String>> psr0;
    /**
     * This is a hash of namespaces (keys) and the PSR-4 directories they can map to (values, can be arrays of paths) by the autoloader.
     * 
     */
    @SerializedName("psr-4")
    @Expose
    private Map<String, List<String>> psr4;
    /**
     * This is an array of directories that contain classes to be included in the class-map generation process.
     * 
     */
    @SerializedName("classmap")
    @Expose
    private List<String> classmap = new ArrayList<>();
    /**
     * This is an array of files that are always required on every request.
     * 
     */
    @SerializedName("files")
    @Expose
    private List<String> files = new ArrayList<>();
    /**
     * This is an array of patterns to exclude from autoload classmap generation. (e.g. "exclude-from-classmap": ["/test/", "/tests/", "/Tests/"]
     * 
     */
    @SerializedName("exclude-from-classmap")
    @Expose
    private List<Object> excludeFromClassmap = new ArrayList<Object>();

    /**
     * This is a hash of namespaces (keys) and the directories they can be found into (values, can be arrays of paths) by the autoloader.
     * 
     * @return
     *     The psr0
     */
    public Map<String, List<String>> getPsr0() {
        return psr0;
    }

    /**
     * This is a hash of namespaces (keys) and the directories they can be found into (values, can be arrays of paths) by the autoloader.
     * 
     * @param psr0
     *     The psr-0
     */
    public void setPsr0(Map<String, List<String>> psr0) {
        this.psr0 = psr0;
    }

    /**
     * This is a hash of namespaces (keys) and the PSR-4 directories they can map to (values, can be arrays of paths) by the autoloader.
     * 
     * @return
     *     The psr4
     */
    public Map<String, List<String>> getPsr4() {
        return psr4;
    }

    /**
     * This is a hash of namespaces (keys) and the PSR-4 directories they can map to (values, can be arrays of paths) by the autoloader.
     * 
     * @param psr4
     *     The psr-4
     */
    public void setPsr4(Map<String, List<String>> psr4) {
        this.psr4 = psr4;
    }

    /**
     * This is an array of directories that contain classes to be included in the class-map generation process.
     * 
     * @return
     *     The classmap
     */
    public List<String> getClassmap() {
        return classmap;
    }

    /**
     * This is an array of directories that contain classes to be included in the class-map generation process.
     * 
     * @param classmap
     *     The classmap
     */
    public void setClassmap(List<String> classmap) {
        this.classmap = classmap;
    }

    /**
     * This is an array of files that are always required on every request.
     * 
     * @return
     *     The files
     */
    public List<String> getFiles() {
        return files;
    }

    /**
     * This is an array of files that are always required on every request.
     * 
     * @param files
     *     The files
     */
    public void setFiles(List<String> files) {
        this.files = files;
    }

    /**
     * This is an array of patterns to exclude from autoload classmap generation. (e.g. "exclude-from-classmap": ["/test/", "/tests/", "/Tests/"]
     * 
     * @return
     *     The excludeFromClassmap
     */
    public List<Object> getExcludeFromClassmap() {
        return excludeFromClassmap;
    }

    /**
     * This is an array of patterns to exclude from autoload classmap generation. (e.g. "exclude-from-classmap": ["/test/", "/tests/", "/Tests/"]
     * 
     * @param excludeFromClassmap
     *     The exclude-from-classmap
     */
    public void setExcludeFromClassmap(List<Object> excludeFromClassmap) {
        this.excludeFromClassmap = excludeFromClassmap;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(psr0).append(psr4).append(classmap).append(files).append(excludeFromClassmap).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Autoload) == false) {
            return false;
        }
        Autoload rhs = ((Autoload) other);
        return new EqualsBuilder().append(psr0, rhs.psr0).append(psr4, rhs.psr4).append(classmap, rhs.classmap).append(files, rhs.files).append(excludeFromClassmap, rhs.excludeFromClassmap).isEquals();
    }

}
