// Copyright 2020 Courville Software
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

package com.archos.mediascraper.themoviedb3;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.archos.medialib.R;
import com.archos.mediascraper.MovieTags;
import com.archos.mediascraper.ScraperImage;
import com.archos.mediascraper.ScraperTrailer;
import com.uwetrottmann.tmdb2.entities.BaseCompany;
import com.uwetrottmann.tmdb2.entities.CastMember;
import com.uwetrottmann.tmdb2.entities.Country;
import com.uwetrottmann.tmdb2.entities.CrewMember;
import com.uwetrottmann.tmdb2.entities.Genre;
import com.uwetrottmann.tmdb2.entities.Image;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.entities.ReleaseDate;
import com.uwetrottmann.tmdb2.entities.ReleaseDatesResult;
import com.uwetrottmann.tmdb2.entities.SpokenLanguage;
import com.uwetrottmann.tmdb2.entities.Videos;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MovieIdParser2 {

    private static final Logger log = LoggerFactory.getLogger(MovieIdParser2.class);

    private static final String DIRECTOR = "Director";
    private static final String WRITER = "Writer";
    private static final String PRODUCER = "Producer";
    private static final String SCREENPLAY = "Screenplay";
    private static final String MUSICCOMPOSER = "Original Music Composer";
    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    private final static int limitTrailers = 40; // limit number of trailers

    private static Context mContext;

    public static MovieTags getResult(Movie movie, Context context) {
        mContext = context;
        MovieTags result = new MovieTags();
        if (movie.id != null) result.setOnlineId(movie.id);
        if (movie.genres != null) {
            List<String> localizedGenres = getLocalizedGenres(movie.genres);
            for (String genre : localizedGenres)
                result.addGenreIfAbsent(genre);
        }
        if (movie.imdb_id != null) result.setImdbId(movie.imdb_id);
        if (movie.overview != null) result.setPlot(movie.overview);
        if (movie.production_companies != null)
            for (BaseCompany productionCompany: movie.production_companies)
                result.addStudioIfAbsent(productionCompany.name);
        if (movie.production_countries != null)
            for (Country country: movie.production_countries)
                result.addCountryIfAbsent(country.iso_3166_1);
        if (movie.release_date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(movie.release_date);
            result.setYear(cal.get(Calendar.YEAR));
        }
        if (movie.belongs_to_collection != null) {
            log.debug("getResult collection id: " + movie.belongs_to_collection.id + ", for " + movie.belongs_to_collection.name);
            result.setCollectionId(movie.belongs_to_collection.id);
            result.setCollectionBackdropPath(movie.belongs_to_collection.backdrop_path);
            result.setCollectionPosterPath(movie.belongs_to_collection.poster_path);
            result.setCollectionName(movie.belongs_to_collection.name);
            log.debug("getResult collection overview: " + movie.belongs_to_collection.overview);
        } else
            result.setCollectionId(-1);
        if (movie.title != null) result.setTitle(movie.title);
        if (movie.vote_average != null)
            result.setRating(Math.round(movie.vote_average.floatValue() * 10)/10.0f);

        if (movie.credits != null) {
            if (movie.credits.guest_stars != null)
                for (CastMember guestStar : movie.credits.guest_stars)
                    result.addActorIfAbsent(guestStar.name, guestStar.character);
            if (movie.credits.cast != null)
                for (CastMember actor : movie.credits.cast)
                    try {
                        JSONObject actorObject = new JSONObject();
                        actorObject.put("name", actor.name);
                        actorObject.put("character", actor.character);
                        actorObject.put("profile_path", actor.profile_path);

                        result.addActorIfAbsent(actorObject.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
            if (movie.credits.crew != null)
                for (CrewMember crew : movie.credits.crew) {
                    assert crew.job != null;
                    if (crew.job.equals(DIRECTOR))
                        result.addDirectorIfAbsent(crew.name);
                    if (crew.job.equals(PRODUCER))
                        result.addProducerIfAbsent(crew.name);
                    if (crew.job.equals(WRITER))
                        result.addWriterIfAbsent(crew.name);
                    if (crew.job.equals(SCREENPLAY))
                        result.addScreenplayIfAbsent(crew.name);
                    if (crew.job.equals(MUSICCOMPOSER))
                        result.addMusiccomposerIfAbsent(crew.name);
                }
        }
        if (movie.credits != null) {
            if (movie.credits.cast != null)
                for (int j = 0; j < movie.credits.cast.size(); j++) {
                    result.addDefaultActorPhotoTMDB(mContext, movie.credits.cast.get(j).profile_path);
                }
        } else log.debug("getResult: no actor_photo_path for " + movie.id);

        /* Fetch movie studio logos */
        if (movie.production_companies != null) {
            for (int i = 0; i < movie.production_companies.size(); i++) {
                String githubCompanyPath = movie.production_companies.get(i).name.replaceAll(" ", "%20").replaceAll("\t", "") + ".png";
                result.addStudioLogosGITHUB(mContext, githubCompanyPath);
            }
        }

        //set movie logo
        String apikey = "ac6ed0ad315f924847ff24fa4f555571";
        String url = "https://webservice.fanart.tv/v3/movies/" + movie.id + "?api_key=" + apikey;
        List<String> enClearLogos = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(readUrl(url));
            JSONArray jsonArray = json.getJSONArray("hdmovielogo");
            for(int i = 0; i < jsonArray.length(); i++){
                JSONObject movieObject = jsonArray.getJSONObject(i);
                if (movieObject.getString("lang").equalsIgnoreCase("en"))
                    enClearLogos.add(movieObject.getString("url"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 0; i < enClearLogos.size(); i++) {
            result.addDefaultClearLogoFTV(mContext, enClearLogos.get(0));
        }

        // set certification for movies
        // Get system language country code
        String systemCountryCode = Locale.getDefault().getCountry();

        if (movie.release_dates != null && movie.release_dates.results != null) {
            String selectedCertification = null;

            for (ReleaseDatesResult releaseDatesResult : movie.release_dates.results) {
                if (releaseDatesResult.release_dates != null) {
                    // Match the country code with iso_3166_1
                    if (systemCountryCode.equals(releaseDatesResult.iso_3166_1)) {
                        for (ReleaseDate releaseDate : releaseDatesResult.release_dates) {
                            if (releaseDate.certification != null && !releaseDate.certification.isEmpty()) {
                                selectedCertification = releaseDate.certification;
                                break; // Found a match, exit loop
                            }
                        }
                    }
                }
            }

            // Fallback: If no certification was found for the system country, try US
            if (selectedCertification == null) {
                for (ReleaseDatesResult releaseDatesResult : movie.release_dates.results) {
                    if (releaseDatesResult.release_dates != null && "US".equals(releaseDatesResult.iso_3166_1)) {
                        for (ReleaseDate releaseDate : releaseDatesResult.release_dates) {
                            if (releaseDate.certification != null && !releaseDate.certification.isEmpty()) {
                                selectedCertification = releaseDate.certification;
                                break; // Found a fallback, exit loop
                            }
                        }
                    }
                }
            }

            // Set the content rating if we found one
            if (selectedCertification != null) {
                result.setContentRating(selectedCertification);
            }
        }


        // setting multiple movie tags using a single pipeline (tagline, budget, revenue, runtime, vote_count, popularity, release date, original language)
        String pattern = "MMMM dd, yyyy";
        String releaseDate = "";
        if (movie.release_date != null) {
            SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.getDefault());
            releaseDate = (movie.release_date != null) ? formatter.format(movie.release_date) : "";
        }
        try {
            JSONObject movieTags = new JSONObject();
            movieTags.put("tagline", movie.tagline);
            movieTags.put("budget", movie.budget);
            movieTags.put("revenue", movie.revenue);
            movieTags.put("runtime", movie.runtime);
            movieTags.put("vote_count", movie.vote_count);
            movieTags.put("popularity", movie.popularity);
            movieTags.put("release_date", releaseDate);
            movieTags.put("original_language", movie.original_language);

            result.addTaglineIfAbsent(movieTags.toString());  // Save JSON as a string
        } catch (JSONException e) {
            e.printStackTrace();  // Log the error for debugging
        }


        // set Spoken languages
        if (movie.spoken_languages != null){
            for (SpokenLanguage spokenLanguage : movie.spoken_languages)
                result.addSpokenlanguageIfAbsent(spokenLanguage.iso_639_1);
        }

        if (movie.runtime != null) result.setRuntime(movie.runtime, TimeUnit.MINUTES);

        List<ScraperTrailer> trailers;
        if (movie.videos != null && movie.videos.results != null) {
            trailers = new ArrayList<>(movie.videos.results.size());
            int i = 0;
            for (Videos.Video trailer: movie.videos.results) {
                if (i < limitTrailers) {
                    if (trailer.site != null && trailer.iso_639_1 != null && trailer.type !=null) {
                        log.debug("getResult: addTrailers found " + trailer.name + " for service " + trailer.site + " of type " + trailer.type + " in " + trailer.iso_639_1);
                        if (trailer.site.equals("YouTube") && ("Trailer".equals(trailer.type.toString())||"Teaser".equals(trailer.type.toString()))) {
                            log.debug("getResult: addTrailers adding it " + trailer.name);
                            ScraperTrailer videoTrailer = new ScraperTrailer(ScraperTrailer.Type.MOVIE_TRAILER, trailer.name, trailer.key, trailer.site, trailer.iso_639_1);
                            trailers.add(videoTrailer);
                            i++;
                        }
                    }
                }
            }
        } else {
            trailers = new ArrayList<>();
        }
        result.setTrailers(trailers);

        // posters
        List<ScraperImage> posters = new ArrayList<>();
        List<Pair<Image, String>> tempPosters = new ArrayList<>();
        // backdrops
        List<ScraperImage> backdrops = new ArrayList<>();
        List<Pair<Image, String>> tempBackdrops = new ArrayList<>();

        // clearlogos
        List<ScraperImage> clearlogos = new ArrayList<>();
        List<String> tempClearLogos = new ArrayList<>();

        //set movie clearlogos
        try {
            JSONObject json = new JSONObject(readUrl(url));
            JSONArray jsonArray = json.getJSONArray("hdmovielogo");
            for(int j = 0; j < jsonArray.length(); j++){
                JSONObject movieObject = jsonArray.getJSONObject(j);
                tempClearLogos.add(movieObject.getString("url"));
                clearlogos.add(genClearLogo(movie.title, movieObject.getString("url"),  context));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (movie.images != null) {
            if (movie.images.posters != null)
                for (Image poster : movie.images.posters)
                    tempPosters.add(Pair.create(poster, poster.iso_639_1));
            if (movie.images.backdrops != null)
                for (Image backdrop : movie.images.backdrops)
                    tempBackdrops.add(Pair.create(backdrop, backdrop.iso_639_1));
            Collections.sort(tempPosters, new Comparator<Pair<Image, String>>() {
                @Override
                public int compare(Pair<Image, String> b1, Pair<Image, String> b2) {
                    return - Double.compare(b1.first.vote_average, b2.first.vote_average);
                }
            });
            Collections.sort(tempBackdrops, new Comparator<Pair<Image, String>>() {
                @Override
                public int compare(Pair<Image, String> b1, Pair<Image, String> b2) {
                    return - Double.compare(b1.first.vote_average, b2.first.vote_average);
                }
            });
            for(Pair<Image, String> poster : tempPosters) {
                log.debug("getResult: generating ScraperImage for poster for " + movie.title + ", large=" + ScraperImage.TMPL + poster.first.file_path);
                posters.add(genPoster(movie.title, poster.first.file_path, poster.second, mContext));
            }
            for(Pair<Image, String> backdrop : tempBackdrops) {
                log.debug("getResult: generating ScraperImage for backdrop for " + movie.title + ", large=" + ScraperImage.TMPL + backdrop.first.file_path);
                posters.add(genBackdrop(movie.title, backdrop.first.file_path, backdrop.second, mContext));
            }
            log.debug("getResult: setting posters and backdrops");
            result.setPosters(posters);
            result.setBackdrops(backdrops);
            result.setClearLogos(clearlogos);
            log.debug("getResult: global " + movie.title + " poster " + movie.poster_path + ", backdrop " + movie.backdrop_path);
            // this must be done after setPosters/setBackdrops otherwise default is removed
            if (movie.poster_path != null) result.addDefaultPosterTMDB(mContext, movie.poster_path);
            if (movie.backdrop_path != null) result.addDefaultBackdropTMDB(mContext, movie.backdrop_path);
        }

        return result;
    }

    public static ScraperImage genPoster(String title, String path, String lang, Context context) {
        ScraperImage image = new ScraperImage(ScraperImage.Type.MOVIE_POSTER, title);
        image.setLanguage(lang);
        image.setLargeUrl(ScraperImage.TMPL + path);
        image.setThumbUrl(ScraperImage.TMPT + path);
        image.generateFileNames(context);
        log.debug("genPoster: " + title + ", has poster " + image.getLargeUrl() + " path " + image.getLargeFile());
        return image;
    }

    public static ScraperImage genBackdrop(String title, String path, String lang, Context context) {
        ScraperImage image = new ScraperImage(ScraperImage.Type.MOVIE_BACKDROP, title);
        image.setLanguage(lang);
        image.setLargeUrl(ScraperImage.TMBL + path);
        image.setThumbUrl(ScraperImage.TMBT + path);
        image.generateFileNames(context);
        log.debug("genBackdrop: " + title + ", has backdrop " + image.getLargeUrl() + " path " + image.getLargeFile());
        return image;
    }

    public static ScraperImage genClearLogo(String title, String path, Context context) {
        ScraperImage image = new ScraperImage(ScraperImage.Type.MOVIE_CLEARLOGO, title);
        image.setLargeUrl(path);
        image.setThumbUrl(path);
        image.generateFileNames(context);
        return image;
    }

    // many genres are not translated on tmdb and localized request is returned in the local language making one to
    // one mapping difficult without changing all db structure --> revert to show trick which is the only way to cope
    // with fallback search in en is perfomed when localized search returns nothing
    private static List<String> getLocalizedGenres(List<Genre> genres) {
        ArrayList<String> localizedGenres = new ArrayList<>();
        for (Genre genre : genres) {
            switch (genre.id) {
                case 28:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_action));
                    break;
                case 12:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_adventure));
                    break;
                case 16:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_animation));
                    break;
                case 35:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_comedy));
                    break;
                case 80:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_crime));
                    break;
                case 99:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_documentary));
                    break;
                case 18:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_drama));
                    break;
                case 10751:
                    localizedGenres.add(mContext.getString(R.string.movie_family));
                    break;
                case 14:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_fantasy));
                    break;
                case 36:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_history));
                    break;
                case 27:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_horror));
                    break;
                case 10402:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_music));
                    break;
                case 9648:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_mystery));
                    break;
                case 10749:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_romance));
                    break;
                case 878:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_science_fiction));
                    break;
                case 10770:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_tv_movie));
                    break;
                case 53:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_thriller));
                    break;
                case 10752:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_war));
                    break;
                case 37:
                    localizedGenres.add(mContext.getString(R.string.movie_genre_western));
                    break;
                default:
                    log.warn("getLocalizedGenres: unknown genre: id=" + genre.id + ", name=" + genre.name);
                    localizedGenres.add(genre.name);
            }
        }
        return localizedGenres;
    }
}
