package httpproxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.impl.ProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class BlockingHttpsFilterSource extends HttpFiltersSourceAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(BlockingHttpsFilterSource.class);
    private List<String> whitelistedAccounts;

    public List<String> getWhitelistedAccounts() {
        return whitelistedAccounts;
    }

    public void setWhitelistedAccounts(List<String> whitelistedAccounts) {
        this.whitelistedAccounts = whitelistedAccounts;
    }

    public BlockingHttpsFilterSource(List<String> whitelistedAccounts) {
        this.whitelistedAccounts = whitelistedAccounts;
    }

    @Override
    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {

        //The connect request must bypass the filter! Otherwise, the handshake will fail
        if (ProxyUtils.isCONNECT(originalRequest)) {
            return new HttpFiltersAdapter(originalRequest);
        }

        return new HttpFiltersAdapter(originalRequest) {
            @Override
            public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                if (httpObject instanceof HttpRequest) {
                    HttpRequest request = (HttpRequest) httpObject;
                    LOG.info("Host: Method: URI: {} {} {}", request.headers().get("Host"), request.getClass(), request.getUri());
                    if (request.headers().get("Host").endsWith("console.aws.amazon.com") && request.headers().get("Cookie") != null) {
                        for (String s : Arrays.asList(request.headers().get("Cookie").split("\\s*;\\s*"))) {
                            if (s.startsWith("aws-userInfo=") && !whitelistedAccounts.contains(s.substring(52, 64))) {
                                LOG.info("Rejected Account Number: {}", s.substring(52, 64));
                                return getBadGatewayResponse();
                            }
                        }
                        return super.clientToProxyRequest(httpObject);
                    }

                }
                return super.clientToProxyRequest(httpObject);
            }

            private HttpResponse getBadGatewayResponse() {
                String body = "<!DOCTYPE HTML \"-//IETF//DTD HTML 2.0//EN\">\n"
                        + "<html><head>\n"
                        + "<title>" + "Bad Gateway" + "</title>\n"
                        + "</head><body>\n"
                        + "Access to this AWS Account is not authorized"
                        + "</body></html>\n";
                byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
                ByteBuf content = Unpooled.copiedBuffer(bytes);
                HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, content);
                response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bytes.length);
                response.headers().set("Content-Type", "text/html; charset=UTF-8");
                response.headers().set("Date", ProxyUtils.formatDate(new Date()));
                response.headers().set(HttpHeaders.Names.CONNECTION, "close");
                return response;
            }
        };
    }
}