package org.open4goods.nudgerfrontapi.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Configuration properties describing the eco-nudger team roster.
 */
@Component
@ConfigurationProperties(prefix = "team-config")
public class TeamProperties {

    @Schema(description = "Core eco-nudger team members.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Member> cores = new ArrayList<>();

    @Schema(description = "Extended contributors supporting the core team.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Member> contributors = new ArrayList<>();

    public List<Member> getCores() {
        return cores;
    }

    public void setCores(List<Member> cores) {
        this.cores = cores == null ? new ArrayList<>() : new ArrayList<>(cores);
    }

    public List<Member> getContributors() {
        return contributors;
    }

    public void setContributors(List<Member> contributors) {
        this.contributors = contributors == null ? new ArrayList<>() : new ArrayList<>(contributors);
    }

    /**
     * Individual team member entry.
     */
    @Schema(description = "Team member profile entry.")
    public static class Member {

        @Schema(description = "Full name of the team member.", example = "Goulven Furet")
        private String name;

        @Schema(description = "LinkedIn profile URL of the member.",
                example = "https://www.linkedin.com/in/example/")
        private String linkedInUrl;

        @Schema(description = "Relative URL for the member portrait.",
                example = "/assets/img/team/Goulven.jpeg")
        private String imageUrl;

        @Schema(description = "XWiki bloc identifier providing the member biography.",
                example = "pages:team:goulven-furet:")
        private String blocId;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBlocId() {
            return blocId;
        }

        public void setBlocId(String blocId) {
            this.blocId = blocId;
        }

        public String getLinkedInUrl() {
            return linkedInUrl;
        }

        public void setLinkedInUrl(String linkedInUrl) {
            this.linkedInUrl = linkedInUrl;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }
}
