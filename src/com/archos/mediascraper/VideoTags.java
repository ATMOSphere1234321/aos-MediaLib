// Copyright 2017 Archos SA
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.archos.mediascraper;

import android.os.Parcel;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class VideoTags extends BaseTags {

    protected final List<String> mStudios = new ArrayList<String>();
    protected String mStudiosFormatted;

    protected List<String> mGenres = new ArrayList<String>();
    protected String mGenresFormatted;

    protected final List<String> mNetworks = new ArrayList<String>();
    protected String mNetworksFormatted;

    public VideoTags() {
        super();
    }

    public VideoTags(Parcel in) {
        super(in);
        readFromParcel(in);
    }

    public boolean genreExists(String name) {
        return mGenres.contains(name);
    }

    public List<String> getGenres() { return mGenres; }

    public String getGenresFormatted() {
        ensureFormattedGenres();
        return mGenresFormatted;
    }

    public List<String> getStudios() { return mStudios; }

    public String getStudiosFormatted() {
        ensureFormattedStudios();
        return mStudiosFormatted;
    }

    /** does nothing if mGenresFormatted is already set, otherwise builds from mGenres */
    private void ensureFormattedGenres() {
        if (mGenresFormatted == null && mGenres != null && !mGenres.isEmpty()) {
            mGenresFormatted = TextUtils.join(", ", mGenres);
        }
    }

    /** does nothing if mStudiosFormatted is already set, otherwise builds from mStudios */
    private void ensureFormattedStudios() {
        if (mStudiosFormatted == null && mStudios != null && !mStudios.isEmpty()) {
            mStudiosFormatted = TextUtils.join(", ", mStudios);
        }
    }


    /**
     * all strings are trimmed, does not put empty values, does not replace entries.
     * optional splitCharacters to specify how to split the string
     **/
    public void addGenreIfAbsent(String genre, char... splitCharacters) {
        addIfAbsentSplitNTrim(genre, mGenres, splitCharacters);
    }

    public void setGenres(List<String> genres) { mGenres = genres; }

    public void setGenresFormatted(String genres) { mGenresFormatted = genres; }

    /**
     * all strings are trimmed, does not put empty values, does not replace entries.
     * optional splitCharacters to specify how to split the string
     **/
    public void addStudioIfAbsent(String studio, char... splitCharacters) {
        addIfAbsentSplitNTrim(studio, mStudios, splitCharacters);
    }

    public void setStudiosFormatted(String studios) { mStudiosFormatted = studios; }
    public boolean studioExists(String name) {
        return mStudios.contains(name);
    }

    public List<String> getNetworks() { return mNetworks; }

    public String getNetworksFormatted() {
        ensureFormattedNetworks();
        return mNetworksFormatted;
    }

    private void ensureFormattedNetworks() {
        if (mNetworksFormatted == null && mNetworks != null && !mNetworks.isEmpty()) {
            mNetworksFormatted = TextUtils.join(", ", mNetworks);
        }
    }

    public void addNetworkIfAbsent(String network, char... splitCharacters) {
        addIfAbsentSplitNTrim(network, mNetworks, splitCharacters);
    }

    public void setNetworksFormatted(String networks) { mNetworksFormatted = networks; }

    public boolean networkExists(String name) {
        return mNetworks.contains(name);
    }

    public void addAllNetworks(List<String> networks){
        mNetworks.addAll(networks);
    }

    @Override
    public String toString() {
        return super.toString() + " / GENRES=" + mGenres + " / STUDIOS=" + mStudios + " / NETWORKS=" + mNetworks;
    }

    private void readFromParcel(Parcel in) {
        in.readStringList(mStudios);
        in.readStringList(mGenres);
        // --- Add this line for networks ---
        in.readStringList(mNetworks);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeStringList(mStudios);
        out.writeStringList(mGenres);
        // --- Add this line for networks ---
        out.writeStringList(mNetworks);
    }

    public void addAllGenres(List<String> genres){
        mGenres.addAll(genres);
    }
}
