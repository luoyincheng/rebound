/*
 * This file provided by Facebook is for non-commercial testing and evaluation purposes only.
 * Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.rebound.playground.app;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.facebook.rebound.SpringUtil;
import com.facebook.rebound.playground.R;
import com.facebook.rebound.playground.examples.BallExample;
import com.facebook.rebound.playground.examples.PhotoGalleryExample;
import com.facebook.rebound.playground.examples.SpringChainExample;
import com.facebook.rebound.playground.examples.OrigamiExample;
import com.facebook.rebound.playground.examples.SimpleExample;
import com.facebook.rebound.playground.examples.SpringScrollViewExample;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

public class PlaygroundActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final List<Sample> SAMPLES = new ArrayList<Sample>();

    static {
        SAMPLES.add(new Sample(SimpleExample.class, "Simple Example", "Scale a photo when you press and release"));
        SAMPLES.add(new Sample(SpringScrollViewExample.class, "Scroll View", "A scroll view with spring physics"));
        SAMPLES.add(new Sample(SpringChainExample.class, "SpringChain", "Drag any row in the list."));
        SAMPLES.add(new Sample(PhotoGalleryExample.class, "Photo Gallery", "Tap on a photo to enlarge or minimize."));
        SAMPLES.add(new Sample(BallExample.class, "Inertia Ball", "Toss the ball around the screen and watch it settle"));
        SAMPLES.add(new Sample(OrigamiExample.class, "Origami Example", "Rebound port of an Origami composition"));
    }

    private ListView mListView;
    private View mRootContainer;
    private ExampleListAdapter mAdapter;
    private ViewGroup mRootView;
    private ExampleContainerView mCurrentExample;//extends FrameLayout
    private boolean mAnimating;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playground);
        mInflater = LayoutInflater.from(this);
        mRootView = (ViewGroup) findViewById(R.id.root);
        mRootContainer = findViewById(R.id.root_container);
        mListView = (ListView) findViewById(R.id.list_view);
        mAdapter = new ExampleListAdapter();
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.playground, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mAnimating) {
            return;
        }

        Class<? extends View> clazz = SAMPLES.get(position).viewClass;
        View sampleView = null;
        try {
            /***
             * {@code Constructor} provides information about, and access to, a single
             * constructor for a class.
             *
             * <p>{@code Constructor} permits widening(拓宽,扩展) conversions(转换) to occur when matching the
             * actual parameters to newInstance() with the underlying(根本的,潜在的,在下面的)
             * constructor's formal(有条理的,正式的) parameters, but throws an
             * {@code IllegalArgumentException} if a narrowing conversion would occur.
             *
             * @param <T> the class in which the constructor is declared(声明的，宣布的)
             *
             */
            Constructor<? extends View> ctor = clazz.getConstructor(Context.class);//// TODO: 2018/4/27  to un
            sampleView = ctor.newInstance(PlaygroundActivity.this);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (sampleView == null) {
            return;
        }
        mAnimating = true;

        mCurrentExample = new ExampleContainerView(this);//只要有Context就能构造出来一个View
        mCurrentExample.addView(sampleView);
        mRootView.addView(mCurrentExample);

        /**
         * reveal:显示,揭露
         * <p>Causes the Runnable to be added to the message queue, to be run
         * after the specified amount of time elapses.
         * The runnable will be run on the user interface thread.</p>
         */
        mCurrentExample.postDelayed(new Runnable() {
            @Override
            public void run() {
                mCurrentExample.reveal(true, new ExampleContainerView.Callback() {
                    @Override
                    public void onProgress(double progress) {
                        float scale = (float) SpringUtil.mapValueFromRangeToRange(progress, 0, 1, 0.8, 1);
                        mRootContainer.setScaleX(scale);
                        mRootContainer.setScaleY(scale);
                        mRootContainer.setAlpha((float) progress);
                    }

                    @Override
                    public void onEnd() {
                        mAnimating = false;
                    }
                });
            }
        }, 100);
    }

    @Override
    public void onBackPressed() {
        if (mAnimating || mCurrentExample == null) {
            return;
        }
        mAnimating = true;
        mCurrentExample.hide(true, new ExampleContainerView.Callback() {
            @Override
            public void onProgress(double progress) {
                float scale = (float) SpringUtil.mapValueFromRangeToRange(progress, 0, 1, 0.8, 1);
                mRootContainer.setScaleX(scale);
                mRootContainer.setScaleY(scale);
                mRootContainer.setAlpha((float) progress);
            }

            @Override
            public void onEnd() {
                mAnimating = false;
                mCurrentExample.clearCallback();
                mRootView.removeView(mCurrentExample);
                mCurrentExample = null;
            }
        });
    }

    private class ExampleListAdapter implements ListAdapter {

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public int getCount() {
            return SAMPLES.size();
        }

        @Override
        public Object getItem(int position) {
            return SAMPLES.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            RowView rowView;
            if (convertView != null) {
                rowView = (RowView) convertView;
            } else {
                rowView = new RowView(PlaygroundActivity.this);
            }
            rowView.setText(SAMPLES.get(position).text);
            rowView.setSubtext(SAMPLES.get(position).subtext);
            return rowView;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return SAMPLES.isEmpty();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }
    }

    private static class Sample {
        public Class<? extends View> viewClass;// TODO: 2018/4/27  to un
        public String text;
        public String subtext;

        public Sample(Class<? extends View> viewClass, String text, String subtext) {
            this.viewClass = viewClass;
            this.text = text;
            this.subtext = subtext;
        }
    }
}
