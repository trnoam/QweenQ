package com.qween.qweenq;

import android.os.Parcel;
import android.os.Parcelable;

import com.firebase.client.DataSnapshot;

/**
 * Created by User on 12/9/2016.
 */

public class Attraction implements Parcelable {
    public int _cost, _max;
    public String _description, _name, _picture, _key;
    public boolean _is_full;

    public Attraction(int cost, String description, boolean is_full,
                      int max, String name, String picture, String key){
        _cost = cost;
        _description = description;
        _is_full = is_full;
        _max = max;
        _name = name;
        _picture = picture;
        _key = key;
    }

    public Attraction(Parcel in){
        _cost = in.readInt();
        _description = in.readString();
        _is_full = in.readByte() != 0;
        _max = in.readInt();
        _name = in.readString();
        _picture = in.readString();
        _key = in.readString();
    }

    public Attraction(DataSnapshot attraction_data){
        _cost = attraction_data.child("cost").getValue(Integer.class);
        _description = attraction_data.child("description").getValue(String.class);
        _is_full= attraction_data.child("is full").getValue(Boolean.class);
        _max = attraction_data.child("max").getValue(Integer.class);
        _name = attraction_data.child("name").getValue(String.class);
        _picture = attraction_data.child("picture").getValue(String.class);
        _key = attraction_data.getKey();
    }

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_cost);
        dest.writeString(_description);
        dest.writeByte((byte) (_is_full ? 1 : 0));
        dest.writeInt(_max);
        dest.writeString(_name);
        dest.writeString(_picture);
        dest.writeString(_key);
    }
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Attraction createFromParcel(Parcel in) {
            return new Attraction(in);
        }

        public Attraction[] newArray(int size) {
            return new Attraction[size];
        }
    };
}
