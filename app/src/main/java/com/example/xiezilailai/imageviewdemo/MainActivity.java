package com.example.xiezilailai.imageviewdemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class MainActivity extends Activity implements View.OnTouchListener {



    private float lastX[] = {0, 0};//用来记录上一次两个触点的横坐标
    private float lastY[] = {0, 0};//用来记录上一次两个触点的纵坐标

    private float windowWidth, windowHeight;//当前窗口的宽和高
    private float imageHeight, imageWidth;//imageview中图片的宽高（注意：不是imageview的宽和高）

    private ImageView imageView;

    private static Matrix currentMatrix = new Matrix();//保存当前窗口显示的矩阵
    private Matrix touchMatrix,mmatrix;
    private boolean flag = false;//用来标记是否进行过移动前的首次点击
    private float moveLastX, moveLastY;//进行移动时用来记录上一个触点的坐标

    private static float max_scale = 4f;//缩放的最大值
    private static float min_scale = 0.8f;//缩放的最小值


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取窗口的宽高
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowWidth = windowManager.getDefaultDisplay().getWidth();
        windowHeight = windowManager.getDefaultDisplay().getHeight();
        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setOnTouchListener(this);

        center();
    }

    /*
    * 开始时将图片居中显示
    *
    *
    * */
    private void center() {

        //获取imageview中图片的实际高度
        Bitmap bitmap=((BitmapDrawable) (imageView).getDrawable()).getBitmap();
        imageHeight = bitmap.getHeight();
        imageWidth = bitmap.getWidth();

        //变换矩阵，使其移动到屏幕中央
        Matrix matrix = new Matrix();
        matrix.postTranslate(windowWidth / 2 - imageWidth / 2, windowHeight / 2 - imageHeight / 2);
        //保存到currentMatrix
        currentMatrix.set(matrix);
        imageView.setImageMatrix(matrix);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        //注意这一句的写法，用在多点触控中
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            // TODO: 2016/4/30  在这里解释一下，在程序中我们将单点控制移动和双点控制缩放区分开（但是双点也是可以
            // TODO: 2016/4/30  控制移动的)flag 的作用很简单，主要是用在单点移动时判断是否此次点击是否将要移动（不好描述，请读者自行细想一下）
            // todo，否则容易与双点操作混乱在一起，给用户带来较差的用户体验
            /*
            *
            *
            *
            * */
            case MotionEvent.ACTION_DOWN://第一个触点按下，将第一次的坐标保存下来
                lastX[0] = motionEvent.getX(0);
                lastY[0] = motionEvent.getY(0);
                moveLastX = motionEvent.getX();
                moveLastY = motionEvent.getY();
                flag = true;//第一次点击，说明有可能要进行单点移动，flag设为true
                break;
            case MotionEvent.ACTION_POINTER_DOWN://第二个触点按下，保存下来
                lastX[1] = motionEvent.getX(1);
                lastY[1] = motionEvent.getY(1);
                flag = false;//第二次点击，说明要进行双点操作,而不是单点移动，所以设为false
                break;
            case MotionEvent.ACTION_MOVE:
                //计算上一次触点间的距离
                float lastDistance = getDistance(lastX[0], lastY[0], lastX[1], lastY[1]);

                //如果有两个触点，进行放缩操作
                if (motionEvent.getPointerCount() == 2) {
                    //得到当前触点之间的距离
                    float currentDistance = getDistance(motionEvent.getX(0), motionEvent.getY(0), motionEvent.getX(1), motionEvent.getY(1));
                    touchMatrix = new Matrix();
                    //矩阵初始化为当前矩阵
                    touchMatrix.set(currentMatrix);

                    touchMatrix.preTranslate(-(currentDistance / lastDistance - 1) * imageWidth / 2,
                            -(currentDistance / lastDistance - 1) * imageHeight / 2);
                    float p[]=new float[9];
                    touchMatrix.getValues(p);
                    //根据判断当前缩放的大小来判断是否达到缩放边界
                    if(p[0]*currentDistance / lastDistance<min_scale||p[0]*currentDistance / lastDistance>max_scale){
                        //超过边界值时，设置为先前记录的矩阵
                        touchMatrix.set(mmatrix);
                        imageView.setImageMatrix(touchMatrix);
                    }else{
                        //图像缩放
                        touchMatrix.preScale(currentDistance / lastDistance, currentDistance / lastDistance);
                        //根据两个触点移动的距离实现位移（双触点平移）
                        float movex = (motionEvent.getX(0) - lastX[0] + motionEvent.getX(1) - lastX[1]) / 2;
                        float movey = (motionEvent.getY(0) - lastY[0] + motionEvent.getY(1) - lastY[1]) / 2;
                        touchMatrix.postTranslate(movex, movey);
                        //保存最后的矩阵，当缩放超过边界值时就设置为此矩阵
                        mmatrix=touchMatrix;
                        imageView.setImageMatrix(touchMatrix);
                    }

                } else {
                    if (flag) {
                        touchMatrix = new Matrix();
                        touchMatrix.set(currentMatrix);
                        //只有一个点时，进行位移
                        touchMatrix.postTranslate(-moveLastX + motionEvent.getX(0), -moveLastY + motionEvent.getY(0));
                        imageView.setImageMatrix(touchMatrix);
                    }

                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:

                //松开手时，保存当前矩阵，此时的位置保存下来
                //flag设为控制
                currentMatrix = touchMatrix;
                moveLastX = motionEvent.getX(0);
                moveLastY = motionEvent.getY(0);
                flag = false;

                break;
        }
        return true;
    }

    //得到两点之间的距离
    private float getDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }



}
