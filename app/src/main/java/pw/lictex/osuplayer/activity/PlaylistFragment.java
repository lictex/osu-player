package pw.lictex.osuplayer.activity;

import android.animation.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.*;
import androidx.core.content.res.*;
import androidx.fragment.app.*;
import androidx.interpolator.view.animation.*;
import androidx.lifecycle.Observer;
import androidx.lifecycle.*;
import androidx.preference.*;
import androidx.recyclerview.widget.*;

import java.nio.charset.*;
import java.util.*;

import lombok.*;
import pw.lictex.osuplayer.R;
import pw.lictex.osuplayer.*;
import pw.lictex.osuplayer.databinding.*;
import pw.lictex.osuplayer.storage.*;

public class PlaylistFragment extends Fragment {
    @Getter private FragmentPlaylistBinding views;

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

    private void onTextChanged() {
        refreshListToCurrent();
    }

    private void onTextFocusChanged(View v, boolean b) {
        if (views.searchText.hasFocus()) return;
        Utils.hideSoftInput(views.searchText);
        if (views.searchText.getText().toString().trim().isEmpty()) onCloseSearchClick(v);
    }

    private void onOpenSearchClick(View v) {
        openSearch(true);
    }

    private void onCloseSearchClick(View v) {
        int baseAnimationDuration = ((MainActivity) getActivity()).getBaseAnimationDuration();
        ValueAnimator animator = ValueAnimator.ofInt((int) views.searchArea.getTranslationX(), views.searchArea.getMeasuredWidth() - views.searchIcon.getMeasuredWidth());
        animator.addUpdateListener(valueAnimator -> views.searchArea.setTranslationX((Integer) valueAnimator.getAnimatedValue()));
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.setDuration(baseAnimationDuration);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation, boolean isReverse) {
                views.searchText.setText("");
            }
        });
        animator.start();
        views.buttonCloseSearch.animate().setStartDelay(0).alpha(0f).setDuration(baseAnimationDuration / 2).setInterpolator(new FastOutSlowInInterpolator()).start();
        views.searchStatus.animate().alpha(0f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();

        Utils.clearFocus(getActivity());
    }

    private void onAllClick(View v) {
        setPlaylist(false, true);
    }

    private void onFavoriteClick(View v) {
        setPlaylist(true, true);
    }

    public void setPlaylist(boolean collection, boolean scrollToCurrent) {
        int baseAnimationDuration = ((MainActivity) getActivity()).getBaseAnimationDuration();
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) views.status.getLayoutParams();
        var anim = ValueAnimator.ofFloat(params.horizontalBias, collection ? 1 : 0);
        anim.addUpdateListener(valueAnimator -> {
            params.horizontalBias = (Float) valueAnimator.getAnimatedValue();
            views.status.setLayoutParams(params);
        });
        anim.setDuration(baseAnimationDuration);
        anim.setInterpolator(new FastOutSlowInInterpolator());
        anim.start();

        views.buttonAll.animate().alpha(collection ? .75f : 1f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();
        views.buttonFavorite.animate().alpha(collection ? 1f : .75f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();

        showCollectionList = collection;
        if (scrollToCurrent) refreshListToCurrent();
        else refreshList();
    }

    private void onListOrderClick(View v) {
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
        views = FragmentPlaylistBinding.bind(view);
        views.searchText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                PlaylistFragment.this.onTextChanged();
            }

            @Override public void afterTextChanged(Editable s) {

            }
        });
        views.searchText.setOnFocusChangeListener(this::onTextFocusChanged);
        views.searchIcon.setOnClickListener(this::onOpenSearchClick);
        views.buttonCloseSearch.setOnClickListener(this::onCloseSearchClick);
        views.buttonAll.setOnClickListener(this::onAllClick);
        views.buttonFavorite.setOnClickListener(this::onFavoriteClick);
        views.buttonListOrder.setOnClickListener(this::onListOrderClick);

        views.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        HomeAdapter adapter = new HomeAdapter();
        adapter.setHasStableIds(true);
        views.recyclerView.setAdapter(adapter);
        int baseAnimationDuration = ((MainActivity) getActivity()).getBaseAnimationDuration();
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(baseAnimationDuration);
        animator.setChangeDuration(baseAnimationDuration);
        animator.setMoveDuration(baseAnimationDuration);
        animator.setRemoveDuration(baseAnimationDuration / 2);
        views.recyclerView.setItemAnimator(animator);

        var playerService = ((MainActivity) getActivity()).getPlayerService();

        int order = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("playlist_order", BeatmapIndex.Order.Title.ordinal());
        order = order < BeatmapIndex.Order.values().length ? order : 0;
        setListOrder(BeatmapIndex.Order.values()[order]);

        showCollectionList = playerService.isPlayCollectionList();
        if (showCollectionList) setPlaylist(true, false);

        container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                views.searchArea.setTranslationX(views.searchArea.getMeasuredWidth() - views.searchIcon.getMeasuredWidth());
                views.buttonCloseSearch.setAlpha(0f);
                views.searchStatus.setAlpha(0f);
            }
        });

        return view;
    }

    public void openSearch(boolean showKeyboard) {
        int baseAnimationDuration = ((MainActivity) getActivity()).getBaseAnimationDuration();
        ValueAnimator animator = ValueAnimator.ofInt((int) views.searchArea.getTranslationX(), 0);
        animator.addUpdateListener(valueAnimator -> views.searchArea.setTranslationX((Integer) valueAnimator.getAnimatedValue()));
        animator.setDuration(baseAnimationDuration);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        if (showKeyboard) animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                views.searchText.post(() -> {
                    views.searchText.requestFocus();
                    Utils.showSoftInput(views.searchText);
                });
            }
        });
        animator.start();
        views.buttonCloseSearch.animate().setStartDelay(baseAnimationDuration / 2).alpha(1f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();
        views.searchStatus.animate().alpha(1f).setDuration(baseAnimationDuration).setInterpolator(new FastOutSlowInInterpolator()).start();
    }

    private void refreshList(Runnable onFinish) {
        LiveData<List<BeatmapEntity>> beatmaps = showCollectionList ? BeatmapIndex.getInstance().getFavoriteBeatmaps(views.searchText.getText().toString().trim(), listOrder) : BeatmapIndex.getInstance().getAllBeatmaps(views.searchText.getText().toString().trim(), listOrder);
        beatmaps.observe(getViewLifecycleOwner(), new Observer<List<BeatmapEntity>>() {
            @Override public void onChanged(List<BeatmapEntity> beatmapEntities) {
                ((HomeAdapter) views.recyclerView.getAdapter()).list = beatmapEntities;
                views.recyclerView.getAdapter().notifyDataSetChanged();
                beatmaps.removeObserver(this);
                onFinish.run();
            }
        });
    }

    public void refreshList() {
        int index = ((LinearLayoutManager) views.recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        View v = views.recyclerView.getChildAt(0);
        int offset = (v == null) ? 0 : (v.getTop() - views.recyclerView.getPaddingTop());
        refreshListToPosition(index, offset);
    }

    public void refreshListToPosition(int index, int offset) {
        refreshList(() -> {
            if (index >= 0) ((LinearLayoutManager) views.recyclerView.getLayoutManager()).scrollToPositionWithOffset(index, offset);
        });
    }

    public void refreshListToCurrent() {
        refreshList(() -> {
            views.recyclerView.stopScroll();
            ((LinearLayoutManager) views.recyclerView.getLayoutManager()).scrollToPositionWithOffset(((HomeAdapter) views.recyclerView.getAdapter()).list.indexOf(((MainActivity) getActivity()).getPlayerService().getCurrentMap()), Utils.dp2px(getContext(), 24));
        });
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
        allMapLiveData.observe(getViewLifecycleOwner(), allMapObserver);
        collectionMapLiveData = BeatmapIndex.getInstance().getFavoriteBeatmaps("", listOrder);
        collectionMapLiveData.observe(getViewLifecycleOwner(), collectionMapObserver);

        views.buttonListOrder.setImageDrawable(listOrder == BeatmapIndex.Order.Title ?
                ResourcesCompat.getDrawable(getResources(), R.drawable.ic_sort_title, null) :
                (listOrder == BeatmapIndex.Order.Artist ?
                        ResourcesCompat.getDrawable(getResources(), R.drawable.ic_sort_artist, null) :
                        ResourcesCompat.getDrawable(getResources(), R.drawable.ic_sort_mapper, null)
                ));
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
                holder.getFavorite().setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_heart, null));
            } else {
                holder.getFavorite().setAlpha(0.75f);
                holder.getFavorite().setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_heart_outline, null));
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
            return UUID.nameUUIDFromBytes(list.get(position).path.getBytes(StandardCharsets.UTF_8)).getMostSignificantBits(); //hope there will be no conflicts....
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class PlaylistViewHolder extends RecyclerView.ViewHolder {
            @Getter TextView title;
            @Getter TextView version;
            @Getter View root;
            @Getter ImageView playing;
            @Getter ImageButton favorite;

            private PlaylistViewHolder(View view) {
                super(view);
                title = view.findViewById(R.id.title);
                version = view.findViewById(R.id.version);
                root = view.findViewById(R.id.root);
                playing = view.findViewById(R.id.playing);
                favorite = view.findViewById(R.id.imageButton);
            }
        }
    }
}
