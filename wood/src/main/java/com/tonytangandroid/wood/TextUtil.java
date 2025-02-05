package com.tonytangandroid.wood;


import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.PrecomputedText;
import android.widget.TextView;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;


class TextUtil {
    /**
     * Pref Matters
     * <p>
     * PrecomputedText is not yet in support library, But still this is left
     * because the callable which is formatting Json, Xml will now be done in background thread
     */
    public static void asyncSetText(Executor bgExecutor, final AsyncTextProvider asyncTextProvider) {
        final Reference<AsyncTextProvider> asyncTextProviderReference = new WeakReference<>(asyncTextProvider);

        bgExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    AsyncTextProvider asyncTextProvider = asyncTextProviderReference.get();
                    if (asyncTextProvider == null) return;
                    // get text from background
                    CharSequence longString = asyncTextProvider.getText();
                    // pre-compute Text before setting on text view. so UI thread can be free from calculating text paint
                    CharSequence updateText;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        PrecomputedText.Params params = asyncTextProvider.getTextView().getTextMetricsParams();
                        updateText = PrecomputedText.create(longString, params);
                    } else {
                        updateText = longString;
                    }
                    final CharSequence updateTextFinal = updateText;

                    asyncTextProvider.getTextView().post(new Runnable() {
                        @Override
                        public void run() {
                            AsyncTextProvider asyncTextProviderInternal = asyncTextProviderReference.get();
                            if (asyncTextProviderInternal == null) return;
                            // set pre computed text
                            TextView textView = asyncTextProviderInternal.getTextView();
                            textView.setText(updateTextFinal, TextView.BufferType.SPANNABLE);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static boolean isNullOrWhiteSpace(CharSequence text) {
        return text == null || text.length() == 0 || text.toString().trim().length() == 0;
    }

    public interface AsyncTextProvider {
        CharSequence getText();

        AppCompatTextView getTextView();
    }

    public static void share(Context context, CharSequence content) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, content);
        sendIntent.setType("text/plain");
        context.startActivity(Intent.createChooser(sendIntent, null));
    }
}
