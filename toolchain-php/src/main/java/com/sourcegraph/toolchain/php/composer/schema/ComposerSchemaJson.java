
package com.sourcegraph.toolchain.php.composer.schema;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Generated;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Generated("org.jsonschema2pojo")
public class ComposerSchemaJson {

    /**
     * Package name, including 'vendor-name/' prefix.
     * (Required)
     * 
     */
    @SerializedName("name")
    @Expose
    private String name;
    /**
     * Package type, either 'library' for common packages, 'composer-plugin' for plugins, 'metapackage' for empty packages, or a custom type ([a-z0-9-]+) defined by whatever project this package applies to.
     * 
     */
    @SerializedName("type")
    @Expose
    private String type;
    /**
     * DEPRECATED: Forces the package to be installed into the given subdirectory path. This is used for autoloading PSR-0 packages that do not contain their full path. Use forward slashes for cross-platform compatibility.
     * 
     */
    @SerializedName("target-dir")
    @Expose
    private String targetDir;
    /**
     * Short package description.
     * (Required)
     * 
     */
    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("keywords")
    @Expose
    private List<String> keywords = new ArrayList<String>();
    /**
     * Homepage URL for the project.
     * 
     */
    @SerializedName("homepage")
    @Expose
    private URI homepage;
    /**
     * Package version, see https://getcomposer.org/doc/04-schema.md#version for more info on valid schemes.
     * 
     */
    @SerializedName("version")
    @Expose
    private String version;
    /**
     * Package release date, in 'YYYY-MM-DD', 'YYYY-MM-DD HH:MM:SS' or 'YYYY-MM-DDTHH:MM:SSZ' format.
     * 
     */
    @SerializedName("time")
    @Expose
    private String time;
    /**
     * License name. Or an array of license names.
     * 
     */
    @SerializedName("license")
    @Expose
    private List<String> licenses = new ArrayList<>();
    /**
     * List of authors that contributed to the package. This is typically the main maintainers, not the full list.
     * 
     */
    @SerializedName("authors")
    @Expose
    private List<Author> authors = new ArrayList<Author>();
    /**
     * This is a hash of package name (keys) and version constraints (values) that are required to run this package.
     * 
     */
    @SerializedName("require")
    @Expose
    private Require require;
    /**
     * This is a hash of package name (keys) and version constraints (values) that can be replaced by this package.
     * 
     */
    @SerializedName("replace")
    @Expose
    private Replace replace;
    /**
     * This is a hash of package name (keys) and version constraints (values) that conflict with this package.
     * 
     */
    @SerializedName("conflict")
    @Expose
    private Conflict conflict;
    /**
     * This is a hash of package name (keys) and version constraints (values) that this package provides in addition to this package's name.
     * 
     */
    @SerializedName("provide")
    @Expose
    private Provide provide;
    /**
     * This is a hash of package name (keys) and version constraints (values) that this package requires for developing it (testing tools and such).
     * 
     */
    @SerializedName("require-dev")
    @Expose
    private RequireDev requireDev;
    /**
     * This is a hash of package name (keys) and descriptions (values) that this package suggests work well with it (this will be suggested to the user during installation).
     * 
     */
    @SerializedName("suggest")
    @Expose
    private Suggest suggest;
    /**
     * Composer options.
     * 
     */
    @SerializedName("config")
    @Expose
    private Config config;
    /**
     * Arbitrary extra data that can be used by plugins, for example, package of type composer-plugin may have a 'class' key defining an installer class name.
     * 
     */
    @SerializedName("extra")
    @Expose
    private Extra extra;
    /**
     * Description of how the package can be autoloaded.
     * 
     */
    @SerializedName("autoload")
    @Expose
    private Autoload autoload;
    /**
     * Description of additional autoload rules for development purpose (eg. a test suite).
     * 
     */
    @SerializedName("autoload-dev")
    @Expose
    private AutoloadDev autoloadDev;
    /**
     * Options for creating package archives for distribution.
     * 
     */
    @SerializedName("archive")
    @Expose
    private Archive archive;
    /**
     * A set of additional repositories where packages can be found.
     * 
     */
    @SerializedName("repositories")
    @Expose
    private Repositories repositories;
    /**
     * The minimum stability the packages must have to be install-able. Possible values are: dev, alpha, beta, RC, stable.
     * 
     */
    @SerializedName("minimum-stability")
    @Expose
    private String minimumStability;
    /**
     * If set to true, stable packages will be preferred to dev packages when possible, even if the minimum-stability allows unstable packages.
     * 
     */
    @SerializedName("prefer-stable")
    @Expose
    private Boolean preferStable;
    /**
     * A set of files that should be treated as binaries and symlinked into bin-dir (from config).
     * 
     */
    @SerializedName("bin")
    @Expose
    private List<String> bin = new ArrayList<String>();
    /**
     * DEPRECATED: A list of directories which should get added to PHP's include path. This is only present to support legacy projects, and all new code should preferably use autoloading.
     * 
     */
    @SerializedName("include-path")
    @Expose
    private List<String> includePath = new ArrayList<String>();
    /**
     * Scripts listeners that will be executed before/after some events.
     * 
     */
    @SerializedName("scripts")
    @Expose
    private Scripts scripts;
    @SerializedName("support")
    @Expose
    private Support support;
    /**
     * A set of string or regex patterns for non-numeric branch names that will not be handled as feature branches.
     * 
     */
    @SerializedName("non-feature-branches")
    @Expose
    private List<String> nonFeatureBranches = new ArrayList<String>();

    /**
     * Package name, including 'vendor-name/' prefix.
     * (Required)
     * 
     * @return
     *     The name
     */
    public String getName() {
        return name;
    }

    /**
     * Package name, including 'vendor-name/' prefix.
     * (Required)
     * 
     * @param name
     *     The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Package type, either 'library' for common packages, 'composer-plugin' for plugins, 'metapackage' for empty packages, or a custom type ([a-z0-9-]+) defined by whatever project this package applies to.
     * 
     * @return
     *     The type
     */
    public String getType() {
        return type;
    }

    /**
     * Package type, either 'library' for common packages, 'composer-plugin' for plugins, 'metapackage' for empty packages, or a custom type ([a-z0-9-]+) defined by whatever project this package applies to.
     * 
     * @param type
     *     The type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * DEPRECATED: Forces the package to be installed into the given subdirectory path. This is used for autoloading PSR-0 packages that do not contain their full path. Use forward slashes for cross-platform compatibility.
     * 
     * @return
     *     The targetDir
     */
    public String getTargetDir() {
        return targetDir;
    }

    /**
     * DEPRECATED: Forces the package to be installed into the given subdirectory path. This is used for autoloading PSR-0 packages that do not contain their full path. Use forward slashes for cross-platform compatibility.
     * 
     * @param targetDir
     *     The target-dir
     */
    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    /**
     * Short package description.
     * (Required)
     * 
     * @return
     *     The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Short package description.
     * (Required)
     * 
     * @param description
     *     The description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * @return
     *     The keywords
     */
    public List<String> getKeywords() {
        return keywords;
    }

    /**
     * 
     * @param keywords
     *     The keywords
     */
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    /**
     * Homepage URL for the project.
     * 
     * @return
     *     The homepage
     */
    public URI getHomepage() {
        return homepage;
    }

    /**
     * Homepage URL for the project.
     * 
     * @param homepage
     *     The homepage
     */
    public void setHomepage(URI homepage) {
        this.homepage = homepage;
    }

    /**
     * Package version, see https://getcomposer.org/doc/04-schema.md#version for more info on valid schemes.
     * 
     * @return
     *     The version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Package version, see https://getcomposer.org/doc/04-schema.md#version for more info on valid schemes.
     * 
     * @param version
     *     The version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Package release date, in 'YYYY-MM-DD', 'YYYY-MM-DD HH:MM:SS' or 'YYYY-MM-DDTHH:MM:SSZ' format.
     * 
     * @return
     *     The time
     */
    public String getTime() {
        return time;
    }

    /**
     * Package release date, in 'YYYY-MM-DD', 'YYYY-MM-DD HH:MM:SS' or 'YYYY-MM-DDTHH:MM:SSZ' format.
     * 
     * @param time
     *     The time
     */
    public void setTime(String time) {
        this.time = time;
    }

    /**
     * License name. Or an array of license names.
     * 
     * @return
     *     The license
     */
    public List<String> getLicenses() {
        return licenses;
    }

    /**
     * License name. Or an array of license names.
     * 
     * @param license
     *     The license
     */
    public void setLicenses(List<String> licenses) {
        this.licenses = licenses;
    }

    /**
     * List of authors that contributed to the package. This is typically the main maintainers, not the full list.
     * 
     * @return
     *     The authors
     */
    public List<Author> getAuthors() {
        return authors;
    }

    /**
     * List of authors that contributed to the package. This is typically the main maintainers, not the full list.
     * 
     * @param authors
     *     The authors
     */
    public void setAuthors(List<Author> authors) {
        this.authors = authors;
    }

    /**
     * This is a hash of package name (keys) and version constraints (values) that are required to run this package.
     * 
     * @return
     *     The require
     */
    public Require getRequire() {
        return require;
    }

    /**
     * This is a hash of package name (keys) and version constraints (values) that are required to run this package.
     * 
     * @param require
     *     The require
     */
    public void setRequire(Require require) {
        this.require = require;
    }

    /**
     * This is a hash of package name (keys) and version constraints (values) that can be replaced by this package.
     * 
     * @return
     *     The replace
     */
    public Replace getReplace() {
        return replace;
    }

    /**
     * This is a hash of package name (keys) and version constraints (values) that can be replaced by this package.
     * 
     * @param replace
     *     The replace
     */
    public void setReplace(Replace replace) {
        this.replace = replace;
    }

    /**
     * This is a hash of package name (keys) and version constraints (values) that conflict with this package.
     * 
     * @return
     *     The conflict
     */
    public Conflict getConflict() {
        return conflict;
    }

    /**
     * This is a hash of package name (keys) and version constraints (values) that conflict with this package.
     * 
     * @param conflict
     *     The conflict
     */
    public void setConflict(Conflict conflict) {
        this.conflict = conflict;
    }

    /**
     * This is a hash of package name (keys) and version constraints (values) that this package provides in addition to this package's name.
     * 
     * @return
     *     The provide
     */
    public Provide getProvide() {
        return provide;
    }

    /**
     * This is a hash of package name (keys) and version constraints (values) that this package provides in addition to this package's name.
     * 
     * @param provide
     *     The provide
     */
    public void setProvide(Provide provide) {
        this.provide = provide;
    }

    /**
     * This is a hash of package name (keys) and version constraints (values) that this package requires for developing it (testing tools and such).
     * 
     * @return
     *     The requireDev
     */
    public RequireDev getRequireDev() {
        return requireDev;
    }

    /**
     * This is a hash of package name (keys) and version constraints (values) that this package requires for developing it (testing tools and such).
     * 
     * @param requireDev
     *     The require-dev
     */
    public void setRequireDev(RequireDev requireDev) {
        this.requireDev = requireDev;
    }

    /**
     * This is a hash of package name (keys) and descriptions (values) that this package suggests work well with it (this will be suggested to the user during installation).
     * 
     * @return
     *     The suggest
     */
    public Suggest getSuggest() {
        return suggest;
    }

    /**
     * This is a hash of package name (keys) and descriptions (values) that this package suggests work well with it (this will be suggested to the user during installation).
     * 
     * @param suggest
     *     The suggest
     */
    public void setSuggest(Suggest suggest) {
        this.suggest = suggest;
    }

    /**
     * Composer options.
     * 
     * @return
     *     The config
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Composer options.
     * 
     * @param config
     *     The config
     */
    public void setConfig(Config config) {
        this.config = config;
    }

    /**
     * Arbitrary extra data that can be used by plugins, for example, package of type composer-plugin may have a 'class' key defining an installer class name.
     * 
     * @return
     *     The extra
     */
    public Extra getExtra() {
        return extra;
    }

    /**
     * Arbitrary extra data that can be used by plugins, for example, package of type composer-plugin may have a 'class' key defining an installer class name.
     * 
     * @param extra
     *     The extra
     */
    public void setExtra(Extra extra) {
        this.extra = extra;
    }

    /**
     * Description of how the package can be autoloaded.
     * 
     * @return
     *     The autoload
     */
    public Autoload getAutoload() {
        return autoload;
    }

    /**
     * Description of how the package can be autoloaded.
     * 
     * @param autoload
     *     The autoload
     */
    public void setAutoload(Autoload autoload) {
        this.autoload = autoload;
    }

    /**
     * Description of additional autoload rules for development purpose (eg. a test suite).
     * 
     * @return
     *     The autoloadDev
     */
    public AutoloadDev getAutoloadDev() {
        return autoloadDev;
    }

    /**
     * Description of additional autoload rules for development purpose (eg. a test suite).
     * 
     * @param autoloadDev
     *     The autoload-dev
     */
    public void setAutoloadDev(AutoloadDev autoloadDev) {
        this.autoloadDev = autoloadDev;
    }

    /**
     * Options for creating package archives for distribution.
     * 
     * @return
     *     The archive
     */
    public Archive getArchive() {
        return archive;
    }

    /**
     * Options for creating package archives for distribution.
     * 
     * @param archive
     *     The archive
     */
    public void setArchive(Archive archive) {
        this.archive = archive;
    }

    /**
     * A set of additional repositories where packages can be found.
     * 
     * @return
     *     The repositories
     */
    public Repositories getRepositories() {
        return repositories;
    }

    /**
     * A set of additional repositories where packages can be found.
     * 
     * @param repositories
     *     The repositories
     */
    public void setRepositories(Repositories repositories) {
        this.repositories = repositories;
    }

    /**
     * The minimum stability the packages must have to be install-able. Possible values are: dev, alpha, beta, RC, stable.
     * 
     * @return
     *     The minimumStability
     */
    public String getMinimumStability() {
        return minimumStability;
    }

    /**
     * The minimum stability the packages must have to be install-able. Possible values are: dev, alpha, beta, RC, stable.
     * 
     * @param minimumStability
     *     The minimum-stability
     */
    public void setMinimumStability(String minimumStability) {
        this.minimumStability = minimumStability;
    }

    /**
     * If set to true, stable packages will be preferred to dev packages when possible, even if the minimum-stability allows unstable packages.
     * 
     * @return
     *     The preferStable
     */
    public Boolean getPreferStable() {
        return preferStable;
    }

    /**
     * If set to true, stable packages will be preferred to dev packages when possible, even if the minimum-stability allows unstable packages.
     * 
     * @param preferStable
     *     The prefer-stable
     */
    public void setPreferStable(Boolean preferStable) {
        this.preferStable = preferStable;
    }

    /**
     * A set of files that should be treated as binaries and symlinked into bin-dir (from config).
     * 
     * @return
     *     The bin
     */
    public List<String> getBin() {
        return bin;
    }

    /**
     * A set of files that should be treated as binaries and symlinked into bin-dir (from config).
     * 
     * @param bin
     *     The bin
     */
    public void setBin(List<String> bin) {
        this.bin = bin;
    }

    /**
     * DEPRECATED: A list of directories which should get added to PHP's include path. This is only present to support legacy projects, and all new code should preferably use autoloading.
     * 
     * @return
     *     The includePath
     */
    public List<String> getIncludePath() {
        return includePath;
    }

    /**
     * DEPRECATED: A list of directories which should get added to PHP's include path. This is only present to support legacy projects, and all new code should preferably use autoloading.
     * 
     * @param includePath
     *     The include-path
     */
    public void setIncludePath(List<String> includePath) {
        this.includePath = includePath;
    }

    /**
     * Scripts listeners that will be executed before/after some events.
     * 
     * @return
     *     The scripts
     */
    public Scripts getScripts() {
        return scripts;
    }

    /**
     * Scripts listeners that will be executed before/after some events.
     * 
     * @param scripts
     *     The scripts
     */
    public void setScripts(Scripts scripts) {
        this.scripts = scripts;
    }

    /**
     * 
     * @return
     *     The support
     */
    public Support getSupport() {
        return support;
    }

    /**
     * 
     * @param support
     *     The support
     */
    public void setSupport(Support support) {
        this.support = support;
    }

    /**
     * A set of string or regex patterns for non-numeric branch names that will not be handled as feature branches.
     * 
     * @return
     *     The nonFeatureBranches
     */
    public List<String> getNonFeatureBranches() {
        return nonFeatureBranches;
    }

    /**
     * A set of string or regex patterns for non-numeric branch names that will not be handled as feature branches.
     * 
     * @param nonFeatureBranches
     *     The non-feature-branches
     */
    public void setNonFeatureBranches(List<String> nonFeatureBranches) {
        this.nonFeatureBranches = nonFeatureBranches;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(type).append(targetDir).append(description).append(keywords).append(homepage).append(version).append(time).append(licenses).append(authors).append(require).append(replace).append(conflict).append(provide).append(requireDev).append(suggest).append(config).append(extra).append(autoload).append(autoloadDev).append(archive).append(repositories).append(minimumStability).append(preferStable).append(bin).append(includePath).append(scripts).append(support).append(nonFeatureBranches).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ComposerSchemaJson) == false) {
            return false;
        }
        ComposerSchemaJson rhs = ((ComposerSchemaJson) other);
        return new EqualsBuilder().append(name, rhs.name).append(type, rhs.type).append(targetDir, rhs.targetDir).append(description, rhs.description).append(keywords, rhs.keywords).append(homepage, rhs.homepage).append(version, rhs.version).append(time, rhs.time).append(licenses, rhs.licenses).append(authors, rhs.authors).append(require, rhs.require).append(replace, rhs.replace).append(conflict, rhs.conflict).append(provide, rhs.provide).append(requireDev, rhs.requireDev).append(suggest, rhs.suggest).append(config, rhs.config).append(extra, rhs.extra).append(autoload, rhs.autoload).append(autoloadDev, rhs.autoloadDev).append(archive, rhs.archive).append(repositories, rhs.repositories).append(minimumStability, rhs.minimumStability).append(preferStable, rhs.preferStable).append(bin, rhs.bin).append(includePath, rhs.includePath).append(scripts, rhs.scripts).append(support, rhs.support).append(nonFeatureBranches, rhs.nonFeatureBranches).isEquals();
    }

}
