package com.uav.uavserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * 单 jar 部署时提供前端页面。
 *
 * <p>工作方式：
 * <ol>
 *   <li>静态资源从 {@code classpath:/static/} 读取。打包时由 Maven 把 {@code front/dist} 复制进来
 *       （见 pom.xml 中的 maven-resources-plugin 配置），因此最终 jar 里同时包含后端与前端；</li>
 *   <li>前端是 vue-router 的 history 模式，直接访问 {@code /airspace/violation} 这类地址时
 *       classpath 下并没有同名文件，这里统一回退到 {@code index.html}，交给前端路由处理；</li>
 *   <li>{@code /api/**} 交给后端控制器，不做回退，保证接口自身的 404 语义不变；</li>
 *   <li>开发时（未执行前端构建）{@code static/index.html} 不存在，回退直接返回 null，
 *       不会影响 {@code mvn spring-boot:run} + Vite 5173 的既有开发方式。</li>
 * </ol>
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // 接口路径不回退；带扩展名的静态资源找不到就正常 404
                        if (resourcePath.startsWith("api/") || resourcePath.contains(".")) {
                            return null;
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
