package pw.lictex.osuplayer.activity;

import android.animation.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.*;
import androidx.core.app.*;
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
    @BindView(R.id.status) View statusAll;
    @BindView(R.id.buttonAll) Button buttonAll;
    @BindView(R.id.buttonFavorite) Button buttonFavorite;
    @BindView(R.id.buttonClearSearch) ImageButton buttonClearSearch;
    @BindView(R.id.buttonListOrder) ImageButton buttonListOrder;
    private boolean showCollectionList = false;
    private BeatmapIndex.Order listOrder = BeatmapIndex.Order.Title;
    private LiveData<List<BeatmapEntity>> allMapLiveData;
    private LiveData<List<BeatmapEntity>> collectionMapLiveData;

    private final Observer<List<BeatmapEntity>> allMapObserver = beatmapEntities -> {
        var playerService = ((MainActivity) getActivity()).getPlayerService();
        playerService.getAllMapList().clear();
        playerService.getAllMapList().addAll(beatmapEntities);
        refreshList();
    };
    private final Observer<List<BeatmapEntity>> collectionMapObserver = beatmapEntities -> {
        var playerService = ((MainActivity) getActivity()).getPlayerService();
        playerService.getCollectionMapList().clear();
        playerService.getCollectionMapList().addAll(beatmapEntities);
        refreshList();
    };

    @OnTextChanged(R.id.searchText) void onTextChanged() {
        refreshList();
        int baseAnimationDuration = ((MainActivity) getActivity()).getBaseAnimationDuration();
        searchView.animate().alpha(searchText.getText().toString().isEmpty() ? 0.75f : 1f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();
        buttonClearSearch.animate().alpha(searchText.getText().toString().isEmpty() ? 0f : 1f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();
    }

    @OnFocusChange(R.id.searchText) void onTextFocusChanged() {
        if (searchText.hasFocus()) return;
        InputMethodManager inputMethodManager = ActivityCompat.getSystemService(getContext(), InputMethodManager.class);
        inputMethodManager.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
    }

    @OnClick(R.id.buttonAll) void onAllClick() {
        int baseAnimationDuration = ((MainActivity) getActivity()).getBaseAnimationDuration();
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) statusAll.getLayoutParams();
        var anim = ValueAnimator.ofFloat(params.horizontalBias, 0);
        anim.addUpdateListener(valueAnimator -> {
            params.horizontalBias = (Float) valueAnimator.getAnimatedValue();
            statusAll.setLayoutParams(params);
        });
        anim.setDuration(baseAnimationDuration);
        anim.setInterpolator(new FastOutSlowInInterpolator());
        anim.start();

        buttonAll.animate().alpha(1).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();
        buttonFavorite.animate().alpha(.75f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();

        showCollectionList = false;
        refreshList();
    }

    @OnClick(R.id.buttonFavorite) void onFavoriteClick() {
        int baseAnimationDuration = ((MainActivity) getActivity()).getBaseAnimationDuration();
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) statusAll.getLayoutParams();
        var anim = ValueAnimator.ofFloat(params.horizontalBias, 1);
        anim.addUpdateListener(valueAnimator -> {
            params.horizontalBias = (Float) valueAnimator.getAnimatedValue();
            statusAll.setLayoutParams(params);
        });
        anim.setDuration(baseAnimationDuration);
        anim.setInterpolator(new FastOutSlowInInterpolator());
        anim.start();

        buttonAll.animate().alpha(.75f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();
        buttonFavorite.animate().alpha(1).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();

        showCollectionList = true;
        refreshList();
    }

    @OnClick(R.id.buttonClearSearch) void onClearSearchClick() {
        searchText.setText("");
    }

    @OnClick(R.id.buttonListOrder) void onListOrderClick() {
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
        if (showCollectionList) onFavoriteClick();

        return view;
    }

    void setListOrder(BeatmapIndex.Order order) {
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

    public void refreshList() {
        LiveData<List<BeatmapEntity>> beatmaps = showCollectionList ? BeatmapIndex.getInstance().getFavoriteBeatmaps(searchText.getText().toString().trim(), listOrder) : BeatmapIndex.getInstance().getAllBeatmaps(searchText.getText().toString().trim(), listOrder);
        beatmaps.observe(this, new Observer<List<BeatmapEntity>>() {
            @Override public void onChanged(List<BeatmapEntity> beatmapEntities) {
                ((HomeAdapter) mRecyclerView.getAdapter()).list = beatmapEntities;
                mRecyclerView.getAdapter().notifyDataSetChanged();
                beatmaps.removeObserver(this);
            }
        });
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
