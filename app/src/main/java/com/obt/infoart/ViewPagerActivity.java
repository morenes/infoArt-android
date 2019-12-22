package com.obt.infoart;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import java.io.File;

import uk.co.senab.photoview.PhotoView;

public class ViewPagerActivity extends Activity{
    private static final String ISLOCKED_ARG = "isLocked";
    private ViewPager mViewPager;
    private int width;
    private int height;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pager);
        mViewPager = (HackyViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(new SamplePagerAdapter());
        if (savedInstanceState != null) {
            boolean isLocked = savedInstanceState.getBoolean(ISLOCKED_ARG, false);
            ((HackyViewPager) mViewPager).setLocked(isLocked);
        }

    }

    private class SamplePagerAdapter extends PagerAdapter implements ILocationListener {
        private Res res = Res.getInstance();
        private Bitmap bitmap;
        private PhotoView photoView;
        @Override
        public int getCount() {
            return 1;
            /*
            int indice = 1;
            boolean existe;
            File archivo = null;
            do {
                archivo = new File(
                        res.obtenerDir() + "/Museum/"
                                + res.getNombre() + "/MAPA" + indice + Res.EX_IMAGEN);
                existe = res.existeFichero(archivo);
                indice++;
            } while (existe);
            return indice - 2;
            */
        }


        @Override
        public View instantiateItem(ViewGroup container, int position) {
            UDP.getInstance().setLocationListener(this);
            photoView = new PhotoView(container.getContext());
            File archivo = new File(
                    res.obtenerDir() + "/Museum/"
                            + res.getNombre() + "/MAPA" + (int) (position + 1) + Res.EX_IMAGEN);
            //photoView.setImageDrawable(Drawable.createFromPath(archivo.getAbsolutePath()));
            Paint mPaint = new Paint();
            mPaint.setColor(Color.BLUE);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeWidth(5);
            bitmap = BitmapFactory.decodeFile(archivo.getAbsolutePath());
            width=bitmap.getWidth();
            height=bitmap.getHeight();
            Bitmap b= Bitmap.createBitmap(bitmap, 0, 0,width-1,height-1);
            BitmapDrawable bd = new BitmapDrawable(App.context.getResources(), b);
            photoView.setImageDrawable(bd);
            container.addView(photoView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            return photoView;
        }

        @Override
        public void update(final int x,final int y) {
            System.out.println("Update");
            if (x==0&&y==0) return;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //container.removeView(lastView);
                    Paint mPaint = new Paint();
                    mPaint.setColor(Color.BLUE);
                    mPaint.setStyle(Paint.Style.FILL);
                    mPaint.setStrokeWidth(5);

                    Bitmap b= Bitmap.createBitmap(bitmap, 0, 0,width-1,height-1);
                    Canvas canvas = new Canvas(b);
                    canvas.drawCircle(x,y,3,mPaint);
                    BitmapDrawable bd = new BitmapDrawable(App.context.getResources(), b);
                    photoView.setImageDraw(bd);
                }
            });
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    private void toggleViewPagerScrolling() {
        if (isViewPagerActive()) {
            ((HackyViewPager) mViewPager).toggleLock();
        }
    }

    private boolean isViewPagerActive() {
        return (mViewPager != null && mViewPager instanceof HackyViewPager);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (isViewPagerActive()) {
            outState.putBoolean(ISLOCKED_ARG, ((HackyViewPager) mViewPager).isLocked());
        }
        super.onSaveInstanceState(outState);
    }

}
