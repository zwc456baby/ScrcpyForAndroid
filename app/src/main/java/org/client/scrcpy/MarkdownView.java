package org.client.scrcpy;

import android.content.Context;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;


import org.client.scrcpy.utils.ThreadUtils;
import org.lsposed.lsparanoid.Obfuscate;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import io.noties.markwon.simple.ext.SimpleExtPlugin;

@Obfuscate
public class MarkdownView extends TextView {

    private Markwon markwon;

    private boolean isInit = false;

    private String lastStr = null;

    public MarkdownView(Context context) {
        super(context);
        init(context);
    }

    public MarkdownView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MarkdownView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        if (isInit) {
            return;
        }
        isInit = true;
        // 异步初始化，方式画面卡顿
        ThreadUtils.execute(() -> {
            try {
                markwon = Markwon.builder(context)
                        .usePlugin(ImagesPlugin.create())
                        .usePlugin(HtmlPlugin.create())
                        .usePlugin(SimpleExtPlugin.create())
                        .usePlugin(LinkifyPlugin.create())
                        .usePlugin(MarkwonInlineParserPlugin.create())
                        .usePlugin(TablePlugin.create(context))
                        .usePlugin(JLatexMathPlugin.create(12))
//                .usePlugin(TableEntryPlugin.create(this))
//                .usePlugin(SyntaxHighlightPlugin.create(Prism4j(GrammarLocatorDef()), Prism4jThemeDefault.create(0)))
                        .usePlugin(TaskListPlugin.create(context))
                        .usePlugin(StrikethroughPlugin.create())
//                .usePlugin(ReadMeImageDestinationPlugin(intent.data))
                        .build();
                if (!TextUtils.isEmpty(lastStr)) {
                    try {
                        Spanned spanned = markwon.toMarkdown(lastStr);
                        MarkdownView.this.post(() -> {
                            markwon.setParsedMarkdown(MarkdownView.this, spanned);
                        });
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignore) {
            }
        });
    }


    public void setMarkwon(String str) {
        lastStr = str;
        if (!isInit || markwon == null) {
            MarkdownView.this.post(() -> {
                MarkdownView.this.setText(str);
            });
            return;
        }
        // 由于大文本可能导致解析问题，故而子线程中先解析好，再post 到主线程
        ThreadUtils.execute(() -> {
            try {
                Spanned spanned = markwon.toMarkdown(str);
                MarkdownView.this.post(() -> {
                    markwon.setParsedMarkdown(MarkdownView.this, spanned);
                });
            } catch (Exception e) {
                MarkdownView.this.post(() -> {
                    MarkdownView.this.setText(str);
                });
            }
        });
    }
}
