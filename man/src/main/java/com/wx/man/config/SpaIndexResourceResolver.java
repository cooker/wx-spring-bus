package com.wx.man.config;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolver;

import java.io.IOException;

/**
 * 对不存在于 static 下的路径返回 index.html，用于 Vue Router history 模式 SPA。
 * 仅对非 /api 的 GET 请求回退到 index.html。
 */
public class SpaIndexResourceResolver extends PathResourceResolver implements ResourceResolver {

    @Override
    protected Resource getResource(String resourcePath, Resource location) throws IOException {
        Resource resource = super.getResource(resourcePath, location);
        if (resource != null) {
            return resource;
        }
        return super.getResource("index.html", location);
    }
}
