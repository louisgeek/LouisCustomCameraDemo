package com.louisgeek.louiscustomcamerademo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


public class CameraLine extends View {

    private Paint mLinePaint;
    private Paint mLineCrossPaint;


    final int LINE_STYLE_HIDE_LINE = 0;
    final int LINE_STYLE_SHOW_LINES = 1;
    final int LINE_STYLE_SHOW_LINES_LINE_IS_WIDE = 2;


    int nowLineStyle = LINE_STYLE_HIDE_LINE;//默认隐藏
    boolean showLines;


    // 窄 对称 网格线
    // 宽 井字 网格线
    boolean lineIsWide = false;

    float crossLineLength = 0;//默认

    public CameraLine(Context context) {
        super(context);
        Log.i("XXX", "louis=xx:CameraLine(Context context)");
        init();
    }

    public CameraLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i("XXX", "louis=xx:CameraLine(Context context, AttributeSet attrs)");
        getAttrsAndInit(context, attrs);

    }

    public CameraLine(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.i("XXX", "louis=xx:CameraLine(Context context, AttributeSet attrs, int defStyleAttr)");
        getAttrsAndInit(context, attrs);
    }

    private void getAttrsAndInit(Context context, AttributeSet attrs) {
        init();
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraLine_Attrs);
        boolean lineIsWide = typedArray.getBoolean(R.styleable.CameraLine_Attrs_lineIsWide, false);
        int lineColor = typedArray.getInt(R.styleable.CameraLine_Attrs_lineColor, 0);
        //float lineWidth = typedArray.getFloat(R.styleable.CameraLine_Attrs_lineWidth, 0);
        float lineWidth = typedArray.getDimension(R.styleable.CameraLine_Attrs_lineWidth, 0);

        float lineCrossLength = typedArray.getDimension(R.styleable.CameraLine_Attrs_lineCrossLength, 0);
        float lineCrossWidth = typedArray.getDimension(R.styleable.CameraLine_Attrs_lineCrossWidth, 0);

        int lineCrossColor = typedArray.getInt(R.styleable.CameraLine_Attrs_lineCrossColor, 0);
        boolean lineIsShow = typedArray.getBoolean(R.styleable.CameraLine_Attrs_lineIsShow, true);
        typedArray.recycle();
        Log.i("XXX", "louis=xx:lineColor:" + lineColor + "lineWidth:" + lineWidth);

        this.lineIsWide = lineIsWide;
        showLines = lineIsShow;
        // 覆盖
        if (lineColor != 0) {
            mLinePaint.setColor(lineColor);
        }
        if (lineWidth != 0) {
            mLinePaint.setStrokeWidth(lineWidth);
        }
        if (lineCrossLength != 0) {
            crossLineLength = Utils.dip2px(getContext(), lineCrossLength);
        }
        if (lineCrossWidth != 0) {
            mLineCrossPaint.setStrokeWidth(Utils.dip2px(getContext(), 1));
        }
        if (lineCrossColor != 0) {
            mLineCrossPaint.setColor(lineColor);
        }

    }

    public void changeLineStyle() {
        switch (nowLineStyle) {
            case LINE_STYLE_HIDE_LINE:
                nowLineStyle = LINE_STYLE_SHOW_LINES;
                lineIsWide = true;
                showLines = true;
                break;
            case LINE_STYLE_SHOW_LINES:
                nowLineStyle = LINE_STYLE_SHOW_LINES_LINE_IS_WIDE;
                lineIsWide = false;
                showLines = true;
                break;
            case LINE_STYLE_SHOW_LINES_LINE_IS_WIDE:
                nowLineStyle = LINE_STYLE_HIDE_LINE;
                showLines = false;
                break;
        }
        this.invalidate();
    }

    // 默认
    private void init() {
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setColor(Color.parseColor("#60E0E0E0"));//默认#45e0e0e0
        mLinePaint.setStrokeWidth(Utils.dip2px(getContext(), 1));//默认1dp


        mLineCrossPaint = new Paint();
        mLineCrossPaint.setColor(Color.parseColor("#55000000"));//00-FF
        mLineCrossPaint.setStrokeWidth(Utils.dip2px(getContext(), 1));//默认1dp

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (showLines) {
            int screenWidth = Utils.getScreenWH(getContext()).widthPixels;
            int screenHeight = Utils.getScreenWH(getContext()).heightPixels;

		/*
         * int width = screenWidth/3; int height = screenHeight/3;
		 * 
		 * for (int i = width, j = 0;i < screenWidth && j<2;i += width, j++) {
		 * canvas.drawLine(i, 0, i, screenHeight, mLinePaint); } for (int j =
		 * height,i = 0;j < screenHeight && i < 2;j += height,i++) {
		 * canvas.drawLine(0, j, screenWidth, j, mLinePaint); }
		 */

            if (lineIsWide) {
                int lineCount = 3;// 为了隐藏最中间的线，线数量基本是奇数数量
                int width = screenWidth / (lineCount + 1);
                int height = screenHeight / (lineCount + 1);
                int centerLineNum = lineCount / 2;
                for (int i = width, j = 0; i < screenWidth && j < lineCount; i += width, j++) {
                    if (centerLineNum != j) {
                        canvas.drawLine(i, 0, i, screenHeight, mLinePaint);
                    }

                }
                for (int j = height, i = 0; j < screenHeight && i < lineCount; j += height, i++) {
                    if (centerLineNum != i) {
                        canvas.drawLine(0, j, screenWidth, j, mLinePaint);
                    }
                }

            } else {
                int lineCount = 2;
                int width = screenWidth / (lineCount + 1);
                int height = screenHeight / (lineCount + 1);
                for (int i = width, j = 0; i < screenWidth && j < lineCount; i += width, j++) {
                    canvas.drawLine(i, 0, i, screenHeight, mLinePaint);

                }
                for (int j = height, i = 0; j < screenHeight && i < lineCount; j += height, i++) {
                    canvas.drawLine(0, j, screenWidth, j, mLinePaint);
                }
            }


            if (crossLineLength != 0) {
                float centerX = canvas.getWidth() / 2;
                float centerY = canvas.getHeight() / 2;

                canvas.drawLine(centerX - crossLineLength, centerY, centerX + crossLineLength, centerY, mLineCrossPaint);
                canvas.drawLine(centerX, centerY - crossLineLength, centerX, centerY + crossLineLength, mLineCrossPaint);
            }
        }
    }


}
