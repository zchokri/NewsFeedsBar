package czdev.newsfeedsbar;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import static czdev.newsfeedsbar.Constants.TAG_LOG;

public class ViewURL extends AppCompatActivity {
    private float x1,x2;
    static final int MIN_DISTANCE=150;


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
        myWebView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent ev) {
                        switch(ev.getAction())
                        {
                            case MotionEvent.ACTION_DOWN:
                                x1 = ev.getX();
                                break;
                            case MotionEvent.ACTION_UP:
                                x2 = ev.getX();
                                float deltaX = x2 - x1;
                                if (Math.abs(deltaX) > MIN_DISTANCE)
                                {
                                    //swiping right to left
                                    if(deltaX<0)
                                    {
                                        Log.d(TAG_LOG, "Swiping right to left ");
                                        // back_button: return to listView
                                        finish();
                                    }
                                }
                                break;
                        }
                        return ViewURL.super.onTouchEvent(ev);
                    }
                });

    }

}
