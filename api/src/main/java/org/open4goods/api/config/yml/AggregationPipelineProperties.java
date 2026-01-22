package org.open4goods.api.config.yml;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties describing aggregation pipeline ordering.
 *
 * <p>The lists are service identifiers used by {@code AggregationFacadeService}
 * to assemble realtime, sanitisation, classification, and scoring pipelines.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "open4goods.aggregation")
public class AggregationPipelineProperties
{

    private Pipelines pipelines = new Pipelines();

    public Pipelines getPipelines()
    {
        return pipelines;
    }

    public void setPipelines(Pipelines pipelines)
    {
        this.pipelines = pipelines;
    }

    public static class Pipelines
    {

        private List<String> realtime = new ArrayList<>(List.of(
                "identity",
                "taxonomy",
                "attributes",
                "names",
                "price",
                "media"));

        private List<String> sanitisation = new ArrayList<>(List.of(
                "identity",
                "taxonomy",
                "attributes",
                "names",
                "price",
                "media"));

        private List<String> classification = new ArrayList<>(List.of(
                "identity",
                "taxonomy"));

        private List<String> scoring = new ArrayList<>(List.of(
                "clean-score",
                "attribute-score",
                "sustainalytics",
                "data-quality",
                "eco-score",
                "participating"));

        public List<String> getRealtime()
        {
            return realtime;
        }

        public void setRealtime(List<String> realtime)
        {
            this.realtime = realtime;
        }

        public List<String> getSanitisation()
        {
            return sanitisation;
        }

        public void setSanitisation(List<String> sanitisation)
        {
            this.sanitisation = sanitisation;
        }

        public List<String> getClassification()
        {
            return classification;
        }

        public void setClassification(List<String> classification)
        {
            this.classification = classification;
        }

        public List<String> getScoring()
        {
            return scoring;
        }

        public void setScoring(List<String> scoring)
        {
            this.scoring = scoring;
        }
    }
}
