
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
 * Scripts listeners that will be executed before/after some events.
 * 
 */
@Generated("org.jsonschema2pojo")
public class Scripts {

    /**
     * Occurs before the install command is executed, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("pre-install-cmd")
    @Expose
    private List<Object> preInstallCmd = new ArrayList<Object>();
    /**
     * Occurs after the install command is executed, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("post-install-cmd")
    @Expose
    private List<Object> postInstallCmd = new ArrayList<Object>();
    /**
     * Occurs before the update command is executed, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("pre-update-cmd")
    @Expose
    private List<Object> preUpdateCmd = new ArrayList<Object>();
    /**
     * Occurs after the update command is executed, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("post-update-cmd")
    @Expose
    private List<Object> postUpdateCmd = new ArrayList<Object>();
    /**
     * Occurs before the status command is executed, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("pre-status-cmd")
    @Expose
    private List<Object> preStatusCmd = new ArrayList<Object>();
    /**
     * Occurs after the status command is executed, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("post-status-cmd")
    @Expose
    private List<Object> postStatusCmd = new ArrayList<Object>();
    /**
     * Occurs before a package is installed, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("pre-package-install")
    @Expose
    private List<Object> prePackageInstall = new ArrayList<Object>();
    /**
     * Occurs after a package is installed, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("post-package-install")
    @Expose
    private List<Object> postPackageInstall = new ArrayList<Object>();
    /**
     * Occurs before a package is updated, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("pre-package-update")
    @Expose
    private List<Object> prePackageUpdate = new ArrayList<Object>();
    /**
     * Occurs after a package is updated, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("post-package-update")
    @Expose
    private List<Object> postPackageUpdate = new ArrayList<Object>();
    /**
     * Occurs before a package has been uninstalled, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("pre-package-uninstall")
    @Expose
    private List<Object> prePackageUninstall = new ArrayList<Object>();
    /**
     * Occurs after a package has been uninstalled, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("post-package-uninstall")
    @Expose
    private List<Object> postPackageUninstall = new ArrayList<Object>();
    /**
     * Occurs before the autoloader is dumped, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("pre-autoload-dump")
    @Expose
    private List<Object> preAutoloadDump = new ArrayList<Object>();
    /**
     * Occurs after the autoloader is dumped, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("post-autoload-dump")
    @Expose
    private List<Object> postAutoloadDump = new ArrayList<Object>();
    /**
     * Occurs after the root-package is installed, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("post-root-package-install")
    @Expose
    private List<Object> postRootPackageInstall = new ArrayList<Object>();
    /**
     * Occurs after the create-project command is executed, contains one or more Class::method callables or shell commands.
     * 
     */
    @SerializedName("post-create-project-cmd")
    @Expose
    private List<Object> postCreateProjectCmd = new ArrayList<Object>();

    /**
     * Occurs before the install command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The preInstallCmd
     */
    public List<Object> getPreInstallCmd() {
        return preInstallCmd;
    }

    /**
     * Occurs before the install command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @param preInstallCmd
     *     The pre-install-cmd
     */
    public void setPreInstallCmd(List<Object> preInstallCmd) {
        this.preInstallCmd = preInstallCmd;
    }

    /**
     * Occurs after the install command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The postInstallCmd
     */
    public List<Object> getPostInstallCmd() {
        return postInstallCmd;
    }

    /**
     * Occurs after the install command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @param postInstallCmd
     *     The post-install-cmd
     */
    public void setPostInstallCmd(List<Object> postInstallCmd) {
        this.postInstallCmd = postInstallCmd;
    }

    /**
     * Occurs before the update command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The preUpdateCmd
     */
    public List<Object> getPreUpdateCmd() {
        return preUpdateCmd;
    }

    /**
     * Occurs before the update command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @param preUpdateCmd
     *     The pre-update-cmd
     */
    public void setPreUpdateCmd(List<Object> preUpdateCmd) {
        this.preUpdateCmd = preUpdateCmd;
    }

    /**
     * Occurs after the update command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The postUpdateCmd
     */
    public List<Object> getPostUpdateCmd() {
        return postUpdateCmd;
    }

    /**
     * Occurs after the update command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @param postUpdateCmd
     *     The post-update-cmd
     */
    public void setPostUpdateCmd(List<Object> postUpdateCmd) {
        this.postUpdateCmd = postUpdateCmd;
    }

    /**
     * Occurs before the status command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The preStatusCmd
     */
    public List<Object> getPreStatusCmd() {
        return preStatusCmd;
    }

    /**
     * Occurs before the status command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @param preStatusCmd
     *     The pre-status-cmd
     */
    public void setPreStatusCmd(List<Object> preStatusCmd) {
        this.preStatusCmd = preStatusCmd;
    }

    /**
     * Occurs after the status command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The postStatusCmd
     */
    public List<Object> getPostStatusCmd() {
        return postStatusCmd;
    }

    /**
     * Occurs after the status command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @param postStatusCmd
     *     The post-status-cmd
     */
    public void setPostStatusCmd(List<Object> postStatusCmd) {
        this.postStatusCmd = postStatusCmd;
    }

    /**
     * Occurs before a package is installed, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The prePackageInstall
     */
    public List<Object> getPrePackageInstall() {
        return prePackageInstall;
    }

    /**
     * Occurs before a package is installed, contains one or more Class::method callables or shell commands.
     * 
     * @param prePackageInstall
     *     The pre-package-install
     */
    public void setPrePackageInstall(List<Object> prePackageInstall) {
        this.prePackageInstall = prePackageInstall;
    }

    /**
     * Occurs after a package is installed, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The postPackageInstall
     */
    public List<Object> getPostPackageInstall() {
        return postPackageInstall;
    }

    /**
     * Occurs after a package is installed, contains one or more Class::method callables or shell commands.
     * 
     * @param postPackageInstall
     *     The post-package-install
     */
    public void setPostPackageInstall(List<Object> postPackageInstall) {
        this.postPackageInstall = postPackageInstall;
    }

    /**
     * Occurs before a package is updated, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The prePackageUpdate
     */
    public List<Object> getPrePackageUpdate() {
        return prePackageUpdate;
    }

    /**
     * Occurs before a package is updated, contains one or more Class::method callables or shell commands.
     * 
     * @param prePackageUpdate
     *     The pre-package-update
     */
    public void setPrePackageUpdate(List<Object> prePackageUpdate) {
        this.prePackageUpdate = prePackageUpdate;
    }

    /**
     * Occurs after a package is updated, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The postPackageUpdate
     */
    public List<Object> getPostPackageUpdate() {
        return postPackageUpdate;
    }

    /**
     * Occurs after a package is updated, contains one or more Class::method callables or shell commands.
     * 
     * @param postPackageUpdate
     *     The post-package-update
     */
    public void setPostPackageUpdate(List<Object> postPackageUpdate) {
        this.postPackageUpdate = postPackageUpdate;
    }

    /**
     * Occurs before a package has been uninstalled, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The prePackageUninstall
     */
    public List<Object> getPrePackageUninstall() {
        return prePackageUninstall;
    }

    /**
     * Occurs before a package has been uninstalled, contains one or more Class::method callables or shell commands.
     * 
     * @param prePackageUninstall
     *     The pre-package-uninstall
     */
    public void setPrePackageUninstall(List<Object> prePackageUninstall) {
        this.prePackageUninstall = prePackageUninstall;
    }

    /**
     * Occurs after a package has been uninstalled, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The postPackageUninstall
     */
    public List<Object> getPostPackageUninstall() {
        return postPackageUninstall;
    }

    /**
     * Occurs after a package has been uninstalled, contains one or more Class::method callables or shell commands.
     * 
     * @param postPackageUninstall
     *     The post-package-uninstall
     */
    public void setPostPackageUninstall(List<Object> postPackageUninstall) {
        this.postPackageUninstall = postPackageUninstall;
    }

    /**
     * Occurs before the autoloader is dumped, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The preAutoloadDump
     */
    public List<Object> getPreAutoloadDump() {
        return preAutoloadDump;
    }

    /**
     * Occurs before the autoloader is dumped, contains one or more Class::method callables or shell commands.
     * 
     * @param preAutoloadDump
     *     The pre-autoload-dump
     */
    public void setPreAutoloadDump(List<Object> preAutoloadDump) {
        this.preAutoloadDump = preAutoloadDump;
    }

    /**
     * Occurs after the autoloader is dumped, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The postAutoloadDump
     */
    public List<Object> getPostAutoloadDump() {
        return postAutoloadDump;
    }

    /**
     * Occurs after the autoloader is dumped, contains one or more Class::method callables or shell commands.
     * 
     * @param postAutoloadDump
     *     The post-autoload-dump
     */
    public void setPostAutoloadDump(List<Object> postAutoloadDump) {
        this.postAutoloadDump = postAutoloadDump;
    }

    /**
     * Occurs after the root-package is installed, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The postRootPackageInstall
     */
    public List<Object> getPostRootPackageInstall() {
        return postRootPackageInstall;
    }

    /**
     * Occurs after the root-package is installed, contains one or more Class::method callables or shell commands.
     * 
     * @param postRootPackageInstall
     *     The post-root-package-install
     */
    public void setPostRootPackageInstall(List<Object> postRootPackageInstall) {
        this.postRootPackageInstall = postRootPackageInstall;
    }

    /**
     * Occurs after the create-project command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @return
     *     The postCreateProjectCmd
     */
    public List<Object> getPostCreateProjectCmd() {
        return postCreateProjectCmd;
    }

    /**
     * Occurs after the create-project command is executed, contains one or more Class::method callables or shell commands.
     * 
     * @param postCreateProjectCmd
     *     The post-create-project-cmd
     */
    public void setPostCreateProjectCmd(List<Object> postCreateProjectCmd) {
        this.postCreateProjectCmd = postCreateProjectCmd;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(preInstallCmd).append(postInstallCmd).append(preUpdateCmd).append(postUpdateCmd).append(preStatusCmd).append(postStatusCmd).append(prePackageInstall).append(postPackageInstall).append(prePackageUpdate).append(postPackageUpdate).append(prePackageUninstall).append(postPackageUninstall).append(preAutoloadDump).append(postAutoloadDump).append(postRootPackageInstall).append(postCreateProjectCmd).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Scripts) == false) {
            return false;
        }
        Scripts rhs = ((Scripts) other);
        return new EqualsBuilder().append(preInstallCmd, rhs.preInstallCmd).append(postInstallCmd, rhs.postInstallCmd).append(preUpdateCmd, rhs.preUpdateCmd).append(postUpdateCmd, rhs.postUpdateCmd).append(preStatusCmd, rhs.preStatusCmd).append(postStatusCmd, rhs.postStatusCmd).append(prePackageInstall, rhs.prePackageInstall).append(postPackageInstall, rhs.postPackageInstall).append(prePackageUpdate, rhs.prePackageUpdate).append(postPackageUpdate, rhs.postPackageUpdate).append(prePackageUninstall, rhs.prePackageUninstall).append(postPackageUninstall, rhs.postPackageUninstall).append(preAutoloadDump, rhs.preAutoloadDump).append(postAutoloadDump, rhs.postAutoloadDump).append(postRootPackageInstall, rhs.postRootPackageInstall).append(postCreateProjectCmd, rhs.postCreateProjectCmd).isEquals();
    }

}
