package proxyhighlighter;

import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.message.MimeType;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.proxy.http.InterceptedResponse;
import burp.api.montoya.proxy.http.ProxyResponseHandler;
import burp.api.montoya.proxy.http.ProxyResponseReceivedAction;
import burp.api.montoya.proxy.http.ProxyResponseToBeSentAction;

import java.util.List;
import java.util.regex.Pattern;

public class MyProxyHttpResponseHandler implements ProxyResponseHandler {
    private static final Pattern versonParamPattern = Pattern.compile("^(v|ver|version|_)$", Pattern.CASE_INSENSITIVE);

    @Override
    public ProxyResponseReceivedAction handleResponseReceived(InterceptedResponse interceptedResponse) {
        Annotations annotations;
        try {
            annotations = interceptedResponse.annotations();
        } catch (Exception e) {
            return ProxyResponseReceivedAction.continueWith(interceptedResponse);
        }
        if (!annotations.highlightColor().equals(HighlightColor.NONE)) {
            return ProxyResponseReceivedAction.continueWith(interceptedResponse);
        }

        List<ParsedHttpParameter> parameters = interceptedResponse.initiatingRequest().parameters();
        // filter parameters type URL
        if (!parameters.isEmpty() && parameters.size() > 1) {
            parameters.removeIf(param -> param.type() != HttpParameterType.URL);
        }

        boolean notHaveParameters = parameters.isEmpty();
        if (!notHaveParameters && parameters.size() == 1) {
            ParsedHttpParameter param = parameters.get(0);
            if (versonParamPattern.matcher(param.name()).matches()) {
                notHaveParameters = true;
            }
            if (param.value() == null && param.name().equals("")) { // surveys.svg?6a59d951e3
                notHaveParameters = true;
            }
            if (isNumeric(param.value())) {
                notHaveParameters = true;
            }
        }

        MimeType parsedMimetype = interceptedResponse.inferredMimeType();

        if (parsedMimetype == MimeType.NONE || parsedMimetype == MimeType.UNRECOGNIZED) {
            parsedMimetype = interceptedResponse.statedMimeType();
        }
        String responseType = getResponseType(parsedMimetype);

        String pathExt = getExtension(interceptedResponse.initiatingRequest().path());

        // spam files
        if (responseType.equals("css") || // index.css
                ((parsedMimetype == MimeType.UNRECOGNIZED || parsedMimetype == MimeType.PLAIN_TEXT) && pathExt.matches("\\.(woff2|woff|ttf|otf|eot|m3u8)$")) || // font.woff2
                (responseType.equals("image") && pathExt.endsWith(".ico")) // favicon.ico (IMAGE_UNKNOWN)
        ) {
            annotations = annotations.withHighlightColor(HighlightColor.GRAY);
            return ProxyResponseReceivedAction.continueWith(interceptedResponse, annotations);
        }

        // Load image from external source?
        if (notHaveParameters) {
            if (responseType.equals("image") || responseType.equals("sound") || responseType.equals("video")) {
                annotations = annotations.withHighlightColor(HighlightColor.GRAY);
                return ProxyResponseReceivedAction.continueWith(interceptedResponse, annotations);
            }
        }

        if ((responseType.equals("script") && notHaveParameters) || // JSONP case
                (responseType.equals("json") && (pathExt.endsWith(".map") || pathExt.endsWith(".js"))) || // .map.js, .bundle.js
                (interceptedResponse.statusCode() == 304 && pathExt.endsWith(".js"))) {
            annotations = annotations.withHighlightColor(HighlightColor.PINK);
            return ProxyResponseReceivedAction.continueWith(interceptedResponse, annotations);

        }

        // CDN cache this static files
        if (interceptedResponse.statusCode() == 304) {
            if (pathExt.matches("\\.(gif|png|jpeg|jpg|pjpeg|ico|css|mp3|mp4|svg)$")) {
                annotations = annotations.withHighlightColor(HighlightColor.GRAY);
                return ProxyResponseReceivedAction.continueWith(interceptedResponse, annotations);
            }
        }

        return ProxyResponseReceivedAction.continueWith(interceptedResponse);
    }

    @Override
    public ProxyResponseToBeSentAction handleResponseToBeSent(InterceptedResponse interceptedResponse) {
        return ProxyResponseToBeSentAction.continueWith(interceptedResponse);
    }

    static String getExtension(String urlPath) {
        int query_start = urlPath.indexOf('?');
        if (query_start == -1) {
            query_start = urlPath.length();
        }
        urlPath = urlPath.substring(0, query_start);
        int last_dot = urlPath.lastIndexOf('.');
        if (last_dot == -1) {
            return "";
        } else {
            return urlPath.substring(last_dot).toLowerCase();
        }
    }

    static String getResponseType(MimeType mimeType) {
        String mimeTypeString = mimeType.toString().toLowerCase();
        if (mimeTypeString.contains("image")) {
            return "image";
        } else {
            return mimeTypeString;
        }
    }

    static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
