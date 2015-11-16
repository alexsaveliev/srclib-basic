
package com.sourcegraph.toolchain.php.composer.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * Composer options.
 * 
 */
@Generated("org.jsonschema2pojo")
public class Config {

    /**
     * The timeout in seconds for process executions, defaults to 300 (5mins).
     * 
     */
    @SerializedName("process-timeout")
    @Expose
    private Integer processTimeout;
    /**
     * If true, the Composer autoloader will also look for classes in the PHP include path.
     * 
     */
    @SerializedName("use-include-path")
    @Expose
    private Boolean useIncludePath;
    /**
     * The install method Composer will prefer to use, defaults to auto and can be any of source, dist or auto.
     * 
     */
    @SerializedName("preferred-install")
    @Expose
    private String preferredInstall;
    /**
     * Composer allows repositories to define a notification URL, so that they get notified whenever a package from that repository is installed. This option allows you to disable that behaviour, defaults to true.
     * 
     */
    @SerializedName("notify-on-install")
    @Expose
    private Boolean notifyOnInstall;
    /**
     * A list of protocols to use for github.com clones, in priority order, defaults to ["git", "https", "http"].
     * 
     */
    @SerializedName("github-protocols")
    @Expose
    private List<String> githubProtocols = new ArrayList<String>();
    /**
     * A hash of domain name => github API oauth tokens, typically {"github.com":"<token>"}.
     * 
     */
    @SerializedName("github-oauth")
    @Expose
    private GithubOauth githubOauth;
    /**
     * A hash of domain name => {"username": "...", "password": "..."}.
     * 
     */
    @SerializedName("http-basic")
    @Expose
    private HttpBasic httpBasic;
    /**
     * What to do after prompting for authentication, one of: true (store), false (do not store) or "prompt" (ask every time), defaults to prompt.
     * 
     */
    @SerializedName("store-auths")
    @Expose
    private String storeAuths;
    /**
     * This is a hash of package name (keys) and version (values) that will be used to mock the platform packages on this machine.
     * 
     */
    @SerializedName("platform")
    @Expose
    private Platform platform;
    /**
     * The location where all packages are installed, defaults to "vendor".
     * 
     */
    @SerializedName("vendor-dir")
    @Expose
    private String vendorDir;
    /**
     * The location where all binaries are linked, defaults to "vendor/bin".
     * 
     */
    @SerializedName("bin-dir")
    @Expose
    private String binDir;
    /**
     * The location where all caches are located, defaults to "~/.composer/cache" on *nix and "%LOCALAPPDATA%\Composer" on windows.
     * 
     */
    @SerializedName("cache-dir")
    @Expose
    private String cacheDir;
    /**
     * The location where files (zip downloads) are cached, defaults to "{$cache-dir}/files".
     * 
     */
    @SerializedName("cache-files-dir")
    @Expose
    private String cacheFilesDir;
    /**
     * The location where repo (git/hg repo clones) are cached, defaults to "{$cache-dir}/repo".
     * 
     */
    @SerializedName("cache-repo-dir")
    @Expose
    private String cacheRepoDir;
    /**
     * The location where vcs infos (git clones, github api calls, etc. when reading vcs repos) are cached, defaults to "{$cache-dir}/vcs".
     * 
     */
    @SerializedName("cache-vcs-dir")
    @Expose
    private String cacheVcsDir;
    /**
     * The default cache time-to-live, defaults to 15552000 (6 months).
     * 
     */
    @SerializedName("cache-ttl")
    @Expose
    private Integer cacheTtl;
    /**
     * The cache time-to-live for files, defaults to the value of cache-ttl.
     * 
     */
    @SerializedName("cache-files-ttl")
    @Expose
    private Integer cacheFilesTtl;
    /**
     * The cache max size for the files cache, defaults to "300MiB".
     * 
     */
    @SerializedName("cache-files-maxsize")
    @Expose
    private String cacheFilesMaxsize;
    /**
     * The compatibility of the binaries, defaults to "auto" (automatically guessed) and can be "full" (compatible with both Windows and Unix-based systems).
     * 
     */
    @SerializedName("bin-compat")
    @Expose
    private Config.BinCompat binCompat;
    /**
     * The default style of handling dirty updates, defaults to false and can be any of true, false or "stash".
     * 
     */
    @SerializedName("discard-changes")
    @Expose
    private String discardChanges;
    /**
     * Optional string to be used as a suffix for the generated Composer autoloader. When null a random one will be generated.
     * 
     */
    @SerializedName("autoloader-suffix")
    @Expose
    private String autoloaderSuffix;
    /**
     * Always optimize when dumping the autoloader.
     * 
     */
    @SerializedName("optimize-autoloader")
    @Expose
    private Boolean optimizeAutoloader;
    /**
     * If false, the composer autoloader will not be prepended to existing autoloaders, defaults to true.
     * 
     */
    @SerializedName("prepend-autoloader")
    @Expose
    private Boolean prependAutoloader;
    /**
     * If true, the composer autoloader will not scan the filesystem for classes that are not found in the class map, defaults to false.
     * 
     */
    @SerializedName("classmap-authoritative")
    @Expose
    private Boolean classmapAuthoritative;
    /**
     * A list of domains to use in github mode. This is used for GitHub Enterprise setups, defaults to ["github.com"].
     * 
     */
    @SerializedName("github-domains")
    @Expose
    private List<String> githubDomains = new ArrayList<String>();
    /**
     * Defaults to true. If set to false, the OAuth tokens created to access the github API will have a date instead of the machine hostname.
     * 
     */
    @SerializedName("github-expose-hostname")
    @Expose
    private Boolean githubExposeHostname;
    /**
     * The default archiving format when not provided on cli, defaults to "tar".
     * 
     */
    @SerializedName("archive-format")
    @Expose
    private String archiveFormat;
    /**
     * The default archive path when not provided on cli, defaults to ".".
     * 
     */
    @SerializedName("archive-dir")
    @Expose
    private String archiveDir;

    /**
     * The timeout in seconds for process executions, defaults to 300 (5mins).
     * 
     * @return
     *     The processTimeout
     */
    public Integer getProcessTimeout() {
        return processTimeout;
    }

    /**
     * The timeout in seconds for process executions, defaults to 300 (5mins).
     * 
     * @param processTimeout
     *     The process-timeout
     */
    public void setProcessTimeout(Integer processTimeout) {
        this.processTimeout = processTimeout;
    }

    /**
     * If true, the Composer autoloader will also look for classes in the PHP include path.
     * 
     * @return
     *     The useIncludePath
     */
    public Boolean getUseIncludePath() {
        return useIncludePath;
    }

    /**
     * If true, the Composer autoloader will also look for classes in the PHP include path.
     * 
     * @param useIncludePath
     *     The use-include-path
     */
    public void setUseIncludePath(Boolean useIncludePath) {
        this.useIncludePath = useIncludePath;
    }

    /**
     * The install method Composer will prefer to use, defaults to auto and can be any of source, dist or auto.
     * 
     * @return
     *     The preferredInstall
     */
    public String getPreferredInstall() {
        return preferredInstall;
    }

    /**
     * The install method Composer will prefer to use, defaults to auto and can be any of source, dist or auto.
     * 
     * @param preferredInstall
     *     The preferred-install
     */
    public void setPreferredInstall(String preferredInstall) {
        this.preferredInstall = preferredInstall;
    }

    /**
     * Composer allows repositories to define a notification URL, so that they get notified whenever a package from that repository is installed. This option allows you to disable that behaviour, defaults to true.
     * 
     * @return
     *     The notifyOnInstall
     */
    public Boolean getNotifyOnInstall() {
        return notifyOnInstall;
    }

    /**
     * Composer allows repositories to define a notification URL, so that they get notified whenever a package from that repository is installed. This option allows you to disable that behaviour, defaults to true.
     * 
     * @param notifyOnInstall
     *     The notify-on-install
     */
    public void setNotifyOnInstall(Boolean notifyOnInstall) {
        this.notifyOnInstall = notifyOnInstall;
    }

    /**
     * A list of protocols to use for github.com clones, in priority order, defaults to ["git", "https", "http"].
     * 
     * @return
     *     The githubProtocols
     */
    public List<String> getGithubProtocols() {
        return githubProtocols;
    }

    /**
     * A list of protocols to use for github.com clones, in priority order, defaults to ["git", "https", "http"].
     * 
     * @param githubProtocols
     *     The github-protocols
     */
    public void setGithubProtocols(List<String> githubProtocols) {
        this.githubProtocols = githubProtocols;
    }

    /**
     * A hash of domain name => github API oauth tokens, typically {"github.com":"<token>"}.
     * 
     * @return
     *     The githubOauth
     */
    public GithubOauth getGithubOauth() {
        return githubOauth;
    }

    /**
     * A hash of domain name => github API oauth tokens, typically {"github.com":"<token>"}.
     * 
     * @param githubOauth
     *     The github-oauth
     */
    public void setGithubOauth(GithubOauth githubOauth) {
        this.githubOauth = githubOauth;
    }

    /**
     * A hash of domain name => {"username": "...", "password": "..."}.
     * 
     * @return
     *     The httpBasic
     */
    public HttpBasic getHttpBasic() {
        return httpBasic;
    }

    /**
     * A hash of domain name => {"username": "...", "password": "..."}.
     * 
     * @param httpBasic
     *     The http-basic
     */
    public void setHttpBasic(HttpBasic httpBasic) {
        this.httpBasic = httpBasic;
    }

    /**
     * What to do after prompting for authentication, one of: true (store), false (do not store) or "prompt" (ask every time), defaults to prompt.
     * 
     * @return
     *     The storeAuths
     */
    public String getStoreAuths() {
        return storeAuths;
    }

    /**
     * What to do after prompting for authentication, one of: true (store), false (do not store) or "prompt" (ask every time), defaults to prompt.
     * 
     * @param storeAuths
     *     The store-auths
     */
    public void setStoreAuths(String storeAuths) {
        this.storeAuths = storeAuths;
    }

    /**
     * This is a hash of package name (keys) and version (values) that will be used to mock the platform packages on this machine.
     * 
     * @return
     *     The platform
     */
    public Platform getPlatform() {
        return platform;
    }

    /**
     * This is a hash of package name (keys) and version (values) that will be used to mock the platform packages on this machine.
     * 
     * @param platform
     *     The platform
     */
    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    /**
     * The location where all packages are installed, defaults to "vendor".
     * 
     * @return
     *     The vendorDir
     */
    public String getVendorDir() {
        return vendorDir;
    }

    /**
     * The location where all packages are installed, defaults to "vendor".
     * 
     * @param vendorDir
     *     The vendor-dir
     */
    public void setVendorDir(String vendorDir) {
        this.vendorDir = vendorDir;
    }

    /**
     * The location where all binaries are linked, defaults to "vendor/bin".
     * 
     * @return
     *     The binDir
     */
    public String getBinDir() {
        return binDir;
    }

    /**
     * The location where all binaries are linked, defaults to "vendor/bin".
     * 
     * @param binDir
     *     The bin-dir
     */
    public void setBinDir(String binDir) {
        this.binDir = binDir;
    }

    /**
     * The location where all caches are located, defaults to "~/.composer/cache" on *nix and "%LOCALAPPDATA%\Composer" on windows.
     * 
     * @return
     *     The cacheDir
     */
    public String getCacheDir() {
        return cacheDir;
    }

    /**
     * The location where all caches are located, defaults to "~/.composer/cache" on *nix and "%LOCALAPPDATA%\Composer" on windows.
     * 
     * @param cacheDir
     *     The cache-dir
     */
    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * The location where files (zip downloads) are cached, defaults to "{$cache-dir}/files".
     * 
     * @return
     *     The cacheFilesDir
     */
    public String getCacheFilesDir() {
        return cacheFilesDir;
    }

    /**
     * The location where files (zip downloads) are cached, defaults to "{$cache-dir}/files".
     * 
     * @param cacheFilesDir
     *     The cache-files-dir
     */
    public void setCacheFilesDir(String cacheFilesDir) {
        this.cacheFilesDir = cacheFilesDir;
    }

    /**
     * The location where repo (git/hg repo clones) are cached, defaults to "{$cache-dir}/repo".
     * 
     * @return
     *     The cacheRepoDir
     */
    public String getCacheRepoDir() {
        return cacheRepoDir;
    }

    /**
     * The location where repo (git/hg repo clones) are cached, defaults to "{$cache-dir}/repo".
     * 
     * @param cacheRepoDir
     *     The cache-repo-dir
     */
    public void setCacheRepoDir(String cacheRepoDir) {
        this.cacheRepoDir = cacheRepoDir;
    }

    /**
     * The location where vcs infos (git clones, github api calls, etc. when reading vcs repos) are cached, defaults to "{$cache-dir}/vcs".
     * 
     * @return
     *     The cacheVcsDir
     */
    public String getCacheVcsDir() {
        return cacheVcsDir;
    }

    /**
     * The location where vcs infos (git clones, github api calls, etc. when reading vcs repos) are cached, defaults to "{$cache-dir}/vcs".
     * 
     * @param cacheVcsDir
     *     The cache-vcs-dir
     */
    public void setCacheVcsDir(String cacheVcsDir) {
        this.cacheVcsDir = cacheVcsDir;
    }

    /**
     * The default cache time-to-live, defaults to 15552000 (6 months).
     * 
     * @return
     *     The cacheTtl
     */
    public Integer getCacheTtl() {
        return cacheTtl;
    }

    /**
     * The default cache time-to-live, defaults to 15552000 (6 months).
     * 
     * @param cacheTtl
     *     The cache-ttl
     */
    public void setCacheTtl(Integer cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    /**
     * The cache time-to-live for files, defaults to the value of cache-ttl.
     * 
     * @return
     *     The cacheFilesTtl
     */
    public Integer getCacheFilesTtl() {
        return cacheFilesTtl;
    }

    /**
     * The cache time-to-live for files, defaults to the value of cache-ttl.
     * 
     * @param cacheFilesTtl
     *     The cache-files-ttl
     */
    public void setCacheFilesTtl(Integer cacheFilesTtl) {
        this.cacheFilesTtl = cacheFilesTtl;
    }

    /**
     * The cache max size for the files cache, defaults to "300MiB".
     * 
     * @return
     *     The cacheFilesMaxsize
     */
    public String getCacheFilesMaxsize() {
        return cacheFilesMaxsize;
    }

    /**
     * The cache max size for the files cache, defaults to "300MiB".
     * 
     * @param cacheFilesMaxsize
     *     The cache-files-maxsize
     */
    public void setCacheFilesMaxsize(String cacheFilesMaxsize) {
        this.cacheFilesMaxsize = cacheFilesMaxsize;
    }

    /**
     * The compatibility of the binaries, defaults to "auto" (automatically guessed) and can be "full" (compatible with both Windows and Unix-based systems).
     * 
     * @return
     *     The binCompat
     */
    public Config.BinCompat getBinCompat() {
        return binCompat;
    }

    /**
     * The compatibility of the binaries, defaults to "auto" (automatically guessed) and can be "full" (compatible with both Windows and Unix-based systems).
     * 
     * @param binCompat
     *     The bin-compat
     */
    public void setBinCompat(Config.BinCompat binCompat) {
        this.binCompat = binCompat;
    }

    /**
     * The default style of handling dirty updates, defaults to false and can be any of true, false or "stash".
     * 
     * @return
     *     The discardChanges
     */
    public String getDiscardChanges() {
        return discardChanges;
    }

    /**
     * The default style of handling dirty updates, defaults to false and can be any of true, false or "stash".
     * 
     * @param discardChanges
     *     The discard-changes
     */
    public void setDiscardChanges(String discardChanges) {
        this.discardChanges = discardChanges;
    }

    /**
     * Optional string to be used as a suffix for the generated Composer autoloader. When null a random one will be generated.
     * 
     * @return
     *     The autoloaderSuffix
     */
    public String getAutoloaderSuffix() {
        return autoloaderSuffix;
    }

    /**
     * Optional string to be used as a suffix for the generated Composer autoloader. When null a random one will be generated.
     * 
     * @param autoloaderSuffix
     *     The autoloader-suffix
     */
    public void setAutoloaderSuffix(String autoloaderSuffix) {
        this.autoloaderSuffix = autoloaderSuffix;
    }

    /**
     * Always optimize when dumping the autoloader.
     * 
     * @return
     *     The optimizeAutoloader
     */
    public Boolean getOptimizeAutoloader() {
        return optimizeAutoloader;
    }

    /**
     * Always optimize when dumping the autoloader.
     * 
     * @param optimizeAutoloader
     *     The optimize-autoloader
     */
    public void setOptimizeAutoloader(Boolean optimizeAutoloader) {
        this.optimizeAutoloader = optimizeAutoloader;
    }

    /**
     * If false, the composer autoloader will not be prepended to existing autoloaders, defaults to true.
     * 
     * @return
     *     The prependAutoloader
     */
    public Boolean getPrependAutoloader() {
        return prependAutoloader;
    }

    /**
     * If false, the composer autoloader will not be prepended to existing autoloaders, defaults to true.
     * 
     * @param prependAutoloader
     *     The prepend-autoloader
     */
    public void setPrependAutoloader(Boolean prependAutoloader) {
        this.prependAutoloader = prependAutoloader;
    }

    /**
     * If true, the composer autoloader will not scan the filesystem for classes that are not found in the class map, defaults to false.
     * 
     * @return
     *     The classmapAuthoritative
     */
    public Boolean getClassmapAuthoritative() {
        return classmapAuthoritative;
    }

    /**
     * If true, the composer autoloader will not scan the filesystem for classes that are not found in the class map, defaults to false.
     * 
     * @param classmapAuthoritative
     *     The classmap-authoritative
     */
    public void setClassmapAuthoritative(Boolean classmapAuthoritative) {
        this.classmapAuthoritative = classmapAuthoritative;
    }

    /**
     * A list of domains to use in github mode. This is used for GitHub Enterprise setups, defaults to ["github.com"].
     * 
     * @return
     *     The githubDomains
     */
    public List<String> getGithubDomains() {
        return githubDomains;
    }

    /**
     * A list of domains to use in github mode. This is used for GitHub Enterprise setups, defaults to ["github.com"].
     * 
     * @param githubDomains
     *     The github-domains
     */
    public void setGithubDomains(List<String> githubDomains) {
        this.githubDomains = githubDomains;
    }

    /**
     * Defaults to true. If set to false, the OAuth tokens created to access the github API will have a date instead of the machine hostname.
     * 
     * @return
     *     The githubExposeHostname
     */
    public Boolean getGithubExposeHostname() {
        return githubExposeHostname;
    }

    /**
     * Defaults to true. If set to false, the OAuth tokens created to access the github API will have a date instead of the machine hostname.
     * 
     * @param githubExposeHostname
     *     The github-expose-hostname
     */
    public void setGithubExposeHostname(Boolean githubExposeHostname) {
        this.githubExposeHostname = githubExposeHostname;
    }

    /**
     * The default archiving format when not provided on cli, defaults to "tar".
     * 
     * @return
     *     The archiveFormat
     */
    public String getArchiveFormat() {
        return archiveFormat;
    }

    /**
     * The default archiving format when not provided on cli, defaults to "tar".
     * 
     * @param archiveFormat
     *     The archive-format
     */
    public void setArchiveFormat(String archiveFormat) {
        this.archiveFormat = archiveFormat;
    }

    /**
     * The default archive path when not provided on cli, defaults to ".".
     * 
     * @return
     *     The archiveDir
     */
    public String getArchiveDir() {
        return archiveDir;
    }

    /**
     * The default archive path when not provided on cli, defaults to ".".
     * 
     * @param archiveDir
     *     The archive-dir
     */
    public void setArchiveDir(String archiveDir) {
        this.archiveDir = archiveDir;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(processTimeout).append(useIncludePath).append(preferredInstall).append(notifyOnInstall).append(githubProtocols).append(githubOauth).append(httpBasic).append(storeAuths).append(platform).append(vendorDir).append(binDir).append(cacheDir).append(cacheFilesDir).append(cacheRepoDir).append(cacheVcsDir).append(cacheTtl).append(cacheFilesTtl).append(cacheFilesMaxsize).append(binCompat).append(discardChanges).append(autoloaderSuffix).append(optimizeAutoloader).append(prependAutoloader).append(classmapAuthoritative).append(githubDomains).append(githubExposeHostname).append(archiveFormat).append(archiveDir).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Config) == false) {
            return false;
        }
        Config rhs = ((Config) other);
        return new EqualsBuilder().append(processTimeout, rhs.processTimeout).append(useIncludePath, rhs.useIncludePath).append(preferredInstall, rhs.preferredInstall).append(notifyOnInstall, rhs.notifyOnInstall).append(githubProtocols, rhs.githubProtocols).append(githubOauth, rhs.githubOauth).append(httpBasic, rhs.httpBasic).append(storeAuths, rhs.storeAuths).append(platform, rhs.platform).append(vendorDir, rhs.vendorDir).append(binDir, rhs.binDir).append(cacheDir, rhs.cacheDir).append(cacheFilesDir, rhs.cacheFilesDir).append(cacheRepoDir, rhs.cacheRepoDir).append(cacheVcsDir, rhs.cacheVcsDir).append(cacheTtl, rhs.cacheTtl).append(cacheFilesTtl, rhs.cacheFilesTtl).append(cacheFilesMaxsize, rhs.cacheFilesMaxsize).append(binCompat, rhs.binCompat).append(discardChanges, rhs.discardChanges).append(autoloaderSuffix, rhs.autoloaderSuffix).append(optimizeAutoloader, rhs.optimizeAutoloader).append(prependAutoloader, rhs.prependAutoloader).append(classmapAuthoritative, rhs.classmapAuthoritative).append(githubDomains, rhs.githubDomains).append(githubExposeHostname, rhs.githubExposeHostname).append(archiveFormat, rhs.archiveFormat).append(archiveDir, rhs.archiveDir).isEquals();
    }

    @Generated("org.jsonschema2pojo")
    public static enum BinCompat {

        @SerializedName("auto")
        AUTO("auto"),
        @SerializedName("full")
        FULL("full");
        private final String value;
        private static Map<String, Config.BinCompat> constants = new HashMap<String, Config.BinCompat>();

        static {
            for (Config.BinCompat c: values()) {
                constants.put(c.value, c);
            }
        }

        private BinCompat(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static Config.BinCompat fromValue(String value) {
            Config.BinCompat constant = constants.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
