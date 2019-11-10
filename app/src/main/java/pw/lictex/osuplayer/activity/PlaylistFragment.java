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
    private boolean showCollectionList = false;

    @OnTextChanged(R.id.searchText) void onTextChanged() {
        refreshList();
        searchView.animate().alpha(searchText.getText().toString().isEmpty() ? 0.75f : 1f).setDuration(200).start();
    }

    @OnFocusChange(R.id.searchText) void onTextFocusChanged() {
        if (searchText.hasFocus()) return;
        InputMethodManager inputMethodManager = ActivityCompat.getSystemService(getContext(), InputMethodManager.class);
        inputMethodManager.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
    }

    @OnClick(R.id.buttonAll) void onAllClick() {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) statusAll.getLayoutParams();
        var anim = ValueAnimator.ofFloat(params.horizontalBias, 0);
        anim.addUpdateListener(valueAnimator -> {
            params.horizontalBias = (Float) valueAnimator.getAnimatedValue();
            statusAll.setLayoutParams(params);
        });
        anim.setDuration(200);
        anim.start();

        buttonAll.animate().alpha(1).setDuration(200).start();
        buttonFavorite.animate().alpha(.75f).setDuration(200).start();

        showCollectionList = false;
        refreshList();
    }

    @OnClick(R.id.buttonFavorite) void onFavoriteClick() {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) statusAll.getLayoutParams();
        var anim = ValueAnimator.ofFloat(params.horizontalBias, 1);
        anim.addUpdateListener(valueAnimator -> {
            params.horizontalBias = (Float) valueAnimator.getAnimatedValue();
            statusAll.setLayoutParams(params);
        });
        anim.setDuration(200);
        anim.start();

        buttonAll.animate().alpha(.75f).setDuration(200).start();
        buttonFavorite.animate().alpha(1).setDuration(200).start();

        showCollectionList = true;
        refreshList();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        ButterKnife.bind(this, view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        HomeAdapter adapter = new HomeAdapter();
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        var playerService = ((MainActivity) getActivity()).getPlayerService();

        var allMapLiveData = BeatmapIndex.getInstance().getAllBeatmaps();
        allMapLiveData.observe(this, beatmapEntities -> {
            playerService.getAllMapList().clear();
            playerService.getAllMapList().addAll(beatmapEntities);
            refreshList();
        });
        var collectionMapLiveData = BeatmapIndex.getInstance().getFavoriteBeatmaps();
        collectionMapLiveData.observe(this, beatmapEntities -> {
            playerService.getCollectionMapList().clear();
            playerService.getCollectionMapList().addAll(beatmapEntities);
            refreshList();
        });

        showCollectionList = playerService.isPlayCollectionList();
        if (showCollectionList) onFavoriteClick();

        return view;
    }

    public void refreshList() {
        LiveData<List<BeatmapEntity>> beatmaps = showCollectionList ? BeatmapIndex.getInstance().getFavoriteBeatmaps(searchText.getText().toString().trim()) : BeatmapIndex.getInstance().getAllBeatmaps(searchText.getText().toString().trim());
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
