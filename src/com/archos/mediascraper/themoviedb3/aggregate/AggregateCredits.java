package com.archos.mediascraper.themoviedb3.aggregate;

import com.uwetrottmann.tmdb2.entities.CrewMember;

import java.util.List;

public class AggregateCredits {
    public List<AggregateCastMember> cast;
    public List<CrewMember> crew; // Optional, can be removed if unused
}
