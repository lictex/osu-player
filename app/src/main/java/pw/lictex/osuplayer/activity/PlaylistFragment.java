package pw.lictex.osuplayer.activity;

import android.animation.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.*;
import androidx.fragment.app.*;
import androidx.interpolator.view.animation.*;
import androidx.lifecycle.Observer;
import androidx.lifecycle.*;
import androidx.preference.*;
import androidx.recyclerview.widget.*;

import java.nio.charset.*;
import java.util.*;

import butterknife.*;
import lombok.*;
import pw.lictex.osuplayer.R;
import pw.lictex.osuplayer.*;
import pw.lictex.osuplayer.storage.*;

public class PlaylistFragment extends Fragment {

    @BindView(R.id.recyclerView) RecyclerView mRecyclerView;
    @BindView(R.id.searchText) EditText searchText;
    @BindView(R.id.searchView) View searchView;
    @BindView(R.id.searchStatus) View searchStatus;
    @BindView(R.id.searchArea) View searchArea;
    @BindView(R.id.searchIcon) View searchIcon;
    @BindView(R.id.status) View statusAll;
    @BindView(R.id.buttonAll) Button buttonAll;
    @BindView(R.id.buttonFavorite) Button buttonFavorite;
    @BindView(R.id.buttonCloseSearch) ImageButton buttonClearSearch;
    @BindView(R.id.buttonListOrder) ImageButton buttonListOrder;
    @Getter private boolean showCollectionList = false;
    private BeatmapIndex.Order listOrder = BeatmapIndex.Order.Title;
    private LiveData<List<BeatmapEntity>> allMapLiveData;
    private LiveData<List<BeatmapEntity>> collectionMapLiveData;

    private boolean scrollToCurrentEnabled = false;
    private final Observer<List<BeatmapEntity>> allMapObserver = beatmapEntities -> {
        var playerService = ((MainActivity) getActivity()).getPlayerService();
        playerService.getAllMapList().clear();
        playerService.getAllMapList().addAll(beatmapEntities);
        if (scrollToCurrentEnabled) refreshListToCurrent();
        else refreshList();
    };
    private final Observer<List<BeatmapEntity>> collectionMapObserver = beatmapEntities -> {
        var playerService = ((MainActivity) getActivity()).getPlayerService();
        playerService.getCollectionMapList().clear();
        playerService.getCollectionMapList().addAll(beatmapEntities);
        if (scrollToCurrentEnabled) refreshListToCurrent();
        else refreshList();
    };

    @OnTextChanged(R.id.searchText) void onTextChanged() {
        refreshList();
    }

    @OnFocusChange(R.id.searchText) void onTextFocusChanged() {
        if (searchText.hasFocus()) return;
        Utils.hideSoftInput(searchText);
        if (searchText.getText().toString().trim().isEmpty()) onCloseSearchClick();
    }

    @OnClick(R.id.searchIcon) void onOpenSearchClick() {
        openSearch(true);
    }

    @OnClick(R.id.buttonCloseSearch) void onCloseSearchClick() {
        int baseAnimationDuration = ((MainActivity) getActivity()).getBaseAnimationDuration();
        ValueAnimator animator = ValueAnimator.ofInt((int) searchArea.getTranslationX(), searchArea.getMeasuredWidth() - searchIcon.getMeasuredWidth());
        animator.addUpdateListener(valueAnimator -> searchArea.setTranslationX((Integer) valueAnimator.getAnimatedValue()));
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.setDuration(baseAnimationDuration);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation, boolean isReverse) {
                searchText.setText("");
            }
        });
        animator.start();
        buttonClearSearch.animate().setStartDelay(0).alpha(0f).setDuration(baseAnimationDuration / 2).setInterpolator(new FastOutSlowInInterpolator()).start();
        searchStatus.animate().alpha(0f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();

        Utils.clearFocus(getActivity());
    }

    @OnClick(R.id.buttonAll) void onAllClick() {
        setPlaylist(false, true);
    }

    @OnClick(R.id.buttonFavorite) void onFavoriteClick() {
        setPlaylist(true, true);
    }

    public void setPlaylist(boolean collection, boolean scrollToCurrent) {
        int baseAnimationDuration = ((MainActivity) getActivity()).getBaseAnimationDuration();
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) statusAll.getLayoutParams();
        var anim = ValueAnimator.ofFloat(params.horizontalBias, collection ? 1 : 0);
        anim.addUpdateListener(valueAnimator -> {
            params.horizontalBias = (Float) valueAnimator.getAnimatedValue();
            statusAll.setLayoutParams(params);
        });
        anim.setDuration(baseAnimationDuration);
        anim.setInterpolator(new FastOutSlowInInterpolator());
        anim.start();

        buttonAll.animate().alpha(collection ? .75f : 1f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();
        buttonFavorite.animate().alpha(collection ? 1f : .75f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();

        showCollectionList = collection;
        if (scrollToCurrent) refreshListToCurrent();
        else refreshList();
    }

    @OnClick(R.id.buttonListOrder) void onListOrderClick() {
        scrollToCurrentEnabled = true;
        switch (listOrder) {
            case Title:
                setListOrder(BeatmapIndex.Order.Artist); break;
            case Artist:
                setListOrder(BeatmapIndex.Order.Creator); break;
            case Creator:
                setListOrder(BeatmapIndex.Order.Title); break;
        }
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt("playlist_order", listOrder.ordinal()).apply();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        ButterKnife.bind(this, view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        HomeAdapter adapter = new HomeAdapter();
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int baseAnimationDuration = ((MainActivity) getActivity()).getBaseAnimationDuration();
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(baseAnimationDuration);
        animator.setChangeDuration(baseAnimationDuration);
        animator.setMoveDuration(baseAnimationDuration);
        animator.setRemoveDuration(baseAnimationDuration / 2);
        mRecyclerView.setItemAnimator(animator);

        var playerService = ((MainActivity) getActivity()).getPlayerService();

        int order = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("playlist_order", BeatmapIndex.Order.Title.ordinal());
        order = order < BeatmapIndex.Order.values().length ? order : 0;
        setListOrder(BeatmapIndex.Order.values()[order]);

        showCollectionList = playerService.isPlayCollectionList();
        if (showCollectionList) setPlaylist(true, false);

        container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                searchArea.setTranslationX(searchArea.getMeasuredWidth() - searchIcon.getMeasuredWidth());
                buttonClearSearch.setAlpha(0f);
                searchStatus.setAlpha(0f);
            }
        });

        return view;
    }

    public void openSearch(boolean showKeyboard) {
        int baseAnimationDuration = ((MainActivity) getActivity()).getBaseAnimationDuration();
        ValueAnimator animator = ValueAnimator.ofInt((int) searchArea.getTranslationX(), 0);
        animator.addUpdateListener(valueAnimator -> searchArea.setTranslationX((Integer) valueAnimator.getAnimatedValue()));
        animator.setDuration(baseAnimationDuration);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        if (showKeyboard) animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                searchText.post(() -> {
                    searchText.requestFocus();
                    Utils.showSoftInput(searchText);
                });
            }
        });
        animator.start();
        buttonClearSearch.animate().setStartDelay(baseAnimationDuration / 2).alpha(1f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();
        searchStatus.animate().alpha(1f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();
    }

    private void refreshList(Runnable onFinish) {
        LiveData<List<BeatmapEntity>> beatmaps = showCollectionList ? BeatmapIndex.getInstance().getFavoriteBeatmaps(searchText.getText().toString().trim(), listOrder) : BeatmapIndex.getInstance().getAllBeatmaps(searchText.getText().toString().trim(), listOrder);
        beatmaps.observe(this, new Observer<List<BeatmapEntity>>() {
            @Override public void onChanged(List<BeatmapEntity> beatmapEntities) {
                ((HomeAdapter) mRecyclerView.getAdapter()).list = beatmapEntities;
                mRecyclerView.getAdapter().notifyDataSetChanged();
                beatmaps.removeObserver(this);
                onFinish.run();
            }
        });
    }

    public void refreshList() {
        int index = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        View v = mRecyclerView.getChildAt(0);
        int offset = (v == null) ? 0 : (v.getTop() - mRecyclerView.getPaddingTop());
        refreshListToPosition(index, offset);
    }

    public void refreshListToPosition(int index, int offset) {
        refreshList(() -> {
            if (index >= 0) ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPositionWithOffset(index, offset);
        });
    }

    public void refreshListToCurrent() {
        refreshList(() -> mRecyclerView.scrollToPosition(((HomeAdapter) mRecyclerView.getAdapter()).list.indexOf(((MainActivity) getActivity()).getPlayerService().getCurrentMap())));
    }

    private void setListOrder(BeatmapIndex.Order order) {
        listOrder = order;
        if (allMapLiveData != null) {
            allMapLiveData.removeObserver(allMapObserver);
            allMapLiveData = null;
        }
        if (collectionMapLiveData != null) {
            collectionMapLiveData.removeObserver(allMapObserver);
            collectionMapLiveData = null;
        }

        allMapLiveData = BeatmapIndex.getInstance().getAllBeatmaps("", listOrder);
        allMapLiveData.observe(this, allMapObserver);
        collectionMapLiveData = BeatmapIndex.getInstance().getFavoriteBeatmaps("", listOrder);
        collectionMapLiveData.observe(this, collectionMapObserver);

        buttonListOrder.setImageDrawable(listOrder == BeatmapIndex.Order.Title ? getResources().getDrawable(R.drawable.ic_sort_title) : (listOrder == BeatmapIndex.Order.Artist ? getResources().getDrawable(R.drawable.ic_sort_artist) : getResources().getDrawable(R.drawable.ic_sort_mapper)));
    }

    protected class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.PlaylistViewHolder> {
        List<BeatmapEntity> list = new ArrayList<>();

        @NonNull @Override
        public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PlaylistViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.item_playlist, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
            var playerService = ((MainActivity) getActivity()).getPlayerService();
            BeatmapEntity beatmapEntity = list.get(position);

            var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            if (sharedPreferences.getBoolean("use_unicode_metadata", false))
                holder.getTitle().setText(getString(R.string.title_artist, beatmapEntity.unicode_title, beatmapEntity.unicode_artist));
            else
                holder.getTitle().setText(getString(R.string.title_artist, beatmapEntity.title, beatmapEntity.artist));

            holder.getVersion().setText(getString(R.string.version_by_mapper, beatmapEntity.version, beatmapEntity.creator));
            BeatmapEntity currentMap = ((MainActivity) getActivity()).getPlayerService().getCurrentMap();
            holder.getPlaying().setVisibility(beatmapEntity.equals(currentMap) ? View.VISIBLE : View.GONE);

            if (playerService.getCollectionMapList().contains(beatmapEntity)) {
                holder.getFavorite().setAlpha(1f);
                holder.getFavorite().setImageDrawable(getResources().getDrawable(R.drawable.ic_heart));
            } else {
                holder.getFavorite().setAlpha(0.75f);
                holder.getFavorite().setImageDrawable(getResources().getDrawable(R.drawable.ic_heart_outline));
            }
            holder.getFavorite().setOnClickListener(a -> {
                if (playerService.getCollectionMapList().contains(beatmapEntity))
                    BeatmapIndex.getInstance().removeCollection(beatmapEntity);
                else
                    BeatmapIndex.getInstance().addCollection(beatmapEntity);
            });

            holder.getRoot().setOnClickListener(a -> Utils.runTask(() -> {
                playerService.setPlayCollectionList(showCollectionList);
                playerService.play(playerService.getPlaylist().indexOf(beatmapEntity));
            }));
        }

        @Override
        public long getItemId(int position) {
            return UUID.nameUUIDFromBytes(list.get(position).path.getBytes(Charset.forName("UTF-8"))).getMostSignificantBits(); //hope there will be no conflicts....
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class PlaylistViewHolder extends RecyclerView.ViewHolder {
            @Getter @BindView(R.id.title) TextView title;
            @Getter @BindView(R.id.version) TextView version;
            @Getter @BindView(R.id.root) View root;
            @Getter @BindView(R.id.playing) ImageView playing;
            @Getter @BindView(R.id.imageButton) ImageButton favorite;

            private PlaylistViewHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);
            }
        }
    }
}
