package pw.lictex.osuplayer.activity;

import android.animation.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.*;
import androidx.fragment.app.*;
import androidx.recyclerview.widget.*;

import java.util.*;

import butterknife.*;
import lombok.*;
import pw.lictex.osuplayer.R;
import pw.lictex.osuplayer.storage.*;


public class PlaylistFragment extends Fragment {

    @BindView(R.id.recyclerView) RecyclerView mRecyclerView;
    @BindView(R.id.searchText) EditText searchText;
    @BindView(R.id.searchView) View searchView;
    @BindView(R.id.status) View statusAll;
    @BindView(R.id.buttonAll) Button buttonAll;
    @BindView(R.id.buttonFavorite) Button buttonFavorite;

    @OnTextChanged(R.id.searchText) void onTextChanged() {
        refreshList();
        searchView.animate().alpha(searchText.getText().toString().isEmpty() ? 0.75f : 1f).setDuration(200).start();
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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        ButterKnife.bind(this, view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        HomeAdapter adapter = new HomeAdapter();
        adapter.list = new ArrayList<>(BeatmapIndex.getInstance().getAllBeatmaps());
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        return view;
    }

    public void refreshList() {
        mRecyclerView.getAdapter().notifyDataSetChanged();
    }

    public void rebuildList() {
        ((HomeAdapter) Objects.requireNonNull(mRecyclerView.getAdapter())).list = new ArrayList<>(BeatmapIndex.getInstance().getAllBeatmaps());
        refreshList();
    }

    protected class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.PlaylistViewHolder> {
        List<String> list = new ArrayList<>();

        @NonNull @Override
        public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return viewType == -1 ? new PlaylistViewHolder() : new PlaylistViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.item_playlist, parent, false));
        }

        @Override
        public int getItemViewType(int position) {
            String path = list.get(position);
            var x = BeatmapIndex.getInstance().getMetadata(path);
            String input = searchText.getText().toString().trim().toLowerCase();
            if (!input.isEmpty()) {
                if (!(x.getArtist().toLowerCase().contains(input) ||
                        x.getTitle().toLowerCase().contains(input) ||
                        x.getRomanisedArtist().toLowerCase().contains(input) ||
                        x.getRomanisedTitle().toLowerCase().contains(input) ||
                        x.getMapper().toLowerCase().contains(input) ||
                        x.getVersion().toLowerCase().contains(input))) {
                    return -1;
                }
            }
            return 0;
        }

        @Override
        public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
            String path = list.get(position);
            var x = BeatmapIndex.getInstance().getMetadata(path);
            if (holder.isNull) return;

            holder.getTitle().setText(x.getTitle() + " - " + x.getArtist());
            holder.getVersion().setText(x.getVersion() + " by " + x.getMapper());
            String currentPath = ((MainActivity) getActivity()).getPlayerService().getCurrentPath();
            holder.getPlaying().setVisibility(currentPath.equals(path) ? View.VISIBLE : View.GONE);
            holder.getFavorite().setAlpha(0.75f); //TODO
            holder.getRoot().setOnClickListener(a -> {
                var playerService = ((MainActivity) getActivity()).getPlayerService();
                playerService.getPlaylist().clear();
                List<String> allBeatmaps = BeatmapIndex.getInstance().getAllBeatmaps();
                playerService.getPlaylist().addAll(allBeatmaps);
                playerService.play(allBeatmaps.indexOf(path));
            });
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

            boolean isNull = false;

            private PlaylistViewHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);
            }

            private PlaylistViewHolder() {
                super(new FrameLayout(PlaylistFragment.this.getContext()));
                isNull = true;
            }
        }
    }
}
