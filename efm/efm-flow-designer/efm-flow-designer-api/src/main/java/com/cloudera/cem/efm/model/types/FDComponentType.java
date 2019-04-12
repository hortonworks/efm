/**
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 * <p>
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 * <p>
 * If this code is provided to you under the terms of the AGPLv3:
 * (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 * (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 * LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 * (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 * FROM OR RELATED TO THE CODE; AND
 * (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 * TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 * UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.model.types;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@ApiModel
public class FDComponentType implements Comparable<FDComponentType> {

    private final String agentManifestId;

    private final String group;
    private final String artifact;
    private final String version;
    private final String type;

    private final String description;
    private final Set<String> tags;

    private FDComponentType() {
        this.agentManifestId = null;
        this.group = null;
        this.artifact = null;
        this.version = null;
        this.type = null;
        this.description = null;
        this.tags = null;
    }

    private FDComponentType(final Builder builder) {
        this.agentManifestId = builder.agentManifestId;
        this.group = builder.group;
        this.artifact = builder.artifact;
        this.version = builder.version;
        this.type = builder.type;
        this.description = builder.description;
        this.tags = Collections.unmodifiableSet(
                builder.tags == null ? Collections.emptySet() : new HashSet<>(builder.tags));

        Validate.notBlank(this.agentManifestId);
        Validate.notBlank(this.group);
        Validate.notBlank(this.artifact);
        Validate.notBlank(this.version);
        Validate.notBlank(this.type);
    }

    @ApiModelProperty("The id of the agent manifest this component belongs to")
    public String getAgentManifestId() {
        return agentManifestId;
    }

    @ApiModelProperty("The group name of the bundle that provides the referenced type.")
    public String getGroup() {
        return group;
    }

    @ApiModelProperty("The artifact name of the bundle that provides the referenced type.")
    public String getArtifact() {
        return artifact;
    }

    @ApiModelProperty("The version of the bundle that provides the referenced type.")
    public String getVersion() {
        return version;
    }

    @ApiModelProperty(
            value = "The fully-qualified class type",
            required = true,
            notes = "For example, 'org.apache.nifi.GetFile' or 'org::apache:nifi::minifi::GetFile'")
    public String getType() {
        return type;
    }

    @ApiModelProperty("The description for this component")
    public String getDescription() {
        return description;
    }

    @ApiModelProperty("The tags for this component")
    public Set<String> getTags() {
        return tags;
    }

    @Override
    public int compareTo(FDComponentType o) {
        if (o == null) {
            return 1;
        }

        return type.compareTo(o.type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        FDComponentType that = (FDComponentType) o;

        return new EqualsBuilder()
                .append(group, that.group)
                .append(artifact, that.artifact)
                .append(version, that.version)
                .append(type, that.type)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(group)
                .append(artifact)
                .append(version)
                .append(type)
                .toHashCode();
    }

    public static class Builder {

        private String agentManifestId;

        private String group;
        private String artifact;
        private String version;
        private String type;

        private String description;
        private Set<String> tags = new LinkedHashSet<>();

        public Builder agentManifestId(final String agentManifestId) {
            this.agentManifestId = agentManifestId;
            return this;
        }

        public Builder group(final String group) {
            this.group = group;
            return this;
        }

        public Builder artifact(final String artifact) {
            this.artifact = artifact;
            return this;
        }

        public Builder version(final String version) {
            this.version = version;
            return this;
        }

        public Builder type(final String type) {
            this.type = type;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder tags(final Set<String> tags) {
            if (tags != null) {
                tags.clear();
                tags.addAll(tags);
            }
            return this;
        }

        public FDComponentType build() {
            return new FDComponentType(this);
        }
    }
}
