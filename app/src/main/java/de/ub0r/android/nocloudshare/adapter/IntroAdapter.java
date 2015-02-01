package de.ub0r.android.nocloudshare.adapter;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.ub0r.android.nocloudshare.R;

/**
 * @author flx
 */
public class IntroAdapter extends PagerAdapter {

    private static final int[] IMAGES = {
            R.drawable.intro_0,
            R.drawable.intro_1,
            R.drawable.intro_2,
    };

    private static final int[] TEXTS = {
            R.string.intro_0,
            R.string.intro_1,
            R.string.intro_2,
    };

    private final Context mContext;

    private final LayoutInflater mInflater;

    public IntroAdapter(final Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {
        View v = mInflater.inflate(R.layout.item_intro, container, false);
        ImageView iv = (ImageView) v.findViewById(android.R.id.icon);
        TextView tv1 = (TextView) v.findViewById(android.R.id.text1);
        TextView tv2 = (TextView) v.findViewById(android.R.id.text2);
        iv.setImageResource(IMAGES[position]);
        tv1.setText(mContext.getString(R.string.step_num, position + 1));
        tv2.setText(TEXTS[position]);
        container.addView(v);
        return v;
    }

    @Override
    public void destroyItem(final ViewGroup container, final int position,
            final Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public boolean isViewFromObject(final View view, final Object object) {
        return view == object;
    }
}