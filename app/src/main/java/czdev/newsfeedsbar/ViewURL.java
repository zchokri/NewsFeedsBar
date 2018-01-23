package czdev.newsfeedsbar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import static czdev.newsfeedsbar.Constants.TAG_LOG;

public class ViewURL extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String link = getIntent().getSerializableExtra("link").toString();
        Log.d(TAG_LOG, "link address =  " + link);
        setContentView(R.layout.activity_urlview);
        getWindow().setWindowAnimations(R.style.WindowAnimationTransition);

        WebView myWebView = (WebView) findViewById(R.id.webview);
        //Obtain the WebSettings object and Enable JS
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.loadUrl(link);
        myWebView.setWebViewClient(new WebViewClient(){
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return super.shouldOverrideUrlLoading(view, request);
            }
        });
    }

}
