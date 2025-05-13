package com.archos.mediascraper.themoviedb3.aggregate;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ExtendedTvService {
    @GET("tv/{tv_id}/aggregate_credits")
    Call<AggregateCredits> aggregateCredits(
            @Path("tv_id") int tvId,
            @Query("language") String language
    );
}
