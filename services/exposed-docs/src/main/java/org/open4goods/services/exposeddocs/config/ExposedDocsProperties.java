package org.open4goods.services.exposeddocs.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties controlling which embedded resources are exposed.
 * Defines the categories, classpath roots, and file extensions to scan.
 */
@ConfigurationProperties(prefix = "exposed-docs")
public class ExposedDocsProperties
{

    private boolean controllerEnabled = true;
    private boolean serviceEnabled = true;
    private List<Category> categories = new ArrayList<>();

    /**
     * Builds default categories for docs and prompts when none are provided.
     */
    public ExposedDocsProperties()
    {
        if (categories.isEmpty()) {
            categories.add(Category.defaultCategory(
                    "docs",
                    "Documentation",
                    "docs",
                    List.of("md", "yml", "yaml")));
            categories.add(Category.defaultCategory(
                    "prompts",
                    "Prompts",
                    "docs",
                    List.of("prompt")));
        }
    }

    /**
     * Indicates whether the REST controller is enabled.
     *
     * @return true when the controller is enabled
     */
    public boolean isControllerEnabled()
    {
        return controllerEnabled;
    }

    /**
     * Enables or disables the REST controller.
     *
     * @param controllerEnabled flag to enable the controller
     */
    public void setControllerEnabled(boolean controllerEnabled)
    {
        this.controllerEnabled = controllerEnabled;
    }

    /**
     * Indicates whether the indexing service is enabled.
     *
     * @return true when the service is enabled
     */
    public boolean isServiceEnabled()
    {
        return serviceEnabled;
    }

    /**
     * Enables or disables the indexing service.
     *
     * @param serviceEnabled flag to enable the service
     */
    public void setServiceEnabled(boolean serviceEnabled)
    {
        this.serviceEnabled = serviceEnabled;
    }

    /**
     * Returns configured resource categories.
     *
     * @return list of categories
     */
    public List<Category> getCategories()
    {
        return categories;
    }

    /**
     * Sets the configured resource categories.
     *
     * @param categories list of categories
     */
    public void setCategories(List<Category> categories)
    {
        this.categories = categories != null ? categories : new ArrayList<>();
    }

    /**
     * Defines a resource category exposed by the service.
     */
    public static class Category
    {

        private String id;
        private String label;
        private String classpathRoot;
        private List<String> extensions = new ArrayList<>();

        /**
         * Creates a category instance with common defaults.
         *
         * @param id category identifier
         * @param label display label
         * @param classpathRoot classpath root to scan
         * @param extensions file extensions to include
         * @return configured category
         */
        public static Category defaultCategory(String id, String label, String classpathRoot, List<String> extensions)
        {
            Category category = new Category();
            category.setId(id);
            category.setLabel(label);
            category.setClasspathRoot(classpathRoot);
            category.setExtensions(new ArrayList<>(extensions));
            return category;
        }

        /**
         * Returns the category identifier.
         *
         * @return category identifier
         */
        public String getId()
        {
            return id;
        }

        /**
         * Sets the category identifier.
         *
         * @param id identifier value
         */
        public void setId(String id)
        {
            this.id = id;
        }

        /**
         * Returns the display label.
         *
         * @return label
         */
        public String getLabel()
        {
            return label;
        }

        /**
         * Sets the display label.
         *
         * @param label label value
         */
        public void setLabel(String label)
        {
            this.label = label;
        }

        /**
         * Returns the classpath root.
         *
         * @return classpath root
         */
        public String getClasspathRoot()
        {
            return classpathRoot;
        }

        /**
         * Sets the classpath root to scan.
         *
         * @param classpathRoot classpath root
         */
        public void setClasspathRoot(String classpathRoot)
        {
            this.classpathRoot = classpathRoot;
        }

        /**
         * Returns configured file extensions.
         *
         * @return extensions list
         */
        public List<String> getExtensions()
        {
            return extensions;
        }

        /**
         * Sets the configured file extensions.
         *
         * @param extensions extensions list
         */
        public void setExtensions(List<String> extensions)
        {
            this.extensions = extensions != null ? extensions : new ArrayList<>();
        }
    }
}
