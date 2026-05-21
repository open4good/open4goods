package org.open4goods.api.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Grants static access to the Spring {@link ApplicationContext}.
 * <p>
 * Used by classes that are instantiated via reflection (e.g. attribute parsers)
 * and need to obtain Spring-managed beans without constructor injection.
 */
@Component
public class SpringContextHolder implements ApplicationContextAware
{
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
    {
        SpringContextHolder.context = applicationContext;
    }

    /**
     * Retrieves a bean from the application context.
     *
     * @param <T>       bean type
     * @param beanClass the class of the bean to retrieve
     * @return the Spring-managed bean instance
     * @throws IllegalStateException if the application context is not yet initialized
     */
    public static <T> T getBean(Class<T> beanClass)
    {
        if (context == null)
        {
            throw new IllegalStateException("Spring ApplicationContext not initialized yet");
        }
        return context.getBean(beanClass);
    }
}
