package org.icgc.dcc.portal.spring;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProvider;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;

@Slf4j
public class SpringComponentProviderFactory implements IoCComponentProviderFactory {

  @NonNull
  private final GenericApplicationContext context;
  private final SpringBeanNameResolver resolver;

  public SpringComponentProviderFactory(ResourceConfig config, GenericApplicationContext context) {
    this.context = context;
    this.resolver = new SpringBeanNameResolver(context);

    config.getSingletons().add(new SpringContextInjectableProvider(context));
  }

  @Override
  public IoCComponentProvider getComponentProvider(Class<?> type) {
    return getComponentProvider(null, type);
  }

  @Override
  public IoCComponentProvider getComponentProvider(ComponentContext componentContext, Class<?> type) {
    val beanName = resolver.resolveBeanName(componentContext, type);
    if (beanName == null) {
      return null;
    }

    log.info("Creating provider: {}", type);
    val definition = context.getBeanDefinition(beanName);
    val scope = getComponentScope(definition);

    val provider = new SpringManagedComponentProvider(context, scope, beanName, type);

    return provider;
  }

  private static ComponentScope getComponentScope(BeanDefinition definition) {
    val scope = definition.getScope();

    if (scope.equals(SCOPE_SINGLETON)) {
      return ComponentScope.Singleton;
    } else if (scope.equals(SCOPE_PROTOTYPE)) {
      return ComponentScope.PerRequest;
    } else if (scope.equals("request")) {
      return ComponentScope.PerRequest;
    } else {
      return ComponentScope.Undefined;
    }
  }

}
