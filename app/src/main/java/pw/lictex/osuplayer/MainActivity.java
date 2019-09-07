package pw.lictex.osuplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import pw.lictex.osuplayer.audio.OsuAudioPlayer;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn)
    Button button;

    @BindView(R.id.textView)
    TextView t;

    @BindView(R.id.seekBar)
    SeekBar seekBar;

    @BindView(R.id.musicVol)
    SeekBar musicVol;

    @BindView(R.id.soundVol)
    SeekBar soundVol;

    @BindView(R.id.offset)
    EditText offset;

    private OsuAudioPlayer osuAudioPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setTitle("Test");
        seekBar.setMax(1000);

        osuAudioPlayer = new OsuAudioPlayer(getApplicationContext());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    osuAudioPlayer.seekTo((long) (i / 1000d * osuAudioPlayer.getAudioLength()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        musicVol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                osuAudioPlayer.setMusicVolume(i);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        soundVol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                osuAudioPlayer.setSoundVolume(i);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        new Thread(() -> {
            while (true) {
                runOnUiThread(() -> {
                    button.setText(String.valueOf(osuAudioPlayer.getCurrentTime()));
                    t.setText(String.valueOf(osuAudioPlayer.getEngine().getAudioClockFreq()));
                    seekBar.setProgress((int) (((double) osuAudioPlayer.getCurrentTime() / osuAudioPlayer.getAudioLength()) * 1000));
                });
                SystemClock.sleep(64);
            }
        }).start();
    }

    @OnClick(R.id.btn)
    void buttonClick() {
        osuAudioPlayer.seekTo(new java.util.Random().nextInt(1000 * 60));
    }

    @OnClick(R.id.btnDT)
    void dtClick() {
        osuAudioPlayer.setMod(OsuAudioPlayer.Mod.DT);
    }

    @OnClick(R.id.btnNC)
    void ncClick() {
        osuAudioPlayer.setMod(OsuAudioPlayer.Mod.NC);
    }

    @OnClick(R.id.btnHT)
    void htClick() {
        osuAudioPlayer.setMod(OsuAudioPlayer.Mod.HT);
    }

    @OnClick(R.id.btnNM)
    void nmClick() {
        osuAudioPlayer.setMod(OsuAudioPlayer.Mod.None);
    }

    @OnClick(R.id.btnOpen)
    void openClick() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        this.startActivityForResult(intent, 141);
    }

    @OnTextChanged(R.id.offset)
    void onOffsetChanged() {
        try {
            osuAudioPlayer.setSampleOffset(Integer.valueOf(offset.getText().toString()));
        } catch (Exception e) {
            osuAudioPlayer.setSampleOffset(0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case 141:
                Uri dataUri = data.getData();
                Toast.makeText(this, dataUri.toString(), Toast.LENGTH_LONG).show();

                osuAudioPlayer.openBeatmapSet(new File(FileUtils.getFilePathByUri(this, dataUri)).getParent() + "/");
                osuAudioPlayer.openBeatmap(new File(FileUtils.getFilePathByUri(this, dataUri)).getName());
                osuAudioPlayer.play();
                break;
        }
    }

    public static final class FileUtils {

        public static String getFilePathByUri(Context context, Uri uri) {
            String path = null;
            // 以 file:// 开头的
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                path = uri.getPath();
                return path;
            }
            // 以 content:// 开头的，比如 content://media/extenral/images/media/17766
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        if (columnIndex > -1) {
                            path = cursor.getString(columnIndex);
                        }
                    }
                    cursor.close();
                }
                return path;
            }
            // 4.4及之后的 是以 content:// 开头的，比如 content://com.android.providers.media.documents/document/image%3A235700
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    if (isExternalStorageDocument(uri)) {
                        // ExternalStorageProvider
                        final String docId = DocumentsContract.getDocumentId(uri);
                        final String[] split = docId.split(":");
                        final String type = split[0];
                        if ("primary".equalsIgnoreCase(type)) {
                            path = Environment.getExternalStorageDirectory() + "/" + split[1];
                            return path;
                        }
                    } else if (isDownloadsDocument(uri)) {
                        // DownloadsProvider
                        final String id = DocumentsContract.getDocumentId(uri);
                        final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                                Long.valueOf(id));
                        path = getDataColumn(context, contentUri, null, null);
                        return path;
                    } else if (isMediaDocument(uri)) {
                        // MediaProvider
                        final String docId = DocumentsContract.getDocumentId(uri);
                        final String[] split = docId.split(":");
                        final String type = split[0];
                        Uri contentUri = null;
                        if ("image".equals(type)) {
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        } else if ("video".equals(type)) {
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        } else if ("audio".equals(type)) {
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        }
                        final String selection = "_id=?";
                        final String[] selectionArgs = new String[]{split[1]};
                        path = getDataColumn(context, contentUri, selection, selectionArgs);
                        return path;
                    }
                }
            }
            return null;
        }

        private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
            Cursor cursor = null;
            final String column = "_data";
            final String[] projection = {column};
            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    final int column_index = cursor.getColumnIndexOrThrow(column);
                    return cursor.getString(column_index);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
            return null;
        }

        private static boolean isExternalStorageDocument(Uri uri) {
            return "com.android.externalstorage.documents".equals(uri.getAuthority());
        }

        private static boolean isDownloadsDocument(Uri uri) {
            return "com.android.providers.downloads.documents".equals(uri.getAuthority());
        }

        private static boolean isMediaDocument(Uri uri) {
            return "com.android.providers.media.documents".equals(uri.getAuthority());
        }

    }
}
