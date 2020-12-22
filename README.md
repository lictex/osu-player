# osu!player
play music with hitsounds on android devices, inspired by [this project](https://github.com/Milkitic/Osu-Player).  

(only) tested on Galaxy S10+ with android r.  
> but it should work on all devices with android kitkat or newer.

<img src="https://file.blessingsoftware.cc/upload/Screenshot_20200421-152847_osu!player.jpg" width="192"> <img src="https://file.blessingsoftware.cc/upload/Screenshot_20200421-152334_osu!player.jpg" width="192">  

## known issues
* `android.permission.MANAGE_EXTERNAL_STORAGE` is required on android r devices. using saf to access osu!stable's directory based storage seems a bit difficult, also indexing performance might be reduced according to [this doc](https://developer.android.com/training/data-storage/shared/documents-files#grant-access-directory).
* reading osu!lazer's database seems impossible since android r introduces [restrictions on accessing apps' private data](https://developer.android.com/about/versions/11/privacy/storage#other-apps-data).
* symbolic links are not supported in songs folder path.  
* maybe incompatible with old osu file formats.
* low performance.
* many many bugs.
