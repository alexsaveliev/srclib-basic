package com.sourcegraph.toolchain.core.objects;

/**
 * Contains information where we can retrieve definition source code
 */
class ResolvedTarget {
    /**
     * Repository SCM URI
     */
    String ToRepoCloneURL;
    /**
     * Source unit name
     */
    String ToUnit;
    /**
     * Source unit type
     */
    String ToUnitType;
    /**
     * Version
     */
    String ToVersionString;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResolvedTarget that = (ResolvedTarget) o;

        if (ToRepoCloneURL != null ? !ToRepoCloneURL.equals(that.ToRepoCloneURL) : that.ToRepoCloneURL != null)
            return false;
        if (ToUnit != null ? !ToUnit.equals(that.ToUnit) : that.ToUnit != null) return false;
        if (ToUnitType != null ? !ToUnitType.equals(that.ToUnitType) : that.ToUnitType != null) return false;
        if (ToVersionString != null ? !ToVersionString.equals(that.ToVersionString) : that.ToVersionString != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ToRepoCloneURL != null ? ToRepoCloneURL.hashCode() : 0;
        result = 31 * result + (ToUnit != null ? ToUnit.hashCode() : 0);
        result = 31 * result + (ToUnitType != null ? ToUnitType.hashCode() : 0);
        result = 31 * result + (ToVersionString != null ? ToVersionString.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ResolvedTarget{" +
                "ToRepoCloneURL='" + ToRepoCloneURL + '\'' +
                ", ToUnit='" + ToUnit + '\'' +
                ", ToUnitType='" + ToUnitType + '\'' +
                ", ToVersionString='" + ToVersionString + '\'' +
                '}';
    }
}
