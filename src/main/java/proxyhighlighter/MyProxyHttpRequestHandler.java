package proxyhighlighter;

import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;

import java.util.List;

public class MyProxyHttpRequestHandler implements ProxyRequestHandler {
    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
        Annotations annotations;
        try {
            annotations = interceptedRequest.annotations();
        } catch (Exception e) {
            return ProxyRequestReceivedAction.continueWith(interceptedRequest);
        }

        if (!annotations.highlightColor().equals(HighlightColor.NONE)) {
            return ProxyRequestReceivedAction.continueWith(interceptedRequest);
        }

        List<HttpHeader> headers = interceptedRequest.headers();
        HttpHeader header = headers.stream().filter(h -> h.name().equalsIgnoreCase("Sec-Fetch-Mode")).findFirst().orElse(null);
        String secFetchMode = header != null ? header.value() : "";
        if (secFetchMode.equals("navigate")) {
            return ProxyRequestReceivedAction.continueWith(interceptedRequest, interceptedRequest.annotations().withHighlightColor(HighlightColor.BLUE));
        }

        if (interceptedRequest.method().equals("OPTIONS")) {
            return ProxyRequestReceivedAction.continueWith(interceptedRequest, interceptedRequest.annotations().withHighlightColor(HighlightColor.ORANGE));
        } else if (interceptedRequest.method().equals("GET")) {
        } else {
            return ProxyRequestReceivedAction.continueWith(interceptedRequest, interceptedRequest.annotations().withHighlightColor(HighlightColor.YELLOW));
        }
        return ProxyRequestReceivedAction.continueWith(interceptedRequest);
    }

    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest) {
        return ProxyRequestToBeSentAction.continueWith(interceptedRequest);
    }
}
