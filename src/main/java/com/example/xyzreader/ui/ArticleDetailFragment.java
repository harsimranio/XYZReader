package com.example.xyzreader.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";
    private static final float PERCENTAGE_TO_SHOW_TITLE = 0.9F;

    private static final int FADE_IN_DURATION = 1000;
    private static final int FADE_OUT_DURATION = 300;

    public static final String ARG_ITEM_ID = "item_id";

    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;

    private DynamicHeightNetworkImageView mPhotoView;
    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbarLayout;

    private TextView titleTextView, bylineTextView, bodyTextView, toolbarTitle;

    private LinearLayout metaBar;

    private OnFragmentInteractionListener listener;

    private boolean isToolbarTitleVisible;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.

        getLoaderManager().initLoader(0, null, this);
        startAlphaAnimation(toolbarTitle, 0, View.INVISIBLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mPhotoView = (DynamicHeightNetworkImageView) mRootView.findViewById(R.id.photo);

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();
        setUpToolbar();
        return mRootView;
    }

    private void setUpToolbar() {
        toolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onBackPressed();
            }
        });
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        titleTextView = (TextView) mRootView.findViewById(R.id.article_title);
        bylineTextView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineTextView.setMovementMethod(new LinkMovementMethod());
        bodyTextView = (TextView) mRootView.findViewById(R.id.article_body);
        bodyTextView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));
        toolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
        AppBarLayout appBarLayout = (AppBarLayout) mRootView.findViewById(R.id.appBarLayout);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                int maxScroll = appBarLayout.getTotalScrollRange();
                float percentage = (float) Math.abs(verticalOffset) / (float) maxScroll;

                handleToolbarTitle(percentage);
            }
        });

        metaBar = (LinearLayout) mRootView.findViewById(R.id.meta_bar);
        collapsingToolbarLayout = (CollapsingToolbarLayout)
                mRootView.findViewById(R.id.collapsingToolbarLayout);
        toolbarTitle = (TextView) mRootView.findViewById(R.id.title);
    }

    private void handleToolbarTitle(float percentage) {
        if (percentage >= PERCENTAGE_TO_SHOW_TITLE) {
            if (!isToolbarTitleVisible) {
                startAlphaAnimation(toolbarTitle, FADE_IN_DURATION, View.VISIBLE);
                isToolbarTitleVisible = true;
            }
        } else {
            if (isToolbarTitleVisible) {
                startAlphaAnimation(toolbarTitle, FADE_OUT_DURATION, View.INVISIBLE);
                isToolbarTitleVisible = false;
            }
        }
    }

    public static void startAlphaAnimation(View view, long duration, int visibility) {
        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE)
                ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);

        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        view.startAnimation(alphaAnimation);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        if (cursor == null || cursor.isClosed()) {
            return;
        }

        if (!cursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            cursor.close();
            return;
        }

        String title = cursor.getString(ArticleLoader.Query.TITLE);
        titleTextView.setText(title);
        toolbarTitle.setText(title);

        Spanned byline, body;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            byline = Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            cursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + cursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>", Html.FROM_HTML_MODE_LEGACY);
            body = Html.fromHtml(cursor.getString(ArticleLoader.Query.BODY),
                    Html.FROM_HTML_MODE_LEGACY);
        } else {
            //noinspection deprecation
            byline = Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            cursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + cursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>");

            //noinspection deprecation
            body = Html.fromHtml(cursor.getString(ArticleLoader.Query.BODY));
        }
        bylineTextView.setText(byline);
        bodyTextView.setText(body);

        if (getResources().getBoolean(R.bool.small_screen)) {
            mPhotoView.setAspectRatio(1f / 1.2f);
        } else {
            mPhotoView.setAspectRatio(2f);
        }

        Picasso.with(getActivity())
                .load(cursor.getString(ArticleLoader.Query.PHOTO_URL))
                .transform(PaletteTransformation.instance())
                .into(mPhotoView, new Callback.EmptyCallback() {
                    @Override
                    public void onSuccess() {
                        Bitmap bitmap = ((BitmapDrawable) mPhotoView.getDrawable()).getBitmap();
                        Palette palette = PaletteTransformation.getPalette(bitmap);

                        int mutedColor = palette.getMutedColor(mMutedColor);
                        metaBar.setBackgroundColor(mutedColor);
                        collapsingToolbarLayout.setContentScrimColor(mutedColor);
                        collapsingToolbarLayout.setStatusBarScrimColor(mutedColor);
                    }
                });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new IllegalStateException("OnFragmentInteractionListener is not implemented");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnFragmentInteractionListener {
        void onBackPressed();
    }
}
