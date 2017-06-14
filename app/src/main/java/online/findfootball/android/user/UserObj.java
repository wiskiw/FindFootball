package online.findfootball.android.user;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.io.Serializable;
import java.util.ArrayList;

import online.findfootball.android.firebase.database.DataInstanceResult;
import online.findfootball.android.firebase.database.children.PackableArrayList;
import online.findfootball.android.firebase.database.children.PackableObject;
import online.findfootball.android.game.GameObj;

/**
 * Created by WiskiW on 17.04.2017.
 */

public class UserObj extends PackableObject implements Parcelable, Serializable {

    private final static String PATH_USERS = "users/";
    public final static String PATH_GAMES_FOOTBALL = "/events/football/";

    public final static String PATH_DISPLAY_NAME = "display_name/";
    public final static String PATH_EMAIL = "email/";
    public final static String PATH_PHOTO_URL = "photo_url/";
    public final static String PATH_REGISTER_TIME = "register_time/";
    public final static String PATH_LAST_ACTIVITY_TIME = "last_activity_time/";


    private String uid;
    private String displayName;
    private String email;
    private long lastActivityTime;
    private long registerTime;
    private Uri photoUrl;
    private PackableArrayList<GameObj> gameList;


    public UserObj(String uid) {
        this.uid = uid;
    }

    public UserObj(FirebaseUser firebaseUser) {
        setUid(firebaseUser.getUid());
        setDisplayName(firebaseUser.getDisplayName());
        setEmail(firebaseUser.getEmail());
        setPhotoUrl(firebaseUser.getPhotoUrl());
    }

    private void newGameList() {
        gameList = new PackableArrayList<GameObj>() {
            @Override
            protected GameObj unpackItem(DataSnapshot dataSnapshot) {
                return new GameObj(dataSnapshot.getKey());
            }

            @Override
            protected void packItem(DatabaseReference databaseReference, GameObj item) {
                String eid = item.getEid();
                databaseReference.child(eid).setValue(eid);
            }
        };
        gameList.setDirectoryPath(getDirectoryPath() + PATH_GAMES_FOOTBALL);
    }

    public PackableArrayList<GameObj> getGameList() {
        if (gameList == null) {
            newGameList();
        }
        return gameList;
    }

    public void setGameList(ArrayList<GameObj> gameList) {
        if (gameList == null) {
            return;
        }
        if (this.gameList == null) {
            newGameList();
        } else {
            this.gameList.clear();
        }
        for (GameObj gameObj : gameList) {
            this.gameList.add(gameObj);
        }
    }

    public void setGameList(PackableArrayList<GameObj> gameList) {
        this.gameList = gameList;
    }

    public void addGame(GameObj gameObj) {
        if (gameList.contains(gameObj)) {
            gameList.remove(gameObj);
        }
        gameList.add(gameObj);
    }

    public void removeGame(GameObj gameObj) {
        if (gameList != null) {
            gameList.remove(gameObj);
        }
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(long lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public long getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(long registerTime) {
        this.registerTime = registerTime;
    }

    public Uri getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(Uri photoUrl) {
        this.photoUrl = photoUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(uid);
        out.writeString(displayName);
        out.writeString(email);
        out.writeLong(lastActivityTime);
        out.writeLong(registerTime);
        out.writeParcelable(photoUrl, flags);
        out.writeTypedList(gameList);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<UserObj> CREATOR = new Parcelable.Creator<UserObj>() {
        public UserObj createFromParcel(Parcel in) {
            return new UserObj(in);
        }

        public UserObj[] newArray(int size) {
            return new UserObj[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    public UserObj(Parcel in) {
        uid = in.readString();
        displayName = in.readString();
        email = in.readString();
        lastActivityTime = in.readLong();
        registerTime = in.readLong();
        photoUrl = in.readParcelable(Uri.class.getClassLoader());
        newGameList();
        in.readTypedList(gameList, GameObj.CREATOR);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UserObj) {
            return (((UserObj) obj).getUid()).equals(this.getUid());
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return this.getUid().hashCode();
    }

    @Override
    public String getDirectoryPath() {
        return PATH_USERS + uid + "/";
    }

    @Override
    public boolean hasLoaded() {
        return photoUrl != null && email != null && displayName != null;
    }

    @Override
    public DataInstanceResult unpack(DataSnapshot dataSnapshot) {
        try {
            setUid(dataSnapshot.getKey());
            setDisplayName((String) dataSnapshot.child(PATH_DISPLAY_NAME).getValue());
            String url = (String) dataSnapshot.child(PATH_PHOTO_URL).getValue();
            if (url != null) {
                setPhotoUrl(Uri.parse(url));
            }
            setEmail((String) dataSnapshot.child(PATH_EMAIL).getValue());
            setRegisterTime((Long) dataSnapshot.child(PATH_REGISTER_TIME).getValue());
            setLastActivityTime((Long) dataSnapshot.child(PATH_LAST_ACTIVITY_TIME).getValue());

            if (gameList == null) {
                newGameList();
            }
            gameList.unpack(dataSnapshot.child(PATH_GAMES_FOOTBALL));

            return DataInstanceResult.onSuccess();
        } catch (Exception ex) {
            return DataInstanceResult.onFailed(ex.getMessage(), ex);
        }
    }

    @Override
    public DataInstanceResult pack(DatabaseReference databaseReference) {
        return new DataInstanceResult(DataInstanceResult.CODE_NO_PERMISSIONS);
    }
}
