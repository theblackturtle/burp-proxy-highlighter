package proxyhighlighter;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

public class ProxyHighlighter implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        api.proxy().registerRequestHandler(new MyProxyHttpRequestHandler());
        api.proxy().registerResponseHandler(new MyProxyHttpResponseHandler());
        api.logging().logToOutput("ProxyHighlighter loaded!");
        api.logging().logToOutput("Author: @theblackturtle");
    }
}