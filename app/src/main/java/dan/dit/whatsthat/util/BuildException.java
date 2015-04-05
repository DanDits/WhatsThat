package dan.dit.whatsthat.util;

/**
 * An Exception that can be thrown by builder classes that lack information and are unable to build
 * a valid instance. Usually all required information should be requested in the constructor of the
 * Builder.
 * Created by daniel on 28.03.15.
 */
public class BuildException extends Exception {
    private String mBuilderName;
    private String mMissingDataHint;

    public BuildException(String msg) {
        super(msg);
    }

    public BuildException() {
    }

    /**
     * Sets the missing data that caused this builder to fail building the object instance.
     * @param builderName The name of the builder. Example: "class House"
     * @param missingDataHint The hint to what kind of data was missing or wrong. Example : "missing roof, no door".
     * @return This exception.
     */
    public BuildException setMissingData(String builderName, String missingDataHint) {
        mBuilderName = builderName;
        mMissingDataHint = missingDataHint;
        return this;
    }

    @Override
    public String toString() {
        return super.getMessage() + " Builder for " + mBuilderName + " failed: " + mMissingDataHint;
    }
}
