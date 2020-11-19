package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@EnableConfigurationProperties(JwtProperties.class)
//自定义网关过滤器进行身份认证
@Component     //1.创建类继承AbstractGatewayFilterFactory   // 3. 添加泛型KeyValueConfig（内部类）
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.KeyValueConfig> {

    @Autowired
    private JwtProperties jwtProperties;

    // 4.重写构造方法
    public AuthGatewayFilterFactory(){
        super(KeyValueConfig.class);
    }

    // 5.重写 shortcutFieldOrder 方法，指定接收参数的顺序
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("paths");
    }

    @Override   //指定接收参数的类型
    public ShortcutType shortcutType(){
        return ShortcutType.GATHER_LIST;
    }

    @Override
    public GatewayFilter apply(KeyValueConfig config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

                System.out.println("自定义局部过滤器，只拦截特定路由对应的服务请求");
                System.err.println("接收参数：" + config.getPaths());

                //注意类型（非HttpServletRequest）
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();

                //身份认证过滤流程
                //1.判断当前请求路径是否在拦截名单中
                String curPath = request.getURI().getPath();//获取当前请求路径
                List<String> paths = config.getPaths();
                    //判断拦截名单中是否包含当前请求的路径
                if(!paths.stream().anyMatch(path -> curPath.startsWith(path) )) {
                    return chain.filter(exchange);
                }

                //2.获取请求的token信息(异步请求->请求头中获取  同步请求->cookie中获取)
                String token = request.getHeaders().getFirst("token");
                if(StringUtils.isBlank(token)){
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                    //如果cookies不为空，且包含特定名称的cookie
                    if(!CollectionUtils.isEmpty(cookies) && cookies.containsKey(jwtProperties.getCookieName())){
                        HttpCookie cookie = cookies.getFirst(jwtProperties.getCookieName());
                        token = cookie.getValue();
                    }
                }

                //3.判断token是否为空，是->重定向到登录页面
                if(StringUtils.isBlank(token)){
                    response.setStatusCode(HttpStatus.SEE_OTHER);//设置响应状态码
                    //设置响应路径
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    return response.setComplete();//拦截后续业务逻辑
                }

                try {
                //4.解析token,出现异常重定向到登录页面
                    Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPubKey());

                //5.拿到token载荷中的ip，和请求中ip进行比较
                    String tokenIp = map.get("ip").toString();
                    String requestIp = IpUtils.getIpAddressAtGateway(request);
                    //如果ip不一致
                    if(!StringUtils.equals(tokenIp, requestIp)){
                        response.setStatusCode(HttpStatus.SEE_OTHER);//设置响应状态码
                        //设置响应路径
                        response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                        return response.setComplete();//拦截后续业务逻辑
                    }

                //6.把解析到的用户登录信息传递给后续服务。
                    //构建request头信息
                    request.mutate().header("userId", map.get("userId").toString()).build();
                    exchange.mutate().request(request).build();

                } catch (Exception e) {
                    e.printStackTrace();
                    //出现异常，重定向到登录页面
                    response.setStatusCode(HttpStatus.SEE_OTHER);//设置响应状态码
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    return response.setComplete();//拦截后续业务逻辑
                }
                //放行
                return chain.filter(exchange);
            }
        };
    }

    @Data //2.定义接收配置文件相关路由信息的内部类
    public static class KeyValueConfig{
        private List<String> paths;//拦截路径的集合
    }
}
