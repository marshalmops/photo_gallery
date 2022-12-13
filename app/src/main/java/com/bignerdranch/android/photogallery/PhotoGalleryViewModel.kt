package com.bignerdranch.android.photogallery

import android.app.Application
import androidx.lifecycle.*

class PhotoGalleryViewModel(private val app: Application) : AndroidViewModel(app) {
    val m_galleryItemLiveData: LiveData<List<GalleryItem>>
    private val m_flickrFetcher = FlickrFetcher()
    private val m_mutableSearchTerm = MutableLiveData<String>()
    val m_searchTerm: String get() = m_mutableSearchTerm.value ?: ""

    init {
        m_mutableSearchTerm.value = QueryPreferences.getStoredQuery(app)

        m_galleryItemLiveData = Transformations.switchMap(m_mutableSearchTerm) {
            searchTerm ->
            if (searchTerm.isBlank())
                m_flickrFetcher.fetchPhotos()
            else
                m_flickrFetcher.searchPhotos(searchTerm)

        }
    }

    fun fetchPhotos(query: String = "") {
        QueryPreferences.setStoredQuery(app, query)

        m_mutableSearchTerm.value = query
    }
}