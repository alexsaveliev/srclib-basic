
package com.sourcegraph.toolchain.php.composer.schema;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Generated;
import java.util.HashMap;


/**
 * This is a hash of package name (keys) and version constraints (values) that this package requires for developing it (testing tools and such).
 * 
 */
@Generated("org.jsonschema2pojo")
public class RequireDev extends HashMap<String, String> {


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
