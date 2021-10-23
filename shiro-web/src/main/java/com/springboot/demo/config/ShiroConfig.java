package com.springboot.demo.config;

import com.springboot.demo.filter.UpmsAuthenticationFilter;
import com.springboot.demo.listener.UpmsSessionListener;
import com.springboot.demo.realm.CustomerRealm;
import com.springboot.demo.session.UpmsSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.filter.authc.AnonymousFilter;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.filter.DelegatingFilterProxy;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Configuration
public class ShiroConfig {


    @Autowired
    private BeanFactory beanFactory;

    //    创建自定义Realm
    @Bean
    public Realm upmsRealm() {
        CustomerRealm customerRealm = new CustomerRealm();
        return customerRealm;
    }


    @Bean
    public SimpleCookie sessionIdCookie() {
        SimpleCookie simpleCookie = new SimpleCookie();
        /** 不会暴露给客户端 */
        simpleCookie.setHttpOnly(true);
        /** 设置Cookie的过期时间，秒为单位，默认-1表示关闭浏览器时过期Cookie */
        simpleCookie.setMaxAge(-1);
        simpleCookie.setName("shiro_web_serssion_id");
        return simpleCookie;
    }

    @Bean
    @DependsOn({"sessionIdCookie", "sessionListener", "sessionFactory"})
    public DefaultWebSessionManager sessionManager(SimpleCookie sessionIdCookie,
                                                   UpmsSessionListener sessionListener, UpmsSessionFactory sessionFactory) {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setGlobalSessionTimeout(600000);
        sessionManager.setSessionIdCookieEnabled(true);
//        SimpleCookie sessionIdCookie = beanFactory.getBean("sessionIdCookie", SimpleCookie.class);
        sessionManager.setSessionIdCookie(sessionIdCookie);
        sessionManager.setSessionValidationSchedulerEnabled(false);
//        sessionManager.getSessionListeners().add(beanFactory.getBean(UpmsSessionListener.class));
        sessionManager.getSessionListeners().add(sessionListener);
//        sessionManager.setSessionFactory(beanFactory.getBean("sessionFactory", SessionFactory.class));
        sessionManager.setSessionFactory(sessionFactory);
        return sessionManager;
    }

    @Bean
    public SimpleCookie rememberMeCookie() {
        SimpleCookie simpleCookie = new SimpleCookie("rememberMe");
        simpleCookie.setHttpOnly(true);
        simpleCookie.setMaxAge(2592000);
        return simpleCookie;
    }

    @Bean
    @DependsOn("rememberMeCookie")
    public CookieRememberMeManager rememberMeManager(SimpleCookie rememberMeCookie) {
        CookieRememberMeManager rememberMeManager = new CookieRememberMeManager();
        rememberMeManager.setCipherKey(org.apache.shiro.codec.Base64.decode("4AvVhmFLUs0KTA3Kprsdag=="));
        rememberMeManager.setCookie(rememberMeCookie);
        return rememberMeManager;
    }

    @Bean
    @DependsOn({"upmsRealm", "sessionManager", "rememberMeManager"})
    public DefaultWebSecurityManager securityManager(DefaultWebSessionManager sessionManager, Realm upmsRealm,
                                                     CookieRememberMeManager rememberMeManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
//        securityManager.getRealms().add(beanFactory.getBean("upmsRealm", CustomerRealm.class));
        securityManager.setRealm(upmsRealm);
//        securityManager.setSessionManager(beanFactory.getBean("sessionManager", DefaultWebSessionManager.class));
        securityManager.setSessionManager(sessionManager);
//        securityManager.setRememberMeManager(beanFactory.getBean("rememberMeManager", CookieRememberMeManager.class));
        securityManager.setRememberMeManager(rememberMeManager);
        return securityManager;
    }

    //创建安全管理器
    @Bean
    public DefaultWebSecurityManager getDefaultWebSecurityManager(Realm realm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(realm);
        return securityManager;
    }

    @Bean
    @DependsOn("securityManager")
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(DefaultWebSecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
//        authorizationAttributeSourceAdvisor.setSecurityManager(beanFactory.getBean("securityManager", DefaultWebSecurityManager.class));
        authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
        return authorizationAttributeSourceAdvisor;
    }

    @Bean
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    @Bean
    @DependsOn("securityManager")
    public org.springframework.beans.factory.config.MethodInvokingFactoryBean methodInvokingFactoryBean(DefaultWebSecurityManager securityManager) {
        MethodInvokingFactoryBean methodInvokingFactoryBean = new MethodInvokingFactoryBean();
        methodInvokingFactoryBean.setStaticMethod("org.apache.shiro.SecurityUtils.setSecurityManager");
//        methodInvokingFactoryBean.setArguments(beanFactory.getBean("securityManager", DefaultWebSecurityManager.class));
        methodInvokingFactoryBean.setArguments(securityManager);
        return methodInvokingFactoryBean;
    }

    @Bean
    @DependsOn("lifecycleBeanPostProcessor")
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        return new DefaultAdvisorAutoProxyCreator();
    }

    @Value("${zheng.upms.sso.server.url}")
    private String loginUrl;

    @Value("${zheng.upms.successUrl}")
    private String successUrl;

    @Value("${zheng.upms.unauthorizedUrl}")
    private String nauthorizedUrl;

    //ShiroFilter过滤所有请求
    @Bean
    @DependsOn({"securityManager"})
    public ShiroFilterFactoryBean shiroFilter(DefaultWebSecurityManager securityManager,
                                              UpmsAuthenticationFilter upmsAuthenticationFilter) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        //给ShiroFilter配置安全管理器
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        shiroFilterFactoryBean.setLoginUrl(loginUrl);
        shiroFilterFactoryBean.setSuccessUrl(successUrl);
        shiroFilterFactoryBean.setUnauthorizedUrl(nauthorizedUrl);
//        shiroFilterFactoryBean.getFilters().put("authc", new UpmsAuthenticationFilter());
        shiroFilterFactoryBean.getFilters().put("authc", upmsAuthenticationFilter);
        Map<String, String> map = new LinkedHashMap<>();
        map.put("/user/logout", "anon");
        map.put("/user/login", "authc");
        map.put("/user/**", "authc");
        // 设置认证界面路径
        shiroFilterFactoryBean.setFilterChainDefinitionMap(map);
        log.info("======================shiroFilter().");
        return shiroFilterFactoryBean;
    }

    @Bean
    public FilterRegistrationBean registration() {

        DelegatingFilterProxy filterProxy = new DelegatingFilterProxy("shiroFilter");
        filterProxy.setTargetFilterLifecycle(true);
        FilterRegistrationBean reg = new FilterRegistrationBean();
        reg.setFilter(filterProxy);
        reg.addUrlPatterns("/*");
        return reg;
    }
}


