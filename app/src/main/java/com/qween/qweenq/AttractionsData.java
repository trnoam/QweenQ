package com.qween.qweenq;

import android.os.Parcel;
import android.os.Parcelable;

import com.firebase.client.DataSnapshot;

/**
 * Created by User on 12/9/2016.
 */

public class AttractionsData implements Parcelable {
    public Attraction[] _attractions_array;

    public AttractionsData(Attraction[] attractions_array){
        _attractions_array = attractions_array;
    }


    public AttractionsData(Parcel in){
        _attractions_array = new Attraction[in.readInt()];
        for(int i = 0; i < _attractions_array.length; i++){
            _attractions_array[i] = new Attraction(in);
        }
    }

    public AttractionsData(DataSnapshot attractions_data){
        _attractions_array = new Attraction[(int) attractions_data.getChildrenCount()];
        for(DataSnapshot attraction : attractions_data.getChildren()){
            Attraction temp_attraction = new Attraction(attraction);
            _attractions_array[Integer.parseInt(temp_attraction._key) - 1] = temp_attraction;
        }
    }

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_attractions_array.length);
        for(int i = 0; i < _attractions_array.length; i++){
            _attractions_array[i].writeToParcel(dest, flags);
        }
    }
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public AttractionsData createFromParcel(Parcel in) {
            return new AttractionsData(in);
        }

        public AttractionsData[] newArray(int size) {
            return new AttractionsData[size];
        }
    };
}
